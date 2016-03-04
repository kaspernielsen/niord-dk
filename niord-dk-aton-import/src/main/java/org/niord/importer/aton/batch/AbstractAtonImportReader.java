/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
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
public abstract class AbstractAtonImportReader extends AbstractItemHandler {

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
            return new BatchAtonItem(colIndex, rowIterator.next());
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
