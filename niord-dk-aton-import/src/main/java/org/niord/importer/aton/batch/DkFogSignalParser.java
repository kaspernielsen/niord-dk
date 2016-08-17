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
