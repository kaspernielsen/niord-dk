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
 * Reads AIS from Excel
 */
@Named
public class BatchDkRaconImportReader extends AbstractDkAtonImportReader {

    public static final String[] FIELDS = {
            "NR_DK", "NR_INT", "AFM_NR", "AFM_navn", "Radarbaand", "Identifikation", "Tidsinterval", "STATUS",
            "LATITUDE", "LONGITUDE", "Ajourfoert_dato", "Retning_mod_fyret" };

    /** {@inheritDoc} **/
    @Override
    public String[] getFields() {
        return FIELDS;
    }
}
