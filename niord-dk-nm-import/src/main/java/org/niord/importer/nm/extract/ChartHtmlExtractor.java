package org.niord.importer.nm.extract;

import org.jsoup.nodes.Element;

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

    /** Constructor **/
    public ChartHtmlExtractor(Element e) {
        this.e = e;
    }

    public List<String> extractCharts() throws NmHtmlFormatException {

        // Strip field header
        e.select("i").first().remove();
        String charts = removeLastPeriod(extractText(e));


        for (String chart : charts.split(",")) {
            Matcher m1 = p1.matcher(chart.trim());
            Matcher m2 = p2.matcher(chart.trim());
            String chartNumber = null;
            String internationalChartNumber = null;
            if (m1.matches()) {
                chartNumber = m1.group(1);
            } else if (m2.matches()) {
                chartNumber = m2.group(1);
                internationalChartNumber = m2.group(2);
            }
            System.out.println(String.format("Chart: %s (INT %s)", chartNumber, internationalChartNumber));
        }

        return null;
    }

}
