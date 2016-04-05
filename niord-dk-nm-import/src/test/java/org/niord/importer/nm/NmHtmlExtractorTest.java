package org.niord.importer.nm;

import org.junit.Test;
import org.niord.importer.nm.extract.NmHtmlExtractor;

import java.net.URL;

/**
 * Extract NMs from HTML
 */
public class NmHtmlExtractorTest {

    @Test
    public void extractNMsFromHtml() {
        try {
            URL doc = NmHtmlExtractorTest.class.getResource("/EfS 49 2015.html");
            NmHtmlExtractor extractor = new NmHtmlExtractor(doc);

            System.out.println("Year " + extractor.getYear() +  ", week " + extractor.getWeek());

            extractor.extractNms();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
