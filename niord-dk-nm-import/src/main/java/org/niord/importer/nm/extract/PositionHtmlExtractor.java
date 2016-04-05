package org.niord.importer.nm.extract;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts positions from the HTML element
 */
public class PositionHtmlExtractor implements IHtmlExtractor {

    private final NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
    private final Pattern posPattern = Pattern.compile("(\\d+)\\s+(\\d+,?\\d+)\\s?(N|S)\\s+(\\d+)\\s+(\\d+,?\\d+)\\s?(E|W),?(.*)");
    private final Logger log = LoggerFactory.getLogger(PositionHtmlExtractor.class);

    Element e;

    /** Constructor **/
    public PositionHtmlExtractor(Element e) {
        this.e = e;
    }

    public List<String> extractPositions() throws NmHtmlFormatException {

        // Clean up positions
        e.select("span[style~=.*SpecialD.*]").forEach(e -> e.text(" "));
        e.select("span[class~=minut|grad]").forEach(e -> e.text(" "));

        String posStr = extractTextPreserveLineBreak(e);

        // Strip "Position." field name
        posStr = posStr.replaceFirst("Position. ", "");

        // Strip number indices used when there are multiple positions
        posStr = posStr.replaceFirst("^\\d+\\) ", "").replaceAll("(?is)\n\\d+\\) ", "\n").trim();


        System.out.println("Positions:");

        // Parse individual positions
        for (String pos : posStr.split("\n")) {

            // Strip trailing "."
            if (pos.endsWith(".")) {
                pos = pos.substring(0, pos.length() - 1);
            }

            Matcher m = posPattern.matcher(pos);
            if (m.matches()) {
                try {
                    int i = 1;
                    double lat = parsePos(
                            Integer.parseInt(m.group(i++)),
                            format.parse(m.group(i++)).doubleValue(),
                            m.group(i++)
                    );
                    double lon = parsePos(
                            Integer.parseInt(m.group(i++)),
                            format.parse(m.group(i++)).doubleValue(),
                            m.group(i++)
                    );
                    String desc = m.group(i).trim();

                    System.out.println(String.format("  lat=%2.2f, lon=%3.2f, desc=%s", lat, lon, desc));
                } catch (ParseException e1) {
                    log.error("Error parsing position " + pos + ": " + e);
                }

            } else {
                log.warn("Error matching position pattern: " + pos);
            }

        }

        return null;
    }

    public static double parsePos(int h, double m, String pos) {
        return (h + m / 60.0) * (pos.equalsIgnoreCase("S") || pos.equalsIgnoreCase("W") ? -1.0 : 1.0);
    }

}
