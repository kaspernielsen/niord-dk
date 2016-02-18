package org.niord.web.nm;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Class for extracting NM messages from HTML files.
 * <p/>
 * The HTML files are created by exporting the source Word documents as "Filtered HTML".
 * <p/>
 * Reference:
 * http://www.soefartsstyrelsen.dk/SikkerhedTilSoes/Sejladsinformation/EfS
 */
public class NmHtmlExtractor implements IHtmlExtractor {

    Logger log = LoggerFactory.getLogger(NmHtmlExtractor.class);
    Element body;
    int year, week;

    /**
     * Constructor
     *
     * @param file the HTML file
     */
    public NmHtmlExtractor(File file) throws Exception {
        this(new FileInputStream(file), file.toURI().toString());
    }

    /**
     * Constructor
     *
     * @param url the HTML URL
     */
    public NmHtmlExtractor(URL url) throws Exception {
        this(url.openStream(), url.toURI().toString());
    }

    /**
     * Constructor
     *
     * @param inputStream the HTML input stream. Will be closed in the constructor
     * @param baseUri the URI of the HTML file
     */
    public NmHtmlExtractor(InputStream inputStream, String baseUri) throws NmHtmlFormatException, IOException {

        try {
            // Parse the HTML document
            Document doc = Jsoup.parse(inputStream, "ISO-8859-1", baseUri);
            body = doc.body();

            // Extract week and year. Also serves as a litmus test for parsing the HTML format
            extractWeekAndYear();

        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Extracts the week and year from the document.
     * Throws an exception if this fails.
     * TODO: Handle accumulated NtM documents with week-ranges
     **/
    private void extractWeekAndYear() throws NmHtmlFormatException {
        Element weekElement = body.select("table tr").first().select("td p span").last();
        if (weekElement == null) {
            throw new NmHtmlFormatException("No week number found in the HTML");
        }
        week = Integer.valueOf(extractText(weekElement));

        Element publishDateElement = body.select("table tr").get(1).select("td p span").first();
        if (publishDateElement == null) {
            throw new NmHtmlFormatException("No publish date found in the HTML");
        }
        String publishDate = extractText(publishDateElement);
        publishDate = publishDate.substring(publishDate.lastIndexOf(' ')).trim();
        year = Integer.valueOf(publishDate);

        log.info(String.format("Extracted year %d week %d", year, week));
    }

    public int getYear() {
        return year;
    }

    public int getWeek() {
        return week;
    }

    /** Extracts the NM messages **/
    public void extractNms() throws NmHtmlFormatException {

        boolean inMessage = false;

        for (Element e : body.getElementsByTag("div").first().getElementsByTag("p")) {

            if (e.attr("class").startsWith("1rom")) {
                continue;
            }

            switch (e.attr("class")) {
                case "1nr":
                    inMessage = true;
                    System.out.println("********* NM *********");

                    System.out.println("Original Info: " + extractOriginalInformation(e));
                    TitleLineHtmlExtractor titleLineExtractor = new TitleLineHtmlExtractor(e);
                    titleLineExtractor.extractTitleLineComponents();
                    break;

                case "EfS-henvisning":
                case "EfSreference0":
                case "tidlefs":
                case "FormerEfsNo":
                    if (!inMessage) {
                        log.warn("NtM reference field outside message");
                        break;
                    }
                    ReferenceHtmlExtractor refExtractor = new ReferenceHtmlExtractor(e);
                    refExtractor.extractReferences();
                    break;

                case "tidspunkt":
                case "Time":
                    if (!inMessage) {
                        log.warn("Time field outside message");
                        break;
                    }
                    System.out.println("Time: " + extractTime(e));
                    break;

                case "position":
                case "positioner":
                case "positionerfelt":
                    if (!inMessage) {
                        log.warn("Position field outside message");
                        break;
                    }
                    PositionHtmlExtractor posExtractor = new PositionHtmlExtractor(e);
                    posExtractor.extractPositions();
                    break;

                case "Publication":
                case "Publications":
                case "publikationer":
                    if (!inMessage) {
                        log.warn("Publication field outside message");
                        break;
                    }
                    System.out.println("Publication: " + extractPublication(e));
                    break;

                case "detaljer":
                case "Details0":
                    if (!inMessage) {
                        log.warn("Details field outside message");
                        break;
                    }
                    System.out.println("Details: " + extractDetails(e));
                    break;

                case "Note":
                case "anm":
                    if (!inMessage) {
                        log.warn("Note field outside message");
                        break;
                    }
                    System.out.println("Note: " + extractNote(e));
                    break;

                case "kort":
                case "Chart":
                case "Charts0":
                    if (!inMessage) {
                        log.warn("Chart field outside message");
                        break;
                    }
                    ChartHtmlExtractor chartExtractor = new ChartHtmlExtractor(e);
                    chartExtractor.extractCharts();
                    break;

                case "sag":
                case "Kilde":
                    if (!inMessage) {
                        log.warn("Source field outside message");
                        break;
                    }
                    System.out.println("Source: " + extractSource(e));
                    inMessage = false;
                    break;

                case "Translation":
                    inMessage = false;
                    break;

                case "efterret":
                case "NoticestoMariners":
                case "brdtekst":
                case "MsoNormal":
                case "1stjerne":
                    log.debug("Ignored field class " + e.attr("class"));
                    break;

                default:
                    log.warn("Unrecognized field class " + e.attr("class"));
            }

        }

    }

    /** Extracts if the message is original information or not **/
    private boolean extractOriginalInformation(Element e) throws NmHtmlFormatException {
        Element originalInfoElement = e
                .previousElementSibling();
        return originalInfoElement != null && "1stjerne".equals(originalInfoElement.attr("class"));
    }

    /** Extracts the message publication **/
    private String extractPublication(Element e) throws NmHtmlFormatException {
        // Strip field header
        e.select("i").first().remove();
        return extractText(e);
    }

    /** Extracts the message note **/
    private String extractNote(Element e) throws NmHtmlFormatException {
        // Strip field header
        e.select("i").first().remove();
        return extractText(e);
    }

    /** Extracts the message details **/
    private String extractDetails(Element e) throws NmHtmlFormatException {
        // Strip field header
        e.select("i").first().remove();
        if (e.getElementsByTag("span").size() == 1){
            e = e.getElementsByTag("span").get(0);
        }
        return e.html();
    }

    /** Extracts the message source **/
    private String extractSource(Element e) throws NmHtmlFormatException {
        String source = extractText(e);
        // Strip brackets
        return removeBrackets(source);
    }

    /** Extracts the message time **/
    private String extractTime(Element e) throws NmHtmlFormatException {
        // Strip field header
        e.select("i").first().remove();
        // TODO: Check for multi-line support
        return extractTextPreserveLineBreak(e);
    }

}
