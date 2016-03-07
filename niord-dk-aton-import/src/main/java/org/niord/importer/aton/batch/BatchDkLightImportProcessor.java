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
public class BatchDkLightImportProcessor extends AbstractDkAtonImportProcessor {

    @Inject
    Logger log;

    /** {@inheritDoc} **/
    @Override
    protected AtonNode parseAtonExcelRow() throws Exception {

        User user = job.getUser();

        AtonNode aton = new AtonNode();

        aton.setVisible("DRIFT".equals(stringValue("STATUS")));
        aton.setLat(numericValue("LATITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("Ajourfoert_dato"));
        aton.setUser(user != null ? user.getUsername() : "");
        aton.setUid(user != null ? user.getId() : -1);
        aton.setChangeset(getChangeSet());
        aton.setVersion(1);     // Unknown version

        String atonUid = stringValue("AFM_NR");
        if (StringUtils.isBlank(atonUid)) {
            atonUid = "light-" + stringValue("NR_DK");
            aton.updateTag("seamark:type", "light");
        }
        aton.updateTag(AtonTag.TAG_ATON_UID, atonUid);
        aton.updateTag(AtonTag.TAG_LIGHT_NUMBER, stringValue("NR_DK"));
        aton.updateTag(AtonTag.TAG_INT_LIGHT_NUMBER, stringValue("NR_INT"));
        aton.updateTag(AtonTag.TAG_LOCALITY, stringValue("Lokalitet"));
        aton.updateTag("seamark:name", stringValue("AFM_navn"));
        aton.updateTag("seamark:light:information", stringValue("Fyrudseende"));

        aton.updateTag("seamark:light:elevation", parseElevation());

        aton.updateTag("seamark:light:range", parseRange());

        String lightChar = stringValue("Fyrkarakter");
        //String fogChar = stringValue("Taagesignal");

        LightSeamark light = DkLightParser.newInstance();

        // Parse the light character, e.g. "Iso.WRG.2s"
        DkLightParser.parseLightCharacteristics(light, lightChar);

        // Parse the light elevations
        DkLightParser.parseHeight(light, numericValueOrNull("Fyrbygnings_hoejde"));

        // Parse the light sector angles
        DkLightParser.parseLightSectorAngles(light, stringValue("Lysvinkler"));

        // Parse the light ranges
        DkLightParser.parseRange(light,
                stringValue("Lysstyrke_1"),
                stringValue("Lysstyrke_2"),
                stringValue("Lysstyrke_3"));

        // Parse the exhibition
        DkLightParser.parseExhibition(light, stringValue("Braendetid"));

        if (!light.isValid()) {
            // TODO: May still be e.g. SIREN
            getLog().info("Skipping invalid light " + stringValue("NR_DK") + ": " + lightChar);
            return null;
        }

        // Copy the light OSM tags to the AtoN
        light.toOsm().forEach(tag -> aton.updateTag(tag.getK(), tag.getV()));

        return aton;
    }

    /** Parses the elevation fields */
    private String parseElevation() {
        String elevation = null;
        for (int x = 1; x <= 4; x++) {
            elevation = appendValue(elevation, numericValueOrNull("Flammehoejde_" + x));
        }
        return elevation;
    }

    /** Parses the rang fields */
    private String parseRange() {
        String range = null;
        for (int x = 1; x <= 3; x++) {
            try {
                String r = stringValue("Lysstyrke_" + x)
                                .split(" ")[1]              // "W 10,5(1,5)" -> "10,5(1,5)"
                                .trim()
                                .replaceAll("\\(.*\\)", "") // "10,5(1,5)" -> "10,5"
                                .replace(',', '.');         // "10,5" -> "10.5"
                range = appendValue(range, r);
            } catch (Exception ignored) {
            }
        }
        return range;
    }



}
