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

package org.niord.importer.nm.extract;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;
import org.niord.core.chart.Chart;
import org.niord.core.message.Message;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts charts from the HTML element
 */
public class ChartHtmlExtractor implements IHtmlExtractor {

    Pattern p1 = Pattern.compile("(\\d+)");
    Pattern p2 = Pattern.compile("(\\d+) \\(INT (\\d+)\\)");

    Element e;
    Message message;

    /** Constructor **/
    public ChartHtmlExtractor(Element e, Message message) {
        this.e = e;
        this.message = message;
    }

    public List<String> extractCharts() throws NmHtmlFormatException {
        String charts;
        try {
            // Strip field header
            e.select("i").first().remove();
            charts = removeLastPeriod(extractText(e));
        } catch (Exception e1) {
            return null;
        }

        for (String chart : charts.split(",")) {
            Matcher m1 = p1.matcher(chart.trim());
            Matcher m2 = p2.matcher(chart.trim());
            String chartNumber;
            String internationalChartNumber = null;
            if (m1.matches()) {
                chartNumber = m1.group(1);
            } else if (m2.matches()) {
                chartNumber = m2.group(1);
                internationalChartNumber = m2.group(2);
            } else {
                continue;
            }

            Chart c = new Chart(
                    chartNumber,
                    StringUtils.isBlank(internationalChartNumber) ? null : Integer.valueOf(internationalChartNumber));
            message.getCharts().add(c);
        }

        return null;
    }

}
