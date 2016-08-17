/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.niord.importer.aton.batch;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonService;
import org.niord.core.aton.batch.BatchAtonImportProcessor;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for Excel-based AtoN import batch processor classes
 */
public abstract class AbstractDkAtonImportProcessor extends BatchAtonImportProcessor {

    public static final String CHANGE_SET_PROPERTY = "changeSet";

    @Inject
    AtonService atonService;

    Row row;
    Map<String, Integer> colIndex = new HashMap<>();


    /**
     * Parses the next AtonNode from the current Excel row
     * @return the parsed AtonNode
     */
    @Override
    protected AtonNode toAtonNode(Object item) throws Exception {
        BatchDkAtonItem atonItem = (BatchDkAtonItem)item;
        this.row = atonItem.getRow();
        this.colIndex = atonItem.getColIndex();

        return parseAtonExcelRow();
    }


    /**
     * Parses the next AtonNode from the current Excel row
     * @return the parsed AtonNode
     */
    protected abstract AtonNode parseAtonExcelRow() throws Exception;


    /**
     * Returns the changeSet from the batch data properties
     * @return the changeSet from the batch data properties
     */
    public int getChangeSet() {
        try {
            return (Integer)job.getProperties().get(CHANGE_SET_PROPERTY);
        } catch (Exception e) {
            return -1;
        }
    }


    /*************************/
    /** Excel Parsing       **/
    /*************************/


    /** Returns the numeric value of the cell with the given header column key */
    Double numericValue(String colKey) {
        Cell cell = row.getCell(colIndex.get(colKey));
        return cell == null ? null : cell.getNumericCellValue();
    }


    /** Returns the numeric value of the cell with the given header column key. Returns null for 0.0 */
    Double numericValueOrNull(String colKey) {
        Double val = numericValue(colKey);
        return val == null || Math.abs(val) < 0.000001 ? null : val;
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

    /** Returns the date value of the cell with the given header column key */
    Date dateValueOrNull(String colKey) {
        try {
            Cell cell = row.getCell(colIndex.get(colKey));
            return cell == null ? null : cell.getDateCellValue();
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns the string representation of the object or null if it is undefined */
    public String toString(Object o) {
        String result = o != null ? o.toString() : null;
        return StringUtils.isNotBlank(result) ? result : null;
    }

    /** Appends a value using semi-colon as a separator */
    public String appendValue(String str, Object o) {
        String val = toString(o);
        if (StringUtils.isNotBlank(val)) {
            if (str == null) {
                str = "";
            } else if (str.length() > 0) {
                str += ";";
            }
            str += val;
        }
        return str;
    }
}
