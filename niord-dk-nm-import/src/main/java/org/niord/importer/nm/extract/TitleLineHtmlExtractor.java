package org.niord.importer.nm.extract;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Extracts the components of the title line from the HTML element
 * <p/>
 * General format of the title line:
 * [(T)/(P)]. Area-lineage. Vicinity. Title.
 */
public class TitleLineHtmlExtractor implements IHtmlExtractor {

    private final Logger log = LoggerFactory.getLogger(TitleLineHtmlExtractor.class);

    Element e;

    /** Constructor **/
    public TitleLineHtmlExtractor(Element e) {
        this.e = e;
    }

    public List<String> extractTitleLineComponents() throws NmHtmlFormatException {

        System.out.println("ID: " + extractIdentifier(e));

        String titleLine = extractText(e);
        String[] parts = titleLine.split("\\.");
        for (int x = 0; x < parts.length; x++) {
            parts[x] = parts[x].trim();
        }

        // Extract the NM sub-type
        int i = 0;
        String type = "permanent"; // TODO: Verify
        if (parts[i].matches("\\(T\\)|\\(P\\)")) {
            type = parts[i++].contains("T") ? "temp" : "prelim";
        }
        System.out.println("NM Type: " + type);

        int areaParts = (parts.length  - i > 2) ? 2 : 1;
        int vicinityNo = (parts.length  - i > 4) ? 1 : 0;

        String area = null;
        while (areaParts > 0) {
            area = area == null ? parts[i] : area + " - " + parts[i];
            areaParts--;
            i++;
        }
        System.out.println("Area: " + area);

        String vicinity = null;
        while (vicinityNo > 0) {
            vicinity = vicinity == null ? parts[i] : vicinity + ", " + parts[i];
            vicinityNo--;
            i++;
        }
        if (vicinity != null) {
            System.out.println("Vicinity: " + vicinity);
        }

        String title = "";
        while (i < parts.length) {
            title = title.length() == 0 ? parts[i] : title + " " + parts[i];
            title += ".";
            i++;
        }
        System.out.println("Title: " + title);

        return null;
    }

    /** Extracts the series identifier of the message **/
    private Integer extractIdentifier(Element e) throws NmHtmlFormatException {
        Element idElement = e
                .getElementsByTag("span")
                .first();
        if (idElement == null) {
            // Translations do not have a series identifier
            return null;
        }

        // For some translated messages, the <span> is present but empty
        String id = extractText(idElement)
                .replace(".", ""); // Strip any "." after number


        if (id.length() > 0 && StringUtils.isNumeric(id)) {
            // Remove the id element
            idElement.remove();
            return Integer.valueOf(id);
        }
        return null;
    }

}
