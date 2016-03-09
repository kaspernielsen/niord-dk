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

import javax.inject.Named;

/**
 * Reads AIS from Excel
 */
@Named
public class BatchDkDgpsImportReader extends AbstractDkAtonImportReader {

    public static final String[] FIELDS = {
            "NR_DK", "AFM_NR", "AFM_navn", "Frekvens_kHz", "Hastighed_Baud", "Reference_station_nr", "Sendestation_nr",
            "Raekkevide_sm", "Monitering", "Meddelelses_typer", "STATUS", "LATITUDE", "LONGITUDE", "Ajourfoert_dato" };

    /** {@inheritDoc} **/
    @Override
    public String[] getFields() {
        return FIELDS;
    }
}
