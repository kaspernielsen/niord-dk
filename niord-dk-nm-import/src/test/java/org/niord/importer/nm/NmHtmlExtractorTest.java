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

            DataFilter dataFilter = DataFilter.get()
                    .fields(DataFilter.GEOMETRY, DataFilter.DETAILS, "Area.parent");

            List<MessageVo> messages = extractor.extractNms().stream()
                    .map(m -> m.toVo(MessageVo.class, dataFilter))
                    .collect(Collectors.toList());

            System.out.println("Extracted NMs:\n\n" +
                    new ObjectMapper().writerWithDefaultPrettyPrinter()
                        .writeValueAsString(messages));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
