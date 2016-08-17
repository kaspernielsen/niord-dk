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
