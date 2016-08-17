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
package org.niord.importer.aton.batch;

import org.apache.commons.lang.StringUtils;
import org.niord.core.aton.AtonTag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for representing an OSM light character model
 *
 * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Lights">OSM Light Documentation</a>
 */
@SuppressWarnings("unused")
public class LightSeamark {

    /*************************/
    /** Enums               **/
    /*************************/

    /** Light types **/
    public enum Type {
        light, light_major, light_minor, light_vessel, light_float
   }

    /** Light colors values **/
    public enum Colour {
        white("white", "W"),
        red("red", "R"),
        green("green", "G"),
        blue("blue", "Bu"),
        yellow("yellow", "Y"),
        amber("amber", "Am");

        String osm, lc;

        Colour(String osm, String lc) {
            this.osm = osm;
            this.lc = lc;
        }

        @Override
        public String toString() {
            return osm;
        }

        public String toLc() {
            return lc;
        }

        public static Colour valueOfLc(String lc) {
            return Arrays.stream(values())
                    .filter(v -> v.toLc().equals(lc))
                    .findFirst()
                    .orElse(null);
        }
    }

    /** Light character values **/
    enum Character {
        F("F", "F"),
        Fl("Fl", "Fl"),
        LFl("LFl", "LFl"),
        Q("Q", "Q"),
        VQ("VQ", "VQ"),
        UQ("UQ", "UQ"),
        Iso("Iso", "Iso"),
        Oc("Oc", "Oc"),
        IQ("IQ", "IQ"),
        IVQ("IVQ", "IVQ"),
        IUQ("IUQ", "IUQ"),
        Mo("Mo", "Mo"),
        FFl("FFl", "FFl"),
        FlLFl("FlLFl", "FlLFl"),
        OcFl("OcFl", "OcFl"),
        FLFl("FLFl", "FLFl"),
        Al_Oc("Al.Oc", "Al.Oc"),
        Al_LFl("Al.LFl", "Al.LFl"),
        Al_Fl("Al.Fl", "Al.Fl"),
        Al_Gr("Al.Gr", "Al.Gr"),
        Q_LFl("Q+LFl", "Q+LFl"),
        VQ_LFl("VQ+LFl", "VQ+LFl"),
        UQ_LFl("UQ+LFl", "UQ+LFl"),
        Al("Al", "Al"),
        Al_FFl("Al.FFl", "Al.FFl"),
        Gr("Gr", "Gr"); // Validate Gr?

        String osm, lc;

        Character(String osm, String lc) {
            this.osm = osm;
            this.lc = lc;
        }

        @Override
        public String toString() {
            return osm;
        }

        public String toLc() {
            return lc;
        }

        public static Character valueOfLc(String lc) {
            return Arrays.stream(values())
                    .filter(v -> v.toLc().equals(lc))
                    .findFirst()
                    .orElse(null);
        }

    }

    /** Light category values **/
    enum Category {
        directional,
        leading,
        aero,
        air_obstruction,
        fog_detector,
        floodlight,
        strip_light,
        subsidiary,
        spotlight,
        front,
        rear,
        upper,
        lower,
        moire,
        emergency,
        bearing,
        horizontal,
        vertical
    }

    /** Light exibition values */
    enum Exhibition {
        h24("24h"),
        day("day"),
        fog("fog"),
        night("night"),
        warning("warning"),
        storm("storm");

        String osm;

        Exhibition(String osm) {
            this.osm = osm;
        }

        @Override
        public String toString() {
            return osm;
        }
    }

    /*************************/
    /** Variables           **/
    /*************************/

    Type type = Type.light;
    Category category;
    Exhibition exhibition;
    List<LightSector> sectors = new ArrayList<>();

    /*************************/
    /** Functions           **/
    /*************************/

    /** Checks if the light is valid */
    public boolean isValid() {
        return sectors.size() > 0 &&
                sectors.stream().allMatch(LightSector::isValid);
    }

    /** Converts this entity to a list of AtoN tags **/
    public List<AtonTag> toOsm() {

        List<AtonTag> tags = new ArrayList<>();

        addAtonTag(tags, "seamark:type", type);
        addAtonTag(tags, "seamark:light:category", category);
        addAtonTag(tags, "seamark:light:exhibition", exhibition);

        for (int x = 0; x < sectors.size(); x++) {
            String prefix = "seamark:light:";
            if (sectors.size() > 1) {
                prefix += String.valueOf(x + 1) + ":";
            }
            sectors.get(x).toOsm(tags, prefix);
        }

        return tags;
    }

    /** Adds the tag if the value is well-defined */
    public static void addAtonTag(List<AtonTag> tags, String key, Object val) {
        String str = val != null ? val.toString() : null;
        if (StringUtils.isNotBlank(str)) {
            tags.add(new AtonTag(key, str));
        }
    }

    @Override
    public String toString() {
        return toOsm().toString();
    }

    /*************************/
    /** Getters and Setters **/
    /*************************/

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Exhibition getExhibition() {
        return exhibition;
    }

    public void setExhibition(Exhibition exhibition) {
        this.exhibition = exhibition;
    }

    public List<LightSector> getSectors() {
        return sectors;
    }

    public void setSectors(List<LightSector> sectors) {
        this.sectors = sectors;
    }

    /*************************/
    /** Helper classes      **/
    /*************************/

    /**
     * Represents a light sector
     */
    public static class LightSector {

        // seamark:light:colour
        List<Colour> colours = new ArrayList<>();

        // seamark:light:character
        Character character;

        // seamark:light:<attribute>
        Double height;
        Integer multiple;
        Double range;
        String group;
        Double period;
        String sequence;
        Double sectorStart;
        Double sectorEnd;

        /** Checks if the light sector is valid */
        public boolean isValid() {
            return character != null &&
                    (colours.size() > 0 ||
                            (character == Character.Mo && group != null));
        }

        /** Creates a copy of this sector **/
        public LightSector copy() {
            LightSector s = new LightSector();
            s.setColours(new ArrayList<>(colours));
            s.setCharacter(character);
            s.setHeight(height);
            s.setMultiple(multiple);
            s.setRange(range);
            s.setGroup(group);
            s.setPeriod(period);
            s.setSequence(sequence);
            s.setSectorStart(sectorStart);
            s.setSectorEnd(sectorEnd);
            return s;
        }

        /** Converts this entity to a list of AtoN tags **/
        public void toOsm(List<AtonTag> tags, String prefix) {

            String colour = colours.stream()
                    .map(Colour::toString)
                    .collect(Collectors.joining(";"));
            addAtonTag(tags, prefix + "colour", colour);
            addAtonTag(tags, prefix + "character", character);
            addAtonTag(tags, prefix + "height", height);
            addAtonTag(tags, prefix + "multiple", multiple);
            addAtonTag(tags, prefix + "range", range);
            addAtonTag(tags, prefix + "group", group);
            addAtonTag(tags, prefix + "period", period);
            addAtonTag(tags, prefix + "sequence", sequence);
            addAtonTag(tags, prefix + "sector_start", sectorStart);
            addAtonTag(tags, prefix + "sector_end", sectorEnd);
        }

        /*************************/
        /** Getters and Setters **/
        /*************************/

        public List<Colour> getColours() {
            return colours;
        }

        public void setColours(List<Colour> colours) {
            this.colours = colours;
        }

        public Character getCharacter() {
            return character;
        }

        public void setCharacter(Character character) {
            this.character = character;
        }

        public Double getHeight() {
            return height;
        }

        public void setHeight(Double height) {
            this.height = height;
        }

        public Integer getMultiple() {
            return multiple;
        }

        public void setMultiple(Integer multiple) {
            this.multiple = multiple;
        }

        public Double getRange() {
            return range;
        }

        public void setRange(Double range) {
            this.range = range;
        }

        public String getGroup() {
            return group;
        }

        public void setGroup(String group) {
            this.group = group;
        }

        public Double getPeriod() {
            return period;
        }

        public void setPeriod(Double period) {
            this.period = period;
        }

        public String getSequence() {
            return sequence;
        }

        public void setSequence(String sequence) {
            this.sequence = sequence;
        }

        public Double getSectorStart() {
            return sectorStart;
        }

        public void setSectorStart(Double sectorStart) {
            this.sectorStart = sectorStart;
        }

        public Double getSectorEnd() {
            return sectorEnd;
        }

        public void setSectorEnd(Double sectorEnd) {
            this.sectorEnd = sectorEnd;
        }
    }
}
