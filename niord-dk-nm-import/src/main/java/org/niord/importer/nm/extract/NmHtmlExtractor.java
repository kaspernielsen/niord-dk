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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.niord.core.area.Area;
import org.niord.core.geojson.Feature;
import org.niord.core.message.Message;
import org.niord.core.util.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

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
    private void extractWeekAndYear() {
        try {
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
        } catch (Exception e) {
            Calendar cal = Calendar.getInstance();
            year = cal.get(Calendar.YEAR);
            week = cal.get(Calendar.WEEK_OF_YEAR);
            log.warn(String.format("Failed extracting week and year. Using current year %d week %d", year, week));
        }
    }


    public int getYear() {
        return year;
    }


    public int getWeek() {
        return week;
    }


    /** Extracts the NM messages **/
    public List<Message> extractNms() throws NmHtmlFormatException {

        // Parse the messages from the HTML
        List<Message> messages =  parseNms();

        // Merge Danish and English messages
        messages = mergeNms(messages);

        // Assign a publish date as the Friday of the published year and week
        Calendar publishDate = Calendar.getInstance();
        publishDate.set(Calendar.YEAR, year);
        publishDate.set(Calendar.WEEK_OF_YEAR, week);
        publishDate.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
        TimeUtils.resetTime(publishDate).set(Calendar.HOUR_OF_DAY, 12);
        messages.forEach(m -> {
            m.setCreated(publishDate.getTime());
            m.setPublishDate(publishDate.getTime());
        });

        return messages;
    }


    /** Merges Danish and English messages */
    private List<Message> mergeNms(List<Message> messages) {

        List<Message> result = new ArrayList<>();
        Message prevDanishMessage = null;
        for (Message message : messages) {

            // If the message number is defined, we have a Danish message
            if (message.getNumber() != null) {
                result.add(message);
                prevDanishMessage = message;

            } else {
                // An english message follows a Danish and does not have a number
                mergeNms(prevDanishMessage, message);
            }
        }
        return result;
    }


    /** Merges Danish and English messages */
    private void mergeNms(Message daMsg, Message enMsg) {
        // Sanity checks...
        if (daMsg == null || enMsg == null ||
                daMsg.getDescs().size() != 1 ||
                enMsg.getDescs().size() != 1 ||
                daMsg.getAreas().size() != enMsg.getAreas().size() ||
                daMsg.getCharts().size() != enMsg.getCharts().size()) {
            return;
        }

        // Start copying
        daMsg.getDescs().add(enMsg.getDescs().get(0));

        if (daMsg.getAreas().size() == 1) {
            Area daArea = daMsg.getAreas().get(0);
            Area enArea = enMsg.getAreas().get(0);
            daArea.getDescs().add(enArea.getDescs().get(0));
            daArea = daArea.getParent();
            enArea = enArea.getParent();
            if (daArea != null && enArea != null) {
                daArea.getDescs().add(enArea.getDescs().get(0));
            }
        }

        if (daMsg.getGeometry() != null && daMsg.getGeometry().getFeatures().size() == 1 &&
                enMsg.getGeometry() != null && enMsg.getGeometry().getFeatures().size() == 1) {
            Feature daFeature = daMsg.getGeometry().getFeatures().get(0);
            Feature enFeature = enMsg.getGeometry().getFeatures().get(0);
            daFeature.getProperties().putAll(enFeature.getProperties());
        }

    }


    /** Extracts the NM messages **/
    private List<Message> parseNms() throws NmHtmlFormatException {

        List<Message> messages = new ArrayList<>();
        Message message = null;
        String lang = "da";

        for (Element e : body.getElementsByTag("div").first().getElementsByTag("p")) {

            if (e.attr("class").startsWith("1rom")) {
                continue;
            }

            switch (e.attr("class")) {
                case "1nr":
                    message = new Message();
                    messages.add(message);

                    message.setOriginalInformation(extractOriginalInformation(e));
                    TitleLineHtmlExtractor titleLineExtractor = new TitleLineHtmlExtractor(e, message);
                    titleLineExtractor.extractTitleLineComponents();
                    lang = message.getNumber() != null ? "da" : "en";
                    break;

                case "EfS-henvisning":
                case "EfSreference0":
                case "tidlefs":
                case "FormerEfsNo":
                    if (message == null) {
                        log.warn("NtM reference field outside message");
                        break;
                    }
                    ReferenceHtmlExtractor refExtractor = new ReferenceHtmlExtractor(e, message);
                    refExtractor.extractReference();
                    break;

                case "tidspunkt":
                case "Time":
                    if (message == null) {
                        log.warn("Time field outside message");
                        break;
                    }
                    message.checkCreateDesc(lang).setTime(extractTime(e));
                    break;

                case "position":
                case "positioner":
                case "positionerfelt":
                    if (message == null) {
                        log.warn("Position field outside message");
                        break;
                    }
                    PositionHtmlExtractor posExtractor = new PositionHtmlExtractor(e, message);
                    posExtractor.extractPositions();
                    break;

                case "Publication":
                case "Publications":
                case "publikationer":
                    if (message == null) {
                        log.warn("Publication field outside message");
                        break;
                    }
                    message.checkCreateDesc(lang).setPublication(extractPublication(e));
                    break;

                case "detaljer":
                case "Details0":
                    if (message == null) {
                        log.warn("Details field outside message");
                        break;
                    }
                    message.checkCreateDesc(lang).setDescription(extractDescription(e));
                    break;

                case "Note":
                case "anm":
                    if (message == null) {
                        log.warn("Note field outside message");
                        break;
                    }
                    message.checkCreateDesc(lang).setNote(extractNote(e));
                    break;

                case "kort":
                case "Chart":
                case "Charts0":
                    if (message == null) {
                        log.warn("Chart field outside message");
                        break;
                    }
                    ChartHtmlExtractor chartExtractor = new ChartHtmlExtractor(e, message);
                    chartExtractor.extractCharts();
                    break;

                case "sag":
                case "Kilde":
                    if (message == null) {
                        log.warn("Source field outside message");
                        break;
                    }
                    message.checkCreateDesc(lang).setSource(extractSource(e));
                    message = null;
                    break;

                case "Translation":
                    message = null;
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

        return messages;
    }


    /** Extracts if the message is original information or not **/
    private boolean extractOriginalInformation(Element e) throws NmHtmlFormatException {
        Element originalInfoElement = e
                .previousElementSibling();
        return originalInfoElement != null && "1stjerne".equals(originalInfoElement.attr("class"));
    }


    /** Extracts the message publication **/
    private String extractPublication(Element e) throws NmHtmlFormatException {
        try {
            // Strip field header
            e.select("i").first().remove();
            return extractText(e);
        } catch (Exception e1) {
            return null;
        }
    }


    /** Extracts the message note **/
    private String extractNote(Element e) throws NmHtmlFormatException {
        try {
            // Strip field header
            e.select("i").first().remove();
            return extractText(e);
        } catch (Exception e1) {
            return null;
        }
    }


    /** Extracts the message details **/
    private String extractDescription(Element e) throws NmHtmlFormatException {
        try {
            // Strip field header
            e.select("i").first().remove();
            if (e.getElementsByTag("span").size() == 1){
                e = e.getElementsByTag("span").get(0);
            }
            return e.html();
        } catch (Exception e1) {
            return null;
        }
    }


    /** Extracts the message source **/
    private String extractSource(Element e) throws NmHtmlFormatException {
        String source = extractText(e);
        // Strip brackets
        return removeBrackets(source);
    }


    /** Extracts the message time **/
    private String extractTime(Element e) throws NmHtmlFormatException {
        try {
            // Strip field header
            e.select("i").first().remove();
            // TODO: Check for multi-line support
            return extractTextPreserveLineBreak(e);
        } catch (Exception e1) {
            return null;
        }
    }

}
