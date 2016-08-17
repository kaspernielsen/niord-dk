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
import org.niord.core.geojson.Feature;
import org.niord.core.geojson.FeatureCollection;
import org.niord.core.geojson.JtsConverter;
import org.niord.core.message.Message;
import org.niord.model.geojson.MultiPointVo;
import org.niord.model.geojson.PointVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts positions from the HTML element
 */
public class PositionHtmlExtractor implements IHtmlExtractor {

    private final NumberFormat format = NumberFormat.getInstance(Locale.FRANCE);
    private final Pattern posPattern = Pattern.compile("(\\d+)\\s+(\\d+,?\\d+)\\s?(N|S)\\s+(\\d+)\\s+(\\d+,?\\d+)\\s?(E|W),?(.*)");
    private final Logger log = LoggerFactory.getLogger(PositionHtmlExtractor.class);

    Element e;
    Message message;
    String lang;

    /** Constructor **/
    public PositionHtmlExtractor(Element e, Message message) {
        this.e = e;
        this.message = message;
        this.lang = message.getNumber() != null ? "da" : "en";
    }


    /** Extracts the positions */
    public void extractPositions() throws NmHtmlFormatException {

        // Clean up positions
        e.select("span[style~=.*SpecialD.*]").forEach(e -> e.text(" "));
        e.select("span[class~=minut|grad]").forEach(e -> e.text(" "));

        String posStr = extractTextPreserveLineBreak(e);

        // Strip "Position." field name
        posStr = posStr.replaceFirst("Position. ", "");

        // Strip number indices used when there are multiple positions
        posStr = posStr.replaceFirst("^\\d+\\) ", "").replaceAll("(?is)\n\\d+\\) ", "\n").trim();

        List<Pos> positions = new ArrayList<>();

        // Parse individual positions
        for (String pos : posStr.split("\n")) {

            // Strip trailing "."
            if (pos.endsWith(".")) {
                pos = pos.substring(0, pos.length() - 1);
            }

            Matcher m = posPattern.matcher(pos);
            if (m.matches()) {
                try {
                    int i = 1;
                    double lat = parsePos(
                            Integer.parseInt(m.group(i++)),
                            format.parse(m.group(i++)).doubleValue(),
                            m.group(i++)
                    );
                    double lon = parsePos(
                            Integer.parseInt(m.group(i++)),
                            format.parse(m.group(i++)).doubleValue(),
                            m.group(i++)
                    );
                    String desc = m.group(i).trim();

                    positions.add(new Pos(lat, lon, desc));
                } catch (ParseException e1) {
                    log.error("Error parsing position " + pos + ": " + e);
                }

            } else {
                log.warn("Error matching position pattern: " + pos);
            }

        }

        if (!positions.isEmpty()) {
            FeatureCollection geometry = new FeatureCollection();
            message.setGeometry(geometry);
            Feature feature = new Feature();
            geometry.getFeatures().add(feature);
            if (positions.size() == 1) {
                PointVo p = new PointVo();
                double[] coordinates = new double[2];
                Pos pos = positions.get(0);
                coordinates[0] = pos.getLon();
                coordinates[1] = pos.getLat();
                if (StringUtils.isNotBlank(pos.getDesc())) {
                    feature.getProperties().put("name:0:" + lang, pos.getDesc());
                }
                p.setCoordinates(coordinates);
                feature.setGeometry(JtsConverter.toJts(p));
            } else {
                MultiPointVo mp = new MultiPointVo();
                double[][] coordinates = new double[positions.size()][2];
                for (int p = 0; p < positions.size(); p++) {
                    Pos pos = positions.get(p);
                    coordinates[p][0] = pos.getLon();
                    coordinates[p][1] = pos.getLat();
                    if (StringUtils.isNotBlank(pos.getDesc())) {
                        feature.getProperties().put("name:" + p + ":" + lang, pos.getDesc());
                    }
                }
                mp.setCoordinates(coordinates);
                feature.setGeometry(JtsConverter.toJts(mp));
            }
        }
    }


    public static double parsePos(int h, double m, String pos) {
        return (h + m / 60.0) * (pos.equalsIgnoreCase("S") || pos.equalsIgnoreCase("W") ? -1.0 : 1.0);
    }


    /** Used to encapsulate a position with a description */
    public static class Pos {
        double lat;
        double lon;
        String desc;

        public Pos(double lat, double lon, String desc) {
            this.lat = lat;
            this.lon = lon;
            this.desc = desc;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public String getDesc() {
            return desc;
        }
    }
}
