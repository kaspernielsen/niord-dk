package org.niord.importer.aton;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.model.User;

import java.util.Date;
import java.util.Map;

/**
 * Base class for Excel-based AtoN import helper classes
 */
public abstract class BaseAtonImportHelper {

    public static final String CUST_TAG_ATON_UID            = "seamark_x:aton_uid";
    public static final String CUST_TAG_LIGHT_NUMBER        = "seamark_x:light_number";
    public static final String CUST_TAG_INT_LIGHT_NUMBER    = "seamark_x:int_light_number";

    final protected User user;
    final protected int changeset;
    final protected Map<String, Integer> colIndex;
    final protected Row row;

    /** Constructor **/
    public BaseAtonImportHelper(User user, int changeset, Map<String, Integer> colIndex, Row row) {
        this.user = user;
        this.changeset = changeset;
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
}
