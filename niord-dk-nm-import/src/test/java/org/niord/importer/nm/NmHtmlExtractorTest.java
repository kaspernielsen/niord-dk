package org.niord.importer.nm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.niord.importer.nm.extract.NmHtmlExtractor;
import org.niord.model.DataFilter;
import org.niord.model.message.MessageVo;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

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

            List<MessageVo> messages = extractor.extractNms().stream()
                    .map(m -> m.toVo(DataFilter.get().fields(DataFilter.GEOMETRY, DataFilter.DETAILS, "Area.parent")))
                    .collect(Collectors.toList());

            System.out.println("Extracted NMs:\n\n" +
                    new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(messages));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
