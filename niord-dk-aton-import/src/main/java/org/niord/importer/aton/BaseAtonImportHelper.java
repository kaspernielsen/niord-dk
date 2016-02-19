package org.niord.importer.aton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.Date;
import java.util.Map;

/**
 * Base class for Excel-based AtoN import helper classes
 */
public abstract class BaseAtonImportHelper {

    public static final String CUST_TAG_ATON_UID            = "seamark_x:aton_uid";
    public static final String CUST_TAG_LIGHT_NUMBER        = "seamark_x:light_number";
    public static final String CUST_TAG_INT_LIGHT_NUMBER    = "seamark_x:int_light_number";

    protected Map<String, Integer> colIndex;
    protected Row row;

    /** Constructor **/
    public BaseAtonImportHelper(Map<String, Integer> colIndex, Row row) {
        this.colIndex = colIndex;
        this.row = row;
    }

    /*************************/
    /** Excel Parsing       **/
    /*************************/

    /** Returns the numeric value of the cell with the given header column key */
    Double numericValue(String colKey) {
        Cell cell = row.getCell(colIndex.get(colKey));
        return cell == null ? null : cell.getNumericCellValue();
    }

    /** Returns the string value of the cell with the given header column key */
    String stringValue(String colKey) {
        Cell cell = row.getCell(colIndex.get(colKey));
        return cell == null ? null : cell.getStringCellValue();
    }

    /** Returns the date value of the cell with the given header column key */
    Date dateValue(String colKey) {
        Cell cell = row.getCell(colIndex.get(colKey));
        return cell == null ? null : cell.getDateCellValue();
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
    Integer[] parseTopmarkAndColor(String shortDesc, String description) {
        if (StringUtils.isBlank(description)) {
            return new Integer[] { null, null };
        }

        switch (description) {
            case "AIS Syntetisk AtoN": return new Integer[] { null, 2174 };
            case "AIS Virtuel AtoN": return new Integer[] { null, 2174 };
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
            case "Kabelskilt": return new Integer[] { 2174, 2174 };
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
            case "Specialbåke": return new Integer[] { null, 2174 };
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
            case "Transformerplatform": return new Integer[] { 2174, 3 };
            case "Treben (jern) + trekant-top": return new Integer[] { 2074, 1 };
            case "Trebenet jernbåke": return new Integer[] { null, 1 };
            case "Tremmeværk": return new Integer[] { null, 1 };
            case "Varde": return new Integer[] { null, 1 };
            case "Vindmølle": return new Integer[] { 2174, 2174 };
            case "Vinkelfyr": return new Integer[] { null, 5 };
        }

        return new Integer[] { null, null };

    }

}
