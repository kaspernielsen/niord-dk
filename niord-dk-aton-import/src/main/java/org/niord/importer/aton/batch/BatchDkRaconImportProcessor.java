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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AtoN batch processor used for converting the legacy "AFM" RACON Excel row into OSM seamark format.
 * Before running this batch job, run the dk-aton-import job.
 *
 * The AtoN model adheres to the OSM seamark specification, please refer to:
 * http://wiki.openstreetmap.org/wiki/Key:seamark
 * and sub-pages.
 *
 * RACONs are also documented at:
 * http://wiki.openstreetmap.org/wiki/Key:radar_transponder
 */
@Named
public class BatchDkRaconImportProcessor extends AbstractDkAtonImportProcessor {

    public static final Pattern PERIOD_FORMAT = Pattern.compile("^(\\d+)[ ]*s?");

    public static final Pattern SECTOR_FORMAT = Pattern.compile("^(?<start>\\d+)°[-]?(?<end>\\d+)?°?");

    @Inject
    Logger log;

    /** {@inheritDoc} **/
    @Override
    protected AtonNode parseAtonExcelRow() throws Exception {

        String raconNr = String.valueOf(numericValue("NR_DK").intValue());

        // Only process active RACONS
        if (!"DRIFT".equalsIgnoreCase(stringValue("STATUS"))) {
            getLog().info("Skipping inactive RACON " + raconNr);
            return null;
        }

        // Only process RACON with known AFM-NR
        String atonUid = stringValue("AFM_NR");
        if (StringUtils.isBlank(atonUid)) {
            getLog().info("Skipping RACON without AFM-NR " + raconNr);
            return null;
        }

        User user = job.getUser();

        AtonNode aton = new AtonNode();

        aton.setVisible(true);
        aton.setLat(numericValue("LATITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValueOrNull("Ajourfoert_dato"));
        aton.setUser(user != null ? user.getUsername() : "");
        aton.setUid(user != null ? user.getId() : -1);
        aton.setChangeset(getChangeSet());
        aton.setVersion(1);     // Unknown version

        // If no AtoN UID exists, construct it
        aton.updateTag(AtonTag.TAG_ATON_UID, atonUid);
        aton.updateTag(AtonTag.TAG_RACON_NUMBER, raconNr);
        aton.updateTag(AtonTag.TAG_INT_RACON_NUMBER, String.valueOf(numericValue("NR_INT").intValue()));
        aton.updateTag("seamark:name", stringValue("AFM_navn"));

        aton.updateTag("seamark:type", "radar_transponder");

        // Category
        aton.updateTag("seamark:radar_transponder:category", "racon");

        // Wave length
        aton.updateTag("seamark:radar_transponder:wavelength", stringValue("Radarbaand"));

        // Group
        aton.updateTag("seamark:radar_transponder:group", stringValue("Identifikation"));

        // Period
        aton.updateTag("seamark:radar_transponder:period", parsePeriod(stringValue("Tidsinterval")));

        // Sectors
        String[] sectors = parseSectors(stringValue("Retning_mod_fyret"));
        if (sectors.length == 2) {
            aton.updateTag("seamark:radar_transponder:sector_start", sectors[0]);
            aton.updateTag("seamark:radar_transponder:sector_end", sectors[1]);
        } else if (sectors.length == 1) {
            aton.updateTag("seamark:radar_transponder:orientation", sectors[0]);
        }

        return aton;
    }


    /** Parses the period format, e.g. "60 s" */
    private String parsePeriod(String period) {
        if (StringUtils.isNotBlank(period)) {
            Matcher m = PERIOD_FORMAT.matcher(period);
            if (m.matches()) {
                return m.group(1);
            }
        }
        return null;
    }


    /** Parses the sector format, e.g. "360°" or "20°-280°" */
    public String[] parseSectors(String sector) {
        if (StringUtils.isNotBlank(sector)) {
            Matcher m = SECTOR_FORMAT.matcher(sector);
            if (m.find()) {
                String start = m.group("start");
                String end = m.group("end");
                if (StringUtils.isNotBlank(end)) {
                    return new String[] { start, end };
                } else {
                    return new String[] { start };
                }
            }
        }
        return new String[0];
    }


    /** {@inheritDoc} */
    @Override
    protected void mergeAtonNodes(AtonNode original, AtonNode aton) {

        // Do not override the original AtoN type
        String origType = original.getTagValue("seamark:type");
        if (origType != null) {
            aton.removeTags("seamark:type");
        }

        // Remove any RACON information from the original
        original.removeTags("seamark:radar_transponder\\.*");

        // Override any remaining tags in the original
        original.updateNode(aton);
    }

}
