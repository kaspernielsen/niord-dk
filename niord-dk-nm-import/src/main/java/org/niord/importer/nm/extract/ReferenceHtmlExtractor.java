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
import org.niord.core.message.Message;
import org.niord.core.message.Reference;
import org.niord.model.message.ReferenceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    Message message;
    String lang;

    /** Constructor **/
    public ReferenceHtmlExtractor(Element e, Message message) {
        this.e = e;
        this.message = message;
        this.lang = message.getNumber() != null ? "da" : "en";
    }

    /** Extract the reference of the field **/
    public void extractReference() throws NmHtmlFormatException {

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
            ReferenceType refType;
            if (type != null && !type.trim().isEmpty()) {
                switch (removeBrackets(type)) {
                    case "gentagelse":
                    case "repetition":
                    case "gentagelse med ny tid":
                    case "repetition with new time":
                        refType = ReferenceType.REPETITION;
                        break;

                    case "ajourført":
                    case "updated": // TODO: Verify
                        refType = ReferenceType.UPDATE;
                        break;

                    case "udgår":
                    case "cancelled":
                        refType = ReferenceType.CANCELLATION;
                        break;

                    default:
                        description = type;
                        refType = ReferenceType.REFERENCE;
                }
            } else  {
                refType = ReferenceType.REFERENCE;
            }
            Reference reference = new Reference();
            reference.setType(refType);
            reference.setMessageId(String.format("NM-%03d-%02d", id, year - 2000));
            if (StringUtils.isNotBlank(description)) {
                reference.checkCreateDesc(lang).setDescription(description);
            }
            message.getReferences().add(reference);
        } else {
            log.warn("Unknown reference format " + ref);
        }
    }

}
