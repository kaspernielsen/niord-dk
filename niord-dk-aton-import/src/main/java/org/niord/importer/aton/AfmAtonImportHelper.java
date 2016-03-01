package org.niord.importer.aton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonTag;
import org.niord.core.user.User;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class used for converting the legacy "AFM" data into OSM seamark format.
 */
@SuppressWarnings("unused")
public class AfmAtonImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "AFMSTATION", "FYRLBNR_DK", "AFM_NAVN", "PLADSNAVN", "AFUFORKORTELSE", "BESKRIVELSE",
            "LATTITUDE", "LONGITUDE", "KARAKNR", "EJER", "KARAKNR", "AJF_BRUGER", "AJF_DATO" };

    /** Each AtoN will have a combination of these values in the "KARAKNR" field **/
    enum AtonType {

        AIS(0),             // 0: AIS
        LIGHT(1),           // 1: Fyr
        LIGHT_MINOR(2),     // 2: Bifyr, tågelys, advarselslys, retningsfyr, hindringslys, m.v.
        BEACON(3),          // 3: Båker, Signalmaster
        RACON(4),           // 4: RACONS
        LIGHT_BUOY(5),      // 5: Lystønder
        BUOY(6),            // 6: Vagere
        STAKE(7),           // 7: Stager i bund
        RADIO_BEACON(8),    // 8: Radiofyr
        FOG_SIGNAL(9);      // 9: Tågesignaler

        int code;
        AtonType(int code) {
            this.code = code;
        }

        static AtonType findByCode(int code) {
            return Arrays.stream(values())
                    .filter(t -> t.code == code)
                    .findFirst()
                    .orElse(null);
        }

        public boolean isLight() {
            return this == LIGHT || this == LIGHT_MINOR;
        }

        public boolean isBuoy() {
            return this == LIGHT_BUOY || this == BUOY;
        }

        public boolean isBeacon() {
            return this == BEACON || this == STAKE;
        }
    }

    /** Constructor **/
    public AfmAtonImportHelper(User user, int changeset, Map<String, Integer> colIndex, Row row) {
        super(user, changeset, colIndex, row);
    }

    /** Converts the current AFM row into an OSM AtoN **/
    public AtonNode afm2osm() throws Exception {
        AtonNode aton = new AtonNode();

        // TODO: aton.setId();
        aton.setVisible(true);
        aton.setLat(numericValue("LATTITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("AJF_DATO"));
        aton.setUser(user.getUsername());
        aton.setUid(user.getId());
        aton.setChangeset(changeset);
        aton.setVersion(1);     // Unknown version

        aton.updateTag(AtonTag.CUST_TAG_ATON_UID, stringValue("AFMSTATION"));
        if (StringUtils.isNotBlank(stringValue("FYRLBNR_DK"))) {
            aton.updateTag(AtonTag.CUST_TAG_LIGHT_NUMBER, stringValue("FYRLBNR_DK"));
        }

        generateAton(
                aton,
                stringValue("AFUFORKORTELSE"),
                stringValue("BESKRIVELSE"),
                numericValue("KARAKNR").intValue());

        return aton;
    }


    /********************************/
    /** Generating OSM AtoN        **/
    /********************************/

    /**
     * Generates type-specific AtoN OSM tags.
     *
     * @param shortDesc the FORKORTELSE field
     * @param description the BESKRIVELSE field
     * @param type the "KARAKNR" field
     * @return the corresponding AFM topmark and color code
     */
    public Integer[] generateAton(AtonNode aton, String shortDesc, String description, int type) {
        if (StringUtils.isBlank(description)) {
            return new Integer[] { null, null };
        }

        // An AtoN consists of a master type (e.g. "light") and a set of slave types (e.g. AIS)
        // Currently, only master is handled, and the slave types are left for the other Excel imports.
        Set<AtonType> types = parseType(type);
        AtonType masterType = masterType(types);

        Integer topMark;
        String osmType = null;
        String osmCateogry = null;
        String osmShape = null;
        switch (description) {

            case "AIS Syntetisk AtoN":
                generaetRadioStationAton(aton, "s-ais");
                break;
            case "AIS Virtuel AtoN":
                generaetRadioStationAton(aton, "v-ais");
                break;

            case "Anduvningsfyr":
                generateAton(aton, "light_major", 3);
                break;

            case "Bagbord båke - CAN":
                generateAton(aton, "beacon_lateral", "port", getBeaconShape(masterType), 2004, 1);
                break;

            case "Bagbord båke - Trekant ned":
                generateAton(aton, "beacon_lateral", "port", getBeaconShape(masterType), 2164, 1);
                break;

            case "Bagbord sideafmærkning":
                topMark = shortDesc.equals("PORT u/top") ? null : 2;
                if (masterType.isLight()) {
                    osmType = "light_minor";
                } else if (masterType.isBeacon()) {
                    osmType = "beacon_lateral";
                    osmCateogry = "port";
                    osmShape = getBeaconShape(masterType);
                } else {
                    osmType = "buoy_lateral";
                    osmCateogry = "port";
                    osmShape = "can";  // Guessing
                }
                generateAton(aton, osmType, osmCateogry, osmShape, topMark, 1);
                break;

            case "Bagbord skillepkts.afmærkning":
            case "Bagbords skillepunktsafmærkn.":
                generateAton(aton, "buoy_lateral", "preferred_channel_starboard", "can", 2, 9);
                break;

            case "Bagbords molefyr":
                generateAton(aton, "light_minor", null, null, null, 1);
                break;

            case "Bifyr med vinkler":
            case "Bifyr, lokalt advarselsfyr":
            case "Bagfyr":
                generateAton(aton, "light_minor", null, null, null, 5);
                break;

            case "Båke (Keglestub) u/top":
                generateAton(aton, "beacon_special_purpose", null, null, null, 4);
                break;

            case "Båke med firkantet plade-top":
                generateAton(aton, "beacon_special_purpose", null, null, 2054, 4);
                break;

            case "Bropassage, bagbord":
                generateAton(aton, "light", null, null, 29, 1);
                break;

            case "Bropassage, styrbord":
                generateAton(aton, "light", null, null, 28, 2);
                break;

            case "Bropassagesignal":
                generateAton(aton, "light", null, null, null, 1);
                break;

            case "CAN-båke i gul tønde, m/top":
                generateAton(aton, "beacon_special_purpose", null, "buoyant", 2004, 4);
                break;

            case "Emergency Wreck Marking Buoy":
                generateAton(aton, "buoy_special_purpose", "warning", "buoyant", null, 2224);
                break;

            case "Firkantet båke u/top":
                generateAton(aton, "beacon_special_purpose", null, null, null, 1);
                break;

            case "Forbåke m/top":
                generateAton(aton, "beacon_special_purpose", "leading", null, 28, 1);
                break;

            case "Forfyr":
                generateAton(aton, masterType == AtonType.LIGHT_MINOR ? "light_minor" : "light", null, null, null, 5);
                break;

            case "Fredningsbåke":
                generateAton(aton, "beacon_special_purpose", null, null, 4, 4);
                break;

            case "Fundet via Dansk Fyrliste":
                if (masterType.isBeacon()) {
                    // Might this be an error?
                    osmType = "beacon_special_purpose";
                } else {
                    osmType = masterType == AtonType.LIGHT_MINOR ? "light_minor" : "light";
                }
                generateAton(aton, osmType, null, null, null, 23);
                break;

            case "Fyrtårn":
                generateAton(aton, "light_major", null, null, null, 3);
                break;

            case "Grave-bagbåke":
                generateAton(aton, "beacon_special_purpose", "leading", null, 20, 3);
                break;

            case "Grave-forbåke":
                generateAton(aton, "beacon_special_purpose", "leading", null, 20, 3);
                break;

            case "Havn / fredningsomr. bagbåke":
            case "Havn / fredningsomr. forbåke":
                generateAton(aton, "beacon_special_purpose", "leading", null, 4, 4);
                break;

            case "Hvid stage med rød kugletop":
                generateAton(aton, "beacon_special_purpose", null, "stake", 11, 3);
                break;

            case "Isoleret fareafmærkning":
                generateAton(aton, "buoy_isolated_danger", null, null, 27, 15);
                break;

            case "Jernstang m. diamant-top":
                generateAton(aton, "beacon_special_purpose", null, "pole", 2104, 1);
                break;

            case "Kabel bagbåke":
                generateAton(aton, "buoy_isolated_danger", "cable", null, 14, 7);
                break;

            case "Kabel forbåke":
                generateAton(aton, "buoy_isolated_danger", "cable", null, 13, 16);
                break;

            case "Kabelskilt":
                generateAton(aton, "buoy_isolated_danger", "cable", null, null, null);
                break;

            case "Kompasafmærkning Ø for.":
                generateAton(aton, masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal", "east", null, 24, 13);
                break;

            case "Kompasafmærkning N for.":
                generateAton(aton, masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal", "north", null, 23, 12);
                break;

            case "Kompasafmærkning S for.":
                generateAton(aton, masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal", "south", null, 25, 11);
                break;

            case "Kompasafmærkning V for.":
                generateAton(aton, masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal", "west", null, 26, 10);
                break;

            case "Meteorologimast":
                generateAton(aton, "landmark", "mast", null, null, 5);
                break;

            case "Midtfarvandsafmærkning":
                generateAton(aton, masterType.isBeacon() ? "beacon_safe_water" : "buoy_safe_water", null, null, 11, 7);
                break;

            case "Pæl m. tværtræ-top":
                generateAton(aton, "beacon_special_purpose", null, "pole", 2094, 1);
                break;

            case "Pyramide":
                generateAton(aton, "beacon_special_purpose", null, "cairn", null, 1);
                break;

            case "Radiofyr":
                generaetRadioStationAton(aton, null);
                break;

            case "Rød stage":
                // TODO: Might also be: topmark=2144
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), 2134, 1);
                break;

            case "Rørledningsbåke":
                generateAton(aton, "beacon_special_purpose", "pipeline", null, 14, 4);
                break;

            case "Retningsfyr":
                generateAton(aton, "light_minor", null, null, null, 5);
                break;

            case "sejladsbagbåke":
                generateAton(aton, "beacon_special_purpose", "leading", null, 9, 7);
                break;

            case "Sejladsforbåke":
                generateAton(aton, "beacon_special_purpose", "leading", null, 12, 7);
                break;

            case "Skydesignal":
                generateAton(aton, "beacon_special_purpose", "firing_danger_area", null, 0, 5);
                break;

            case "Sluse- og kanalsignal":
                generateAton(aton, "beacon_special_purpose", null, null, 0, 5);
                break;

            case "Specialafmærkning":
                // TODO: Might also be: topmark=null
                topMark = shortDesc.equals("PORT u/top") ? null : 2;
                if (masterType.isLight()) {
                    // Possibly a data error...
                    osmType = "light";
                } else if (masterType.isBeacon()) {
                    topMark = 4;
                    osmType = "beacon_special_purpose";
                    osmShape = getBeaconShape(masterType);
                } else {
                    topMark = 4;
                    osmType = "buoy_special_purpose";
                }
                generateAton(aton, osmType, null, osmShape, topMark, 4);
                break;

            case "Specialbåke":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), null, null);
                break;

            case "stage med X-top":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), 2134, 1);
                break;

            case "stage med Y-top":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), 2144, 1);
                break;

            case "Stang m.firkantet plade-top":
                generateAton(aton, "beacon_special_purpose", null, "pole", 2054, 4);
                break;

            case "Sten-båke i gul tønde, m/top":
                generateAton(aton, "beacon_special_purpose", null, "cairn", 2014, 4);
                break;

            case "Styrbord båke - Trekant op":
                // NB: In AFM this was defined with color=1 - wrong methinks
                generateAton(aton, "beacon_lateral", "starboard", getBeaconShape(masterType), 2034, 2);
                break;

            case "Styrbord molefyr":
                generateAton(aton, "light_minor", null, null, null, 2);
                break;

            case "Styrbord sideafmærkning":
                topMark = shortDesc.equals("STAR u/top") ? null : 1;
                if (masterType.isLight()) {
                    osmType = "light_minor";
                } else if (masterType.isBeacon()) {
                    osmType = "beacon_lateral";
                    osmCateogry = "starboard";
                    osmShape = getBeaconShape(masterType);
                } else {
                    osmType = "buoy_lateral";
                    osmCateogry = "starboard";
                    osmShape = "conical"; // Guessing
                }
                generateAton(aton, osmType, osmCateogry, osmShape, topMark, 2);
                break;

            case "Supertønde":
                generateAton(aton, "buoy_special_purpose", null, "super-buoy", null, 7);
                break;

            case "Tågelys":
                generateAton(aton, "light", null, null, null, 4);
                break;

            case "Timeglas-båke, m/top":
                generateAton(aton, "beacon_special_purpose", null, "tower", 2044, 6);
                break;

            case "Treben (jern) + trekant-top":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), 2074, 1);
                break;

            case "Trebenet jernbåke":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), null, 1);
                break;

            case "Tremmeværk":
                generateAton(aton, "beacon_special_purpose", null, getBeaconShape(masterType), null, 1);
                break;

            case "Varde":
                generateAton(aton, "beacon_special_purpose", null, "cairn", null, 1);
                break;

            case "Vindmølle":
                generateAton(aton, "landmark", "windmotor", null, null, null);
                break;

            case "Vinkelfyr":
                if (masterType.isLight()) {
                    osmType = "light";
                } else if (masterType.isBeacon()) {
                    osmType = "beacon_special_purpose";
                    osmShape = getBeaconShape(masterType);
                }
                generateAton(aton, osmType, null, osmShape, null, 5);
                break;
        }

        return new Integer[] { null, null };
    }


    /**
     * Generates the given OSM type.
     *
     * Important: The light details are handled by other Excel imports.
     *
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Lights">OpenStreetMap Light definitions</a>
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Beacons">OpenStreetMap Beacon definitions</a>
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Buoys>OpenStreetMap Buoy definitions</a>
     *
     * @param aton the AtoN to generate
     * @param osmType the OSM type
     * @param osmCategory the OSM category
     * @param osmShape the OSM shape
     * @param topMark the AFM top-mark ID
     * @param color the AFM color code
     * @param pattern the AFM colour pattern
     */
    void generateAton(AtonNode aton, String osmType, String osmCategory, String osmShape, Integer topMark, Integer color, String pattern) {
        aton.updateTag("seamark:type", osmType);
        aton.updateTag(String.format("seamark:%s:category", osmType), osmCategory);
        if (StringUtils.isNotBlank(osmShape)) {
            aton.updateTag(String.format("seamark:%s:shape", osmType), osmShape);
        }
        generateTopmark(aton, topMark);
        generateColor(aton, osmType, color, pattern);
    }


    /** Short-cut method **/
    void generateAton(AtonNode aton, String osmType, String osmCategory, String osmShape, Integer topMark, Integer color) {
        generateAton(aton, osmType, osmCategory, osmShape, topMark, color, null);
    }


    /** Short-cut method **/
    void generateAton(AtonNode aton, String osmType, Integer topMark) {
        generateAton(aton, osmType, null, null, topMark, null);
    }


    /*************************/
    /** AtoN Radio Stations **/
    /*************************/

    /**
     * Generates a radio station AtoN
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Radio_Stations">OpenStreetMap definitions</a>
     */
    private void generaetRadioStationAton(AtonNode aton, String category) {
        aton.updateTag("seamark:type", "radio_station");
        aton.updateTag("seamark:radio_station:category", category);
    }


    /*************************/
    /** AtoN Top-marks      **/
    /*************************/

    /**
     * Generate topmark data for the given AtoN
     * Based on the TOPBETEGNELSE table
     * @param aton the AtoN to generate topmark data for
     * @param topmark the topmark key
     */
    public AtonNode generateTopmark(AtonNode aton, Integer topmark) {
        if (topmark == null) {
            return aton;
        }

        // TODO: These mappings badly need to be verified
        switch (topmark) {
            case    1: return generateTopmark(aton, "cone, point up", parseColor(2));
            case    2: return generateTopmark(aton, "cylinder", parseColor(1));
            case    3: return generateTopmark(aton, "sphere", parseColor(5));
            case    4: return generateTopmark(aton, "x-shape", parseColor(4));
            case    5: return generateTopmark(aton, "sphere", parseColor(3));
            case    9: return generateTopmark(aton, "cone, point down", parseColor(12));
            case   11: return generateTopmark(aton, "sphere", parseColor(1));
            case   12: return generateTopmark(aton, "cone, point up", parseColor(12));

            case   13: return generateTopmark(aton, "sphere", parseColor(16)); // ?? "Diskos, DISC"
            case   14: return generateTopmark(aton, "rhombus", parseColor(7));
            case   20: return generateTopmark(aton, "x-shape", parseColor(4));
            case   23: return generateTopmark(aton, "2 cones up", parseColor(12));
            case   24: return generateTopmark(aton, "2 cones base together", parseColor(13));
            case   25: return generateTopmark(aton, "2 cones down", parseColor(11));
            case   26: return generateTopmark(aton, "2 cones point together", parseColor(10));
            case   27: return generateTopmark(aton, "2 spheres", parseColor(22));
            case   28: return generateTopmark(aton, "cone, point up", parseColor(2));
            case   29: return generateTopmark(aton, "square", parseColor(1));
            case 2004: return generateTopmark(aton, "cylinder", parseColor(6));
            //case 2014: return generateTopmark(aton, "sten", parseColor(4));  // ?? "Sten"
            case 2034: return generateTopmark(aton, "cone, point up", parseColor(6));
            case 2044: return generateTopmark(aton, "2 cones point together", parseColor(6)); //?? Timeglas
            case 2054: return generateTopmark(aton, "square", parseColor(4));
            case 2074: return generateTopmark(aton, "triangle, point down", parseColor(1));
            case 2094: return generateTopmark(aton, "besom, point down", parseColor(1)); //?? Tværtræ
            case 2104: return generateTopmark(aton, "rhombus", parseColor(1));
            case 2134: return generateTopmark(aton, "x-shape", parseColor(1));
            case 2144: return generateTopmark(aton, "besom, point down", parseColor(1));
            case 2164: return generateTopmark(aton, "cone, point down", parseColor(1));
        }
        return aton;
    }


    /** Generate the topmark shape and color tags **/
    private AtonNode generateTopmark(AtonNode aton, String shape, String color) {
        if (StringUtils.isNotBlank(shape)) {
            aton.updateTag("seamark:topmark:shape", "cylinder");
        }
        if (StringUtils.isNotBlank(color)) {
            aton.updateTag("seamark:topmark:color", color);
        }
        return aton;
    }


    /** Generates the default beacon shape */
    private String getBeaconShape(AtonType masterType) {
        // Best we can do without to proper data
        switch (masterType) {
            case STAKE: return "stake";
        }
        return null;
    }


    /*************************/
    /** AtoN Type Parsing   **/
    /*************************/

    /** Parses the "KARAKNR" field into individual types **/
    Set<AtonType> parseType(int type) {
        // Each digit denotes a separate type
        return Arrays.stream(String.valueOf(type).split(""))
                .map(Integer::valueOf)
                .map(AtonType::findByCode)
                .collect(Collectors.toSet());
    }


    /** Resolves the master type of the type set **/
    AtonType masterType(Set<AtonType> types) {
        // Single-type case
        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Check for one of the master types
        AtonType[] masterTypes = { AtonType.LIGHT, AtonType.LIGHT_MINOR, AtonType.LIGHT_BUOY };
        return Arrays.stream(masterTypes)
                .filter(types::contains)
                .findFirst()
                .orElse(types.iterator().next());
    }


    /*************************/
    /** Colour Parsing     **/
    /*************************/

    /**
     * Parses the color code and returns the corresponding OSM color string.
     *
     * The mapping is based on the "Farver" AFM table.
     *
     * @param color the color code
     * @return the corresponding OSM color string
     */
    String parseColor(Integer color) {
        if (color == null) {
            return null;
        }

        switch (color) {
            case    1: return "red";
            case    2: return "green";
            case    3: return "white";
            case    4: return "yellow";
            case    5: return "black";
            case    6: return "orange";
            case    7: return "red;white";
            case    8: return "green;red;green";
            case    9: return "red;green;red";
            case   10: return "yellow;black;yellow";
            case   11: return "yellow;black";
            case   12: return "black;yellow";
            case   13: return "black;yellow;black";
            case   14: return "blue";
            case   15: return "black;red;black";
            case   16: return "white;red";
            case   17: return "yellow";
            case   19: return "white;red;red;white";
            case   21: return "green;white";
            case   22: return "black;black";
            case   23: return "amber";
            case 2004: return "white;red;green";
            case 2044: return "white;green";
            case 2084: return "red;green";
            case 2114: return "white;green;red";
            case 2124: return "red;white";
            case 2134: return "green;white";
            case 2144: return "green;red";
            case 2224: return "blue;yellow";
            case 2164: return "red;green";
        }
        return null;
    }


    /**
     * Generates the color for the given OSM type.
     *
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Colours">OpenStreetMap definitions</a>
     *
     * @param aton the AtoN to generate color for
     * @param osmType the OSM type
     * @param color the color code
     */
    void generateColor(AtonNode aton, String osmType, Integer color, String pattern) {
        String osmColor = parseColor(color);
        if (osmType != null && osmColor != null) {
            aton.updateTag(String.format("seamark:%s:colour", osmType), osmColor);
            if (pattern != null) {
                aton.updateTag(String.format("seamark:%s:colour_pattern", osmType), osmColor);
            }
        }
    }
}
