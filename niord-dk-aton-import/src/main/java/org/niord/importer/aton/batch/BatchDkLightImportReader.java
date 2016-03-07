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
 * Reads lights from the DB
 */
@Named
public class BatchDkLightImportReader extends AbstractDkAtonImportReader {

    public static final String[] FIELDS = {
            "Farvand", "Farvandsafsnit", "NR_DK", "NR_INT", "AFM_navn", "Lokalitet",
            "Fyrkarakter", "Taagesignal", "Flammehoejde_1", "Flammehoejde_2", "Flammehoejde_3", "Flammehoejde_4",
            "Lysstyrke_1", "Lysstyrke_2", "Lysstyrke_3", "Fyrudseende", "Fyrbygnings_hoejde", "Lysvinkler",
            "Braendetid", "Bemaerkninger", "Ajourfoert_dato", "STATUS", "AFM_NR", "LATITUDE", "LONGITUDE", "Note" };

    /** {@inheritDoc} **/
    @Override
    public String[] getFields() {
        return FIELDS;
    }
}
