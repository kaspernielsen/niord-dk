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
import org.niord.importer.aton.batch.FogSignalSeamark.Category;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Parses the fog signal specification of the DK AtoN light list
 */
public class DkFogSignalParser {

    public static String FOG_CATEGORIES = Arrays.stream(Category.values())
            .map(Enum::toString)
            .collect(Collectors.joining("|"));

    public static Pattern FOG_SIGNAL_FORMAT = Pattern.compile(
            "^" +
            "(?<category>" + FOG_CATEGORIES + ")[\\. ]*" +
            "(?<morse>Mo\\([a-z]+\\))?[\\. ]*" +
            "(?<group>\\(\\d+\\))?[\\. ]*" +
            "(?<period>\\d+(,\\d)?[sm])?" +
            ".*$",
            Pattern.CASE_INSENSITIVE
    );

    public static Pattern MORSE_FORMAT = Pattern.compile(
            "Mo\\((?<morse>[a-z]+)\\)",
            Pattern.CASE_INSENSITIVE
    );

    public static Pattern GROUP_FORMAT = Pattern.compile(
            "\\((?<group>\\d+)\\)" +
            Pattern.CASE_INSENSITIVE
    );

    /**
     * No public initialization
     */
    private DkFogSignalParser() {
    }

    /**
     * Creates and initializes a new instance
     *
     * @return the newly created fog signal instance
     */
    public static FogSignalSeamark newInstance() {
        return new FogSignalSeamark();
    }

    /**
     * Parses the fog signal fog signal
     *
     * @param fogSignal     the fogSignal to update
     * @param fogSignalSpec the fogSignal characteristics
     * @return the updated fogSignal
     */
    public static FogSignalSeamark parseFogSignal(FogSignalSeamark fogSignal, String fogSignalSpec) {

        Matcher m = FOG_SIGNAL_FORMAT.matcher(fogSignalSpec);

        if (m.find()) {
            String categorySpec = m.group("category");
            String morseSpec = m.group("morse");
            String groupSpec = m.group("group");
            String periodSpec = m.group("period");

            // Category
            if (StringUtils.isBlank(categorySpec)) {
                return fogSignal;
            }
            fogSignal.setCategory(Category.valueOf(categorySpec.toLowerCase()));

            // Morse group
            if (StringUtils.isNotBlank(morseSpec)) {
                Matcher mm = MORSE_FORMAT.matcher(morseSpec);
                if (mm.find()) {
                    fogSignal.setGroup(mm.group("morse"));
                }
            }

            // Group
            if (StringUtils.isNotBlank(groupSpec)) {
                fogSignal.setGroup(groupSpec.substring(1, groupSpec.length() - 1).trim());
            }

            // Period
            if (StringUtils.isNotBlank(periodSpec)) {
                fogSignal.setPeriod(Double.valueOf(periodSpec.substring(0, periodSpec.length() - 1).replace(',', '.')));
            }
        }
        return fogSignal;
    }

}
