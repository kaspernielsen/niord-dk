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
