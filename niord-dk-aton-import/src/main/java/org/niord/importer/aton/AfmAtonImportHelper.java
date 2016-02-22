package org.niord.importer.aton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.model.AtonNode;

import java.util.Map;

/**
 * Helper class used for converting the legacy "AFM" data into OSM seamark format.
 */
public class AfmAtonImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "AFMSTATION", "FYRLBNR_DK", "AFM_NAVN", "PLADSNAVN", "AFUFORKORTELSE",
            "BESKRIVELSE", "LATTITUDE", "LONGITUDE", "KARAKNR", "EJER", "AJF_BRUGER", "AJF_DATO" };

    /** Constructor **/
    public AfmAtonImportHelper(Map<String, Integer> colIndex, Row row) {
        super(colIndex, row);
    }

    /** Converts the current AFM row into an OSM AtoN **/
    public AtonNode afm2osm() throws Exception {
        AtonNode aton = new AtonNode();

        // TODO: aton.setId();
        aton.setVisible(true);
        aton.setLat(numericValue("LATTITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("AJF_DATO"));
        aton.setUser(stringValue("AJF_BRUGER"));
        aton.setUid(0);         // Unknown user ID
        aton.setChangeset(0);   // Unknown changeset
        aton.setVersion(1);     // Unknown version

        aton.updateTag(CUST_TAG_ATON_UID, stringValue("AFMSTATION"));
        if (StringUtils.isNotBlank(stringValue("FYRLBNR_DK"))) {
            aton.updateTag(CUST_TAG_LIGHT_NUMBER, stringValue("FYRLBNR_DK"));
        }

        Integer[] topmarkAndColor = parseTopmarkAndColor(stringValue("AFUFORKORTELSE"), stringValue("BESKRIVELSE"));

        // Emit topmark
        if (topmarkAndColor[0] != null) {
            generateTopmark(aton, topmarkAndColor[0]);
        }



        return aton;
    }

    /*
        aton.setAtonUid(row.getCell(colIndex.get("AFMSTATION")).getStringCellValue());
        aton.setName(row.getCell(colIndex.get("AFM_NAVN")).getStringCellValue());
        aton.setCode(row.getCell(colIndex.get("AFUFORKORTELSE")).getStringCellValue());
        aton.setDescription(row.getCell(colIndex.get("BESKRIVELSE")).getStringCellValue());
        aton.setOwner(row.getCell(colIndex.get("EJER")).getStringCellValue());


        // In this simplified "type" mapping, we map the "karaknr" field to the value 1-3.
        // "karaknr" values:
        // 0: AIS
        // 1: Fyr
        // 2: Bifyr, tågelys, advarselslys, retningsfyr, hindringslys, m.v.
        // 3: Båker, Signalmaster
        // 4: RACONS
        // 5: Lystønder
        // 6: Vagere
        // 7: Stager i bund
        // 8: Radiofyr
        // 9: Tågesignaler

        String karaknr = String.valueOf(Math.round(row.getCell(colIndex.get("KARAKNR")).getNumericCellValue()));
        if (karaknr.contains("5")) {
            aton.setType(2);
        } else if (karaknr.contains("6") || karaknr.contains("7")) {
            aton.setType(3);
        } else {
            aton.setType(1);
        }
    */

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
    String parseColor(int color) {
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

    /********************************/
    /** Topmark and color Parsing  **/
    /********************************/

    /**
     * Translates the AFM_FUNKTION FORKORTELSE and BESKRIVELSE fields, and returns
     * the corresponding AFM topmark and color codes.
     *
     * The mapping is based on the "AFM_FUNKTION" AFM table.
     *
     * @param shortDesc the FORKORTELSE field
     * @param description the BESKRIVELSE field
     * @return the corresponding AFM topmark and color code
     */
    public Integer[] parseTopmarkAndColor(String shortDesc, String description) {
        if (StringUtils.isBlank(description)) {
            return new Integer[] { null, null };
        }

        switch (description) {
            case "AIS Syntetisk AtoN": return new Integer[] { null, null };
            case "AIS Virtuel AtoN": return new Integer[] { null, null };
            case "Anduvningsfyr": return new Integer[] { null, 3 };
            case "Bagbord båke - CAN": return new Integer[] { 2004, 1 };
            case "Bagbord båke - Trekant ned": return new Integer[] { 2164, 1 };
            case "Bagbord sideafmærkning": return shortDesc.equals("PORT u/top") ? new Integer[] { null, 1 } : new Integer[] { 2, 1 };
            case "Bagbord skillepkts.afmærkning": return new Integer[] { 2, 9 };
            case "Bagbords molefyr": return new Integer[] { null, 1 };
            case "Bagbords skillepunktsafmærkn.": return new Integer[] { 2, 9 };
            case "Bagfyr": return new Integer[] { null, 5 };
            case "Båke (Keglestub) u/top": return new Integer[] { null, 4 };
            case "Båke med firkantet plade-top": return new Integer[] { 2054, 4 };
            case "Bifyr med vinkler": return new Integer[] { null, 5 };
            case "Bifyr, lokalt advarselsfyr": return new Integer[] { null, 5 };
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
     * Generate topmark data for the given AtoN
     * Based on the TOPBETEGNELSE table
     * @param aton the AtoN to generate topmark data for
     * @param topmark the topmark key
     */
    public AtonNode generateTopmark(AtonNode aton, int topmark) {
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
