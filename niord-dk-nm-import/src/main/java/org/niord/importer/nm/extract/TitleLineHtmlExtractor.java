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
import org.niord.core.area.Area;
import org.niord.core.message.Message;
import org.niord.model.message.Type;

/**
 * Extracts the components of the title line from the HTML element
 * <p/>
 * General format of the title line:
 * [(T)/(P)]. Area-lineage. Vicinity. Title.
 */
public class TitleLineHtmlExtractor implements IHtmlExtractor {

    Element e;
    Message message;
    String lang;

    /** Constructor **/
    public TitleLineHtmlExtractor(Element e, Message message) {
        this.e = e;
        this.message = message;
    }

    /** Extracts the different parts of the title line */
    public void extractTitleLineComponents() throws NmHtmlFormatException {

        message.setNumber(extractIdentifier(e));

        lang = message.getNumber() != null ? "da" : "en";

        String titleLine = extractText(e);
        String[] parts = titleLine.split("\\.");
        for (int x = 0; x < parts.length; x++) {
            parts[x] = parts[x].trim();
        }

        // Extract the NM sub-type
        int i = 0;
        Type type = Type.PERMANENT_NOTICE;
        if (parts[i].matches("\\(T\\)|\\(P\\)")) {
            type = parts[i++].contains("T") ? Type.TEMPORARY_NOTICE : Type.PRELIMINARY_NOTICE;
        }
        message.setType(type);

        StringBuilder title = new StringBuilder();
        for (int x = i; x < parts.length; x++) {
            title.append(parts[x]).append(". ");
        }
        message.checkCreateDesc(lang).setTitle(title.toString().trim());

        int areaParts = 1;
        int vicinityNo = (parts.length  - i > 4) ? 1 : 0;

        Area area = null;
        while (areaParts > 0) {
            Area childArea = new Area();
            childArea.createDesc(lang).setName(parts[i]);
            if (area != null) {
                area.addChild(childArea);
            }
            area = childArea;
            areaParts--;
            i++;
        }
        message.getAreas().add(area);

        String vicinity = null;
        while (vicinityNo > 0) {
            vicinity = vicinity == null ? parts[i] : vicinity + ", " + parts[i];
            vicinityNo--;
            i++;
        }
        if (vicinity != null) {
            message.checkCreateDesc(lang).setVicinity(vicinity);
        }

        String subject = "";
        while (i < parts.length) {
            subject = subject.length() == 0 ? parts[i] : subject + " " + parts[i];
            subject += ".";
            i++;
        }
        message.checkCreateDesc(lang).setSubject(subject);
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
