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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.niord.importer.aton.batch.LightSeamark.Character.*;

/**
 * Parses the light specs of the DK AtoN light list
 */
public class DkLightParser {

    public static String LIGHT_PHASES = "FFl|LFl|Fl|F|IVQ|VQ|IQ|IUQ|UQ|Q|Iso|Oc|Al|Mo|Gr";

    public static String LIGHT_COLORS = "W|R|G|Bu|Y|Am";

    public static Pattern LIGHT_CHARACTER_FORMAT = Pattern.compile(
            "^" +
            "(?<multiple>\\d+[ ]+)?" +
            "(?<phase>(" + LIGHT_PHASES + ")([\\. +]?(" + LIGHT_PHASES + "))*)[\\. ]?" +
            "(?<group>\\(\\w+(\\+\\w+)*\\))?[\\. ]?" +
            "(?<colors>(" +  LIGHT_COLORS + ")([\\. ]?(" + LIGHT_COLORS + "))*)?[\\. ]?" +
            "(?<period>\\d+(,\\d)?[sm])?" +
            ".*$"
    );

    public static Pattern LIGHT_FORMAT = Pattern.compile(
            "[\\. ]?(?<color>" + LIGHT_COLORS + ")"
    );

    public static Pattern PHASE_FORMAT = Pattern.compile(
            "[\\. +]?(?<phase>" + LIGHT_PHASES + ")"
    );

    public static void main(String[] args) {
        Matcher cm = LIGHT_CHARACTER_FORMAT.matcher("F.R");
        while (cm.find()) {
            System.out.println(cm.group("phase"));
        }

    }

    /**
     * Parses the light characteristics into a LightSeamark
     * @param lightChar the light characteristics
     * @return the parsed LightSeamark
     */
    public static LightSeamark parseLightCharacteristics(String lightChar) {

        LightSeamark light = null;

        Matcher m = LIGHT_CHARACTER_FORMAT.matcher(lightChar);

        if (m.find()) {
            String multipleSpec = m.group("multiple");
            String phaseSpec = m.group("phase");
            String groupSpec = m.group("group");
            String colorsSpec = m.group("colors");
            String periodSpec = m.group("period");

            if (StringUtils.isBlank(phaseSpec) || (!"Mo".equals(phaseSpec) && StringUtils.isBlank(colorsSpec))) {
                return null;
            }

            light = new LightSeamark();
            LightSeamark.LightSector sector = new LightSeamark.LightSector();
            light.getSectors().add(sector);

            // Phases
            Matcher pm = PHASE_FORMAT.matcher(phaseSpec);
            List<LightSeamark.Character> phases = new ArrayList<>();
            while (pm.find()) {
                phases.add(valueOfLc(pm.group("phase")));
            }
            if (phases.isEmpty() || phases.size() > 2) {
                return null;
            } else if (phases.size() == 1) {
                sector.setCharacter(phases.get(0));
            } else {
                LightSeamark.Character p1 = phases.get(0);
                LightSeamark.Character p2 = phases.get(1);

                // Known OSM composite cases
                if (p1 == Fl && p2 == LFl) {
                    sector.setCharacter(FlLFl);
                } else if (p1 == Oc && p2 == Fl) {
                    sector.setCharacter(OcFl);
                } else if (p1 == F && p2 == LFl) {
                    sector.setCharacter(FLFl);
                } else if (p1 == Al && p2 == Oc) {
                    sector.setCharacter(Al_Oc);
                } else if (p1 == Al && p2 == LFl) {
                    sector.setCharacter(Al_LFl);
                } else if (p1 == Al && p2 == Fl) {
                    sector.setCharacter(Al_Fl);
                } else if (p1 == Al && p2 == Gr) {
                    sector.setCharacter(Al_Gr);
                } else if (p1 == Q && p2 == LFl) {
                    sector.setCharacter(Q_LFl);
                } else if (p1 == VQ && p2 == LFl) {
                    sector.setCharacter(VQ_LFl);
                } else if (p1 == UQ && p2 == LFl) {
                    sector.setCharacter(UQ_LFl);
                } else if (p1 == Al && p2 == FFl) {
                    sector.setCharacter(Al_FFl);
                }
            }

            // Colors
            if (StringUtils.isNotBlank(colorsSpec)) {
                Matcher cm = LIGHT_FORMAT.matcher(colorsSpec);
                while (cm.find()) {
                    sector.getColours().add(LightSeamark.Colour.valueOfLc(cm.group("color")));
                }
            }
            // Multiple
            if (StringUtils.isNotBlank(multipleSpec)) {
                sector.setMultiple(Integer.valueOf(multipleSpec.trim()));
            }

            // Group
            if (StringUtils.isNotBlank(groupSpec)) {
                sector.setGroup(groupSpec.substring(1, groupSpec.length() - 1).trim());
            }

            // Period
            if (StringUtils.isNotBlank(periodSpec)) {
                sector.setPeriod(Double.valueOf(periodSpec.substring(0, periodSpec.length()-1).replace(',', '.')));
            }

        }
        return light;
    }
}
