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

import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonTag;
import org.niord.core.user.User;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * DGPS batch processor used for converting the legacy "AFM" DGPS Excel rows into OSM seamark format.
 *
 * The AtoN model adheres to the OSM seamark specification, please refer to:
 * http://wiki.openstreetmap.org/wiki/Key:seamark
 * and sub-pages.
 */
@Named
public class BatchDkDgpsImportProcessor extends AbstractDkAtonImportProcessor {

    @Inject
    Logger log;

    /** {@inheritDoc} **/
    @Override
    protected AtonNode parseAtonExcelRow() throws Exception {

        String dgpsNr = String.valueOf(numericValue("NR_DK").intValue());

        // Only process active AIS
        if (!"DRIFT".equalsIgnoreCase(stringValue("STATUS"))) {
            getLog().info("Skipping inactive DGPS " + dgpsNr);
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
        String atonUid = "dgps-" + dgpsNr;
        aton.updateTag(AtonTag.TAG_ATON_UID, atonUid);
        aton.updateTag("seamark:name", stringValue("AFM_navn"));

        aton.updateTag("seamark:type", "radio_station");

        // Category
        aton.updateTag("seamark:radio_station:category", "dgps");

        // Frequency - in Hertz
        aton.updateTag("seamark:radio_station:frequency", String.valueOf((int)(numericValue("Frekvens_kHz") * 1000)));

        // Range
        aton.updateTag("seamark:radio_station:range", String.valueOf(numericValue("Raekkevide_sm")));

        // From http://wiki.openstreetmap.org/wiki/Key:radar_transponder
        aton.updateTag("man_made", "monitoring_station");
        aton.updateTag("monitoring:gps", "yes");

        return aton;
    }

}
