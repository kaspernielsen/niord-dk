package org.niord.web.nm;

import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts references from the HTML element
 */
public class ReferenceHtmlExtractor implements IHtmlExtractor {

    private final Logger log = LoggerFactory.getLogger(ReferenceHtmlExtractor.class);
    private final Pattern refPattern = Pattern.compile("[-\\d]+/(\\d+) (\\d+),?(.*)");
    String[] prefixes = { "EfS reference.", "EfS-henvisning. ", "Tidligere EfS.", "Former EfS." };

    Element e;

    /** Constructor **/
    public ReferenceHtmlExtractor(Element e) {
        this.e = e;
    }

    /** Extract the references of the field **/
    public List<String> extractReferences() throws NmHtmlFormatException {

        // TODO handle comma-separated list of references

        String ref = removeLastPeriod(extractText(e));

        // Sometimes the header field is not wrapped in an <i>
        for (String prefix : prefixes) {
            if (ref.startsWith(prefix)) {
                ref = ref.substring(prefix.length()).trim();
            }
        }

        Matcher m = refPattern.matcher(ref);

        if (m.matches()) {
            int id = Integer.valueOf(m.group(1));
            int year = Integer.valueOf(m.group(2));
            String type = m.group(3);
            String description = null;
            if (type != null && !type.trim().isEmpty()) {
                switch (removeBrackets(type)) {
                    case "gentagelse":
                    case "repetition":
                    case "gentagelse med ny tid":
                    case "repetition with new time":
                        type = "repitition";
                        break;

                    case "ajourført":
                    case "updated": // TODO: Verify
                        type = "update";
                        break;

                    case "udgår":
                    case "cancelled":
                        type = "cancelled";
                        break;

                    default:
                        description = type;
                        type = "reference";
                }
            } else  {
                type = "reference";
            }
            System.out.println(String.format("Reference: nm-%d-%d (%s), %s", id, year - 2000, type, description));
        } else {
            log.warn("Unknown reference format " + ref);
        }

        return null;
    }

}
