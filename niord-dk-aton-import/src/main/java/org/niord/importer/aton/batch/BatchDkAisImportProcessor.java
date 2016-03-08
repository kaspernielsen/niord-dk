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
