package org.niord.importer.aton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.model.AtonNode;
import org.niord.core.model.User;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helper class used for converting the legacy "AFM" data into OSM seamark format.
 */
public class AfmAtonImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "AFMSTATION", "FYRLBNR_DK", "AFM_NAVN", "PLADSNAVN", "AFUFORKORTELSE", "BESKRIVELSE",
            "LATTITUDE", "LONGITUDE", "KARAKNR", "EJER", "KARAKNR", "AJF_BRUGER", "AJF_DATO" };

    /** Each AtoN will have a combination of these values in the "KARAKNR" field **/
    enum AtonType {

        AIS(0),             // 0: AIS
        LIGHT_MAJOR(1),     // 1: Fyr
        LIGHT(3),           // 2: Bifyr, tågelys, advarselslys, retningsfyr, hindringslys, m.v.
        BEACON(3),          // 3: Båker, Signalmaster
        RACON(4),           // 4: RACONS
        LIGHT_BUOY(5),      // 5: Lystønder
        BUOY(6),            // 6: Vagere
        STAKE(7),           // 7: Stager i bund
        RADIO_BEACON(8),    // 8: Radiofyr
        FOG_SIGNAL(9);      // 9: Tågesignaler

        int code;
        private AtonType(int code) {
            this.code = code;
        }

        static AtonType findByCode(int code) {
            return Arrays.stream(values())
                    .filter(t -> t.code == code)
                    .findFirst()
                    .orElse(null);
        }

        public boolean isLight() {
            return this == LIGHT_MAJOR || this == LIGHT;
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

        aton.updateTag(CUST_TAG_ATON_UID, stringValue("AFMSTATION"));
        if (StringUtils.isNotBlank(stringValue("FYRLBNR_DK"))) {
            aton.updateTag(CUST_TAG_LIGHT_NUMBER, stringValue("FYRLBNR_DK"));
        }

        Integer[] topmarkAndColor = generateAtoN(
                aton,
                stringValue("AFUFORKORTELSE"),
                stringValue("BESKRIVELSE"),
                numericValue("KARAKNR").intValue());

        // Emit topmark
        //if (topmarkAndColor[0] != null) {
        //    generateTopmark(aton, topmarkAndColor[0]);
        //}



        return aton;
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
        AtonType[] masterTypes = { AtonType.LIGHT_MAJOR, AtonType.LIGHT, AtonType.LIGHT_BUOY };
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

    /**************************/
    /** Generating AtoN OSM  **/
    /**************************/

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
    void generateAtoN(AtonNode aton, String osmType, String osmCategory, String osmShape, Integer topMark, Integer color, String pattern) {
        aton.updateTag("seamark:type", osmType);
        aton.updateTag(String.format("seamark:%s:category", osmType), osmCategory);
        if (StringUtils.isNotBlank(osmShape)) {
            aton.updateTag(String.format("seamark:%s:shape", osmType), osmShape);
        }
        generateTopmark(aton, topMark);
        generateColor(aton, osmType, color, pattern);
    }

    /** Short-cut method **/
    void generateAtoN(AtonNode aton, String osmType, String osmCategory, String osmShape, Integer topMark, Integer color) {
        generateAtoN(aton, osmType, osmCategory, osmShape, topMark, color, null);
    }

    /** Short-cut method **/
    void generateAtoN(AtonNode aton, String osmType, String osmCategory, String osmShape, Integer topMark) {
        generateAtoN(aton, osmType, osmCategory, osmShape, topMark, null);
    }

    /** Short-cut method **/
    void generateAtoN(AtonNode aton, String osmType, Integer topMark) {
        generateAtoN(aton, osmType, null, null, topMark, null);
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
    public Integer[] generateAtoN(AtonNode aton, String shortDesc, String description, int type) {
        if (StringUtils.isBlank(description)) {
            return new Integer[] { null, null };
        }

        // An AtoN consists of a master type (e.g. "light") and a set of slave types (e.g. AIS)
        // Currently, only master is handled, and the slave types are left for the other Excel imports.
        Set<AtonType> types = parseType(type);
        AtonType masterType = masterType(types);

        Integer topMark = null;
        String osmType = null;
        String osmCateogry = null;
        String osmShape = null;
        switch (description) {

            case "AIS Syntetisk AtoN":
                generaetAisAton(aton, "s-ais");
                break;
            case "AIS Virtuel AtoN":
                generaetAisAton(aton, "v-ais");
                break;

            case "Anduvningsfyr":
                generateAtoN(aton, "light_major", 3);
                break;

            case "Bagbord båke - CAN":
                generateAtoN(aton, "beacon_lateral", "port", "stake", 2004, 1);
                break;

            case "Bagbord båke - Trekant ned":
                generateAtoN(aton, "beacon_lateral", "port", "stake", 2164, 1);
                break;

            case "Bagbord sideafmærkning":
                topMark = shortDesc.equals("PORT u/top") ? null : 2;
                if (masterType.isLight()) {
                    osmType = "light_minor";
                } else if (masterType.isBeacon()) {
                    osmType = "beacon_lateral";
                    osmCateogry = "port";
                    osmShape = "stake";
                } else {
                    osmType = "buoy_lateral";
                    osmCateogry = "port";
                    osmShape = "can";
                }
                generateAtoN(aton, osmType, osmCateogry, osmShape, topMark, 1);
                break;

            case "Bagbord skillepkts.afmærkning":
            case "Bagbords skillepunktsafmærkn.":
                generateAtoN(aton, "buoy_lateral", "preferred_channel_starboard", "can", 2, 9);
                break;

            case "Bagbords molefyr":
                generateAtoN(aton, "light_minor", null, null, null, 1);
                break;

            case "Bifyr med vinkler":
            case "Bifyr, lokalt advarselsfyr":
            case "Bagfyr":
                generateAtoN(aton, "light_minor", null, null, null, 5);
                break;

            case "Båke (Keglestub) u/top":
                //generateAtoN(aton, "???", "???", "???", null, 4);
                break;

            case "Båke med firkantet plade-top":
                //generateAtoN(aton, "???", "???", "???", 2054, 4);
                break;

            case "Bropassage, bagbord": return new Integer[] { 29, 1 };
            case "Bropassage, styrbord": return new Integer[] { 28, 2 };
            case "Bropassagesignal": return new Integer[] { null, 1 };
            case "CAN-båke i gul tønde, m/top": return new Integer[] { 2004, 4 };
            case "Emergency Wreck Marking Buoy": return new Integer[] { null, 2224 };
            case "Firkantet båke u/top": return new Integer[] { null, 1 };
            case "Forbåke m/top": return new Integer[] { 28, 1 };
            case "Forfyr": return new Integer[] { null, 5 };
            case "Fredningsbåke": return new Integer[] { 4, 4 };
            case "Fundet via Dansk Fyrliste": return new Integer[] { null, 23 };
            case "Fyrtårn": return new Integer[] { null, 3 };
            case "Grave-bagbåke": return new Integer[] { 20, 3 };
            case "Grave-forbåke": return new Integer[] { 20, 3 };
            case "Havn / fredningsomr. bagbåke": return new Integer[] { 4, 4 };
            case "Havn / fredningsomr. forbåke": return new Integer[] { 4, 4 };
            case "Hvid stage med rød kugletop": return new Integer[] { 11, 3 };
            case "ikke afmærket punkt": return new Integer[] { null, 5 };
            case "Isoleret fareafmærkning": return new Integer[] { 27, 15 };
            case "Jernstang m. diamant-top": return new Integer[] { 2104, 1 };
            case "Kabel bagbåke": return new Integer[] { 14, 7 };
            case "Kabel forbåke": return new Integer[] { 13, 16 };
            case "Kabelskilt": return new Integer[] { null, null };
            case "Kompasafmærkning Ø for.": return new Integer[] { 24, 13 };
            case "Kompasafmærkning N for.": return new Integer[] { 23, 12 };
            case "Kompasafmærkning S for.": return new Integer[] { 25, 11 };
            case "Kompasafmærkning V for.": return new Integer[] { 26, 10 };
            case "Mellemfyr": return new Integer[] { null, 5 };
            case "Meteorologimast": return new Integer[] { null, 5 };
            case "Midtfarvandsafmærkning": return new Integer[] { 11, 7 };
            case "Pæl m. tværtræ-top": return new Integer[] { 2094, 1 };
            case "Pyramide": return new Integer[] { null, 1 };
            case "Radiofyr": return new Integer[] { null, 5 };
            case "Radiopejlestation": return new Integer[] { null, 5 };
            case "Rød stage": return new Integer[] { 2134, 1 };             // TODO: Might also be: { 2144, 1 }
            case "Rørledningsbåke": return new Integer[] { 14, 4 };
            case "Retningsfyr": return new Integer[] { null, 5 };
            case "sejladsbagbåke": return new Integer[] { 9, 7 };
            case "Sejladsforbåke": return new Integer[] { 12, 7 };
            case "Skydesignal": return new Integer[] { null, 5 };
            case "Sluse- og kanalsignal": return new Integer[] { null, 5 };
            case "Specialafmærkning": return new Integer[] { 4, 4 };        // TODO: Might also be: { null, 4 }
            case "Specialbåke": return new Integer[] { null, null };
            case "Stage i tønde m. firkant-top": return new Integer[] { 29, 1 };
            case "stage med X-top": return new Integer[] { 2134, 1 };
            case "stage med Y-top": return new Integer[] { 2144, 1 };
            case "Stang m.firkantet plade-top": return new Integer[] { 2054, 4 };
            case "Sten-båke i gul tønde, m/top": return new Integer[] { 2014, 4 };
            case "Styrbord båke - Trekant op": return new Integer[] { 2034, 1 };
            case "Styrbord molefyr": return new Integer[] { null, 2 };
            case "Styrbord sideafmærkning": return shortDesc.equals("STAR u/top") ? new Integer[] { null, 2 } : new Integer[] { 1, 2 };
            case "Supertønde": return new Integer[] { null, 7 };
            case "Tågelys": return new Integer[] { null, 4 };
            case "Timeglas-båke, m/top": return new Integer[] { 2044, 6 };
            case "Transformerplatform": return new Integer[] { null, 3 };
            case "Treben (jern) + trekant-top": return new Integer[] { 2074, 1 };
            case "Trebenet jernbåke": return new Integer[] { null, 1 };
            case "Tremmeværk": return new Integer[] { null, 1 };
            case "Varde": return new Integer[] { null, 1 };
            case "Vindmølle": return new Integer[] { null, null };
            case "Vinkelfyr": return new Integer[] { null, 5 };
        }

        return new Integer[] { null, null };
    }


    /**
     * Generates AIS AtoN
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Radio_Stations">OpenStreetMap definitions</a>
     */
    private void generaetAisAton(AtonNode aton, String category) {
        aton.updateTag("seamark:type", "radio_station");
        aton.updateTag("seamark:radio_station:category", category);
    }


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
}
