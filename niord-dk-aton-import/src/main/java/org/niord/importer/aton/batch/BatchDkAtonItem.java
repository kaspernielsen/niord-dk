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

import org.apache.poi.ss.usermodel.Row;

import java.util.Map;

/**
 * Encapsulates a row in an Excel sheet + an index of column indexes.
 */
@SuppressWarnings("unused")
public class BatchDkAtonItem {
    Map<String, Integer> colIndex;
    Row row;

    /** Constructor */
    public BatchDkAtonItem() {
    }

    /** Constructor */
    public BatchDkAtonItem(Map<String, Integer> colIndex, Row row) {
        this.colIndex = colIndex;
        this.row = row;
    }

    public Map<String, Integer> getColIndex() {
        return colIndex;
    }

    public void setColIndex(Map<String, Integer> colIndex) {
        this.colIndex = colIndex;
    }

    public Row getRow() {
        return row;
    }

    public void setRow(Row row) {
        this.row = row;
    }
}
