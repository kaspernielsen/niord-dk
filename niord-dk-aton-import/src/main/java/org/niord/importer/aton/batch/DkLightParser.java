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
import org.niord.importer.aton.batch.LightSeamark.Colour;
import org.niord.importer.aton.batch.LightSeamark.LightSector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                    "(?<colors>(" + LIGHT_COLORS + ")([\\. ]?(" + LIGHT_COLORS + "))*)?[\\. ]?" +
                    "(?<period>\\d+(,\\d)?[sm])?" +
                    ".*$"
    );

    public static Pattern LIGHT_FORMAT = Pattern.compile(
            "[\\. ]?(?<color>" + LIGHT_COLORS + ")"
    );

    public static Pattern PHASE_FORMAT = Pattern.compile(
            "[\\. +]?(?<phase>" + LIGHT_PHASES + ")"
    );

    public static Pattern RANGE_FORMAT = Pattern.compile(
            "(?<color>" + LIGHT_COLORS + ") (?<range>\\d+(,\\d)?)"
    );

    public static Pattern SECTOR_FORMAT = Pattern.compile(
            "[\\. ]?(?<color>" + LIGHT_COLORS + ")" +
                    "(?<start>\\d+(,\\d+)?)°-" +
                    "(?<end>\\d+(,\\d+)?)°"
    );

    /**
     * No public initialization
     */
    private DkLightParser() {
    }

    /**
     * Creates and initializes a new instance
     *
     * @return the newly created light instance
     */
    public static LightSeamark newInstance() {
        LightSeamark light = new LightSeamark();
        LightSector sector = new LightSector();
        light.getSectors().add(sector);
        return light;
    }

    /**
     * Parses the light characteristics and updates the first sector of the LightSeamark
     *
     * @param light     the light to update
     * @param lightChar the light characteristics
     * @return the updated light
     */
    public static LightSeamark parseLightCharacteristics(LightSeamark light, String lightChar) {

        Matcher m = LIGHT_CHARACTER_FORMAT.matcher(lightChar);

        if (m.find()) {
            String multipleSpec = m.group("multiple");
            String phaseSpec = m.group("phase");
            String groupSpec = m.group("group");
            String colorsSpec = m.group("colors");
            String periodSpec = m.group("period");

            if (StringUtils.isBlank(phaseSpec) || (!"Mo".equals(phaseSpec) && StringUtils.isBlank(colorsSpec))) {
                return light;
            }

            LightSector sector = light.getSectors().get(0);

            // Phases
            Matcher pm = PHASE_FORMAT.matcher(phaseSpec);
            List<LightSeamark.Character> phases = new ArrayList<>();
            while (pm.find()) {
                phases.add(valueOfLc(pm.group("phase")));
            }
            if (phases.isEmpty() || phases.size() > 2) {
                return light;
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
                    sector.getColours().add(Colour.valueOfLc(cm.group("color")));
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
                sector.setPeriod(Double.valueOf(periodSpec.substring(0, periodSpec.length() - 1).replace(',', '.')));
            }

        }
        return light;
    }


    /**
     * When the light character is read in, there may be multiple colours, e.g. "Iso.WRG.8s".
     * This covers the fact that the light may be a multi-sectored or -directional light.
     * This method will take a multi-light sector and convert it into a single-light
     * multi-sectored version.
     *
     * This function is not used yet. Consider using it if not light sector angles are defined.
     *
     * @param light the light to update
     */
    @SuppressWarnings("unused")
    private static LightSeamark expandColours(LightSeamark light) {
        if (light.getSectors().size() == 1 && light.getSectors().get(0).getColours().size() > 1) {

            LightSector sector = light.getSectors().get(0);
            List<LightSector> sectors = new ArrayList<>();
            sector.getColours().forEach(col -> {
                LightSector s = sector.copy();
                s.getColours().clear();
                s.getColours().add(col);
                sectors.add(s);
            });

            light.setSectors(sectors);
        }
        return light;
    }


    /**
     * Parses the height and updates the light with the value
     * @param light the light to update
     * @param height the height
     * @return the updated light
     */
    public static LightSeamark parseHeight(LightSeamark light, Double height) {
        light.getSectors().forEach(s -> s.setHeight(height));
        return light;
    }


    /**
     * Parses the light sector angles. The field has the format
     * "G020,9°-025,5° W025,5°-030° R030°-19,2° G119,2°-207,5° W207,5°-209,8° R209,8°-220,8°. "
     *
     * @param light the light to update
     * @param sectors the light sector angles
     * @return the updated light
     */
    public static LightSeamark parseLightSectorAngles(LightSeamark light, String sectors) {
        if (StringUtils.isBlank(sectors) || light.getSectors().size() != 1) {
            return light;
        }

        List<LightSector> newSectors = new ArrayList<>();
        LightSector sector = light.getSectors().get(0);

        Matcher m = SECTOR_FORMAT.matcher(sectors);
        while (m.find()) {
            try {
                Colour col = Colour.valueOfLc(m.group("color"));
                Double start = Double.valueOf(m.group("start").replace(',', '.'));
                Double end = Double.valueOf(m.group("end").replace(',', '.'));

                LightSector newSector = sector.copy();
                newSector.getColours().clear();
                newSector.getColours().add(col);
                newSector.setSectorStart(start);
                newSector.setSectorEnd(end);
                newSectors.add(newSector);

            } catch (Exception ignored) {
            }
        }

        if (newSectors.size() > 0) {
            light.setSectors(newSectors);
        }

        return light;
    }


    /**
     * Parses the ranges and updates the light with the values.
     * There should be 1-3 ranges, which should match the colours of the light sectors
     *
     * @param light the light to update
     * @param ranges the ranges
     * @return the updated light
     */
    public static LightSeamark parseRange(LightSeamark light, String... ranges) {

        // Build a map of the color-range values
        Map<Colour, Double> rangeMap = new HashMap<>();
        for (String range : ranges) {
            if (StringUtils.isNotBlank(range)) {
                Matcher m = RANGE_FORMAT.matcher(range);
                try {
                    if (m.matches()) {
                        rangeMap.put(
                                Colour.valueOfLc(m.group("color")),
                                Double.valueOf(m.group("range").replace(',', '.'))
                        );
                    }
                } catch (Exception ignored) {
                }
            }
        }

        // Update the corresponding light sectors
        light.getSectors().forEach(s -> {
            Colour col = s.getColours().size() == 1 ? s.getColours().get(0) : null;
            if (col != null && rangeMap.containsKey(col)) {
                s.setRange(rangeMap.get(col));
            }
        });

        return light;
    }


    /**
     * Parses the exhibition and updates the light with the values.
     *
     * @param light the light to update
     * @param exhibition the exhibition
     * @return the updated light
     */
    public static LightSeamark parseExhibition(LightSeamark light, String exhibition) {

        if (StringUtils.isNotBlank(exhibition) && exhibition.toLowerCase().contains("brændetid: H24")) {
            light.setExhibition(LightSeamark.Exhibition.h24);
        }

        return light;
    }


}
