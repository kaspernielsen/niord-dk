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

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.batch.AbstractItemHandler;

import java.io.FileInputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class for Excel-based AtoN import batch reader classes
 */
public abstract class AbstractDkAtonImportReader extends AbstractItemHandler {

    Map<String, Integer> colIndex = new HashMap<>();
    Iterator<Row> rowIterator;
    int totalRowNo;
    int row = 0;

    /**
     * Returns the column fields to read in the Header row
     * @return the fields to read in the header row
     */
    public abstract String[] getFields();


    /** {@inheritDoc} **/
    @Override
    public void open(Serializable prevCheckpointInfo) throws Exception {

        // Get hold of the data file
        Path path = batchService.getBatchJobDataFile(jobContext.getInstanceId());

        // Parse the header row of the Excel file and build a column index
        rowIterator = parseHeaderRow(path, colIndex, getFields());

        // Fast forward to the previous row index
        if (prevCheckpointInfo != null) {
            row = (Integer) prevCheckpointInfo;
        }
        for (int r = 0; r < row && rowIterator.hasNext(); r++) {
            rowIterator.next();
        }

        getLog().info("Start processing Excel from row " + row);
    }


    /** {@inheritDoc} **/
    @Override
    public Object readItem() throws Exception {
        if (rowIterator.hasNext()) {

            // Every now and then, update the progress
            if (row % 10 == 0) {
                updateProgress((int)(100.0 * row / totalRowNo));
            }

            getLog().info("Reading row " + row);
            row++;
            return new BatchDkAtonItem(colIndex, rowIterator.next());
        }
        return null;
    }


    /** {@inheritDoc} **/
    @Override
    public Serializable checkpointInfo() throws Exception {
        return row;
    }


    /**
     * Opens the Excel sheet, reads in the header row and build a map of the column indexes for the given header fields.
     * @param path a path to the the Excel sheet
     * @param colIndex the column index map
     * @param fields the fields to determine column indexes for
     * @return the Excel row iterator pointing to the first data row
     */
    private Iterator<Row> parseHeaderRow(Path path, Map<String, Integer> colIndex, String[] fields) throws Exception {

        try (FileInputStream inputStream = new FileInputStream(path.toFile())) {
            // Create Workbook instance holding reference to .xls file
            HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
            // Get first/desired sheet from the workbook
            HSSFSheet sheet = workbook.getSheetAt(0);

            totalRowNo = sheet.getLastRowNum();

            // Get row iterator
            Iterator<Row> rowIterator = sheet.iterator();
            Row headerRow = rowIterator.next();

            // Get the column indexes of the relevant columns
            Arrays.stream(fields).forEach(f -> updateColumnIndex(headerRow, colIndex, f));

            return rowIterator;
        }
    }


    /** Determines the column index of the given column name */
    private boolean updateColumnIndex(Row headerRow, Map<String, Integer> colIndex, String colName) {
        int index = 0;
        for (Cell cell : headerRow) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING &&
                    colName.equalsIgnoreCase(cell.getStringCellValue())) {
                colIndex.put(colName, index);
                return true;
            }
            index++;
        }
        return false;
    }

}
