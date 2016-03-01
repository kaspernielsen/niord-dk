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
 * Reads AtoNs from the DB
 */
@Named
public class BatchAtonImportReader extends BaseAtonImportReader {

    public static final String[] FIELDS = {
            "AFMSTATION", "FYRLBNR_DK", "AFM_NAVN", "PLADSNAVN", "AFUFORKORTELSE", "BESKRIVELSE",
            "LATTITUDE", "LONGITUDE", "KARAKNR", "EJER", "KARAKNR", "AJF_BRUGER", "AJF_DATO" };

    /** {@inheritDoc} **/
    @Override
    public String[] getFields() {
        return FIELDS;
    }
}
