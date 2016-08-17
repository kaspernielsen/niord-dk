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
import org.jsoup.nodes.Element;

/**
 * Interface implemented by HTML extractors.
 */
public interface IHtmlExtractor {

    String NON_BREAKING_SPACE = "\u00A0";

    /** Extracts the text from the HTML element and compacts the whitespace **/
    default String extractText(Element e) {
        return e.text()
                .replaceAll(NON_BREAKING_SPACE, "")
                .replace("\t"," ")
                .trim();
    }

    /** Extracts the text from the HTML element and compacts the whitespace, but preserves newlines **/
    default String extractTextPreserveLineBreak(Element e) {
        return Jsoup.parse(e.html().replaceAll("(?i)<br[^>]*>", "<pre>\n</pre>")).text()
                .replaceAll(NON_BREAKING_SPACE, "")
                .replaceAll("^\\s+|\\s+$|\\s*(\n)\\s*|(\\s)\\s*", "$1$2")
                .replace("\t"," ");
    }

    /**
     * Removes any trailing period from the line
     * @param line the line
     * @return the line excluding any trailing period
     */
    default String removeLastPeriod(String line) {
        line = line.trim();
        if (line.endsWith(".")) {
            line = line.substring(0, line.length() - 1);
        }
        return line;
    }

    /**
     * Removes any surrounding brackets
     * @param line the line to remove brackes from
     * @return the updated line
     */
    default String removeBrackets(String line) {
        if (line != null) {
            line = line.trim();
            if (line.startsWith("(") && line.endsWith(")")) {
                line = line.substring(1, line.length() - 1);
            }
        }
        return line;
    }
}
