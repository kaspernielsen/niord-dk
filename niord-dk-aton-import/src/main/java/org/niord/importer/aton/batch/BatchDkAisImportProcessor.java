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
import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonTag;
import org.niord.core.user.User;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * AtoN batch processor used for converting the legacy "AFM" Light Excel row into OSM seamark format.
 * Before running this batch job, run the dk-aton-import job.
 *
 * The AtoN model adheres to the OSM seamark specification, please refer to:
 * http://wiki.openstreetmap.org/wiki/Key:seamark
 * and sub-pages.
 */
@Named
public class BatchDkAisImportProcessor extends AbstractDkAtonImportProcessor {

    @Inject
    Logger log;

    /** {@inheritDoc} **/
    @Override
    protected AtonNode parseAtonExcelRow() throws Exception {

        String aisNr = String.valueOf(numericValue("NR_DK").intValue());

        // Only process active AIS
        if (!"DRIFT".equalsIgnoreCase(stringValue("STATUS"))) {
            getLog().info("Skipping inactive AIS " + aisNr);
            return null;
        }

        // Only process AIS with known AFM-NR
        String atonUid = stringValue("AFM_NR");
        if (StringUtils.isBlank(atonUid)) {
            getLog().info("Skipping AIS without AFM-NR " + aisNr);
            return null;
        }

        User user = job.getUser();

        AtonNode aton = new AtonNode();

        aton.setVisible(true);
        aton.setLat(numericValue("LATITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("Ajourfoert_dato"));
        aton.setUser(user != null ? user.getUsername() : "");
        aton.setUid(user != null ? user.getId() : -1);
        aton.setChangeset(getChangeSet());
        aton.setVersion(1);     // Unknown version

        // If no AtoN UID exists, construct it
        aton.updateTag(AtonTag.TAG_ATON_UID, atonUid);
        aton.updateTag(AtonTag.TAG_AIS_NUMBER, aisNr);
        aton.updateTag("seamark:name", stringValue("AFM_navn"));

        aton.updateTag("seamark:type", "radio_station");

        // Category - NB: spelling mistake intentional
        aton.updateTag("seamark:radio_station:category", "Virituel".equals(stringValue("Type")) ? "v-ais" : "ais");

        // Call-sign
        aton.updateTag("seamark:radio_station:callsign", stringValue("Identifikation"));

        // MMSI
        aton.updateTag("seamark:radio_station:mmsi", String.valueOf(numericValue("MMSI_NR").intValue()));

        // From http://wiki.openstreetmap.org/wiki/Key:radar_transponder
        aton.updateTag("radio_transponder:AIS", "yes");

        return aton;
    }


    /** {@inheritDoc} */
    @Override
    protected void mergeAtonNodes(AtonNode original, AtonNode aton) {

        // Do not override the original AtoN type
        String origType = original.getTagValue("seamark:type");
        if (origType != null) {
            aton.removeTags("seamark:type");
        }

        // Remove any AIS information from the original
        original.removeTags("seamark:radio_station\\.*");

        // Override any remaining tags in the original
        original.updateNode(aton);
    }

}
