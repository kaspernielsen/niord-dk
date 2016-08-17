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
import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonTag;
import org.niord.core.user.User;

import javax.inject.Named;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AtoN batch processor used for converting the legacy "AFM" Excel row into OSM seamark format.
 *
 * Filters out AtoNs that has not changed
 *
 * The AtoN model adheres to the OSM seamark specification, please refer to:
 * http://wiki.openstreetmap.org/wiki/Key:seamark
 * and sub-pages.
 */
@Named
public class BatchDkAtonImportProcessor extends AbstractDkAtonImportProcessor {


    /** {@inheritDoc} **/
    @Override
    protected AtonNode parseAtonExcelRow() throws Exception {

        User user = job.getUser();

        AtonNode aton = new AtonNode();

        // TODO: aton.setId();
        aton.setVisible(true);
        aton.setLat(numericValue("LATTITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("AJF_DATO"));
        aton.setUser(user != null ? user.getUsername() : "");
        aton.setUid(user != null ? user.getId() : -1);
        aton.setChangeset(getChangeSet());
        aton.setVersion(1);     // Unknown version

        aton.updateTag(AtonTag.TAG_ATON_UID, stringValue("AFMSTATION"));
        if (StringUtils.isNotBlank(stringValue("FYRLBNR_DK"))) {
            aton.updateTag(AtonTag.TAG_LIGHT_NUMBER, stringValue("FYRLBNR_DK"));
        }

        if (StringUtils.isNotBlank(stringValue("PLADSNAVN"))) {
            aton.updateTag(AtonTag.TAG_LOCALITY, stringValue("PLADSNAVN"));
        }

        if (StringUtils.isNotBlank(stringValue("AFM_NAVN"))) {
            aton.updateTag("seamark:name", stringValue("AFM_NAVN"));
        }

        generateAton(
                aton,
                stringValue("AFUFORKORTELSE"),
                stringValue("BESKRIVELSE"),
                numericValue("KARAKNR").intValue());

        return aton;
    }

    /** Each AtoN will have a combination of these values in the "KARAKNR" field **/
    @SuppressWarnings("unused")
    enum AtonType {

        AIS(0),             // 0: AIS
        LIGHT(1),           // 1: Fyr
        LIGHT_MINOR(2),     // 2: Bifyr, tågelys, advarselslys, retningsfyr, hindringslys, m.v.
        BEACON(3),          // 3: Båker, Signalmaster
        RACON(4),           // 4: RACONS
        LIGHT_BUOY(5),      // 5: Lystønder
        BUOY(6),            // 6: Vagere
        STAKE(7),           // 7: Stager i bund
        RADIO_BEACON(8),    // 8: Radiofyr
        FOG_SIGNAL(9);      // 9: Tågesignaler

        int code;
        AtonType(int code) {
            this.code = code;
        }

        static AtonType findByCode(int code) {
            return Arrays.stream(values())
                    .filter(t -> t.code == code)
                    .findFirst()
                    .orElse(null);
        }

        public boolean isLight() {
            return this == LIGHT || this == LIGHT_MINOR;
        }

        public boolean isBuoy() {
            return this == LIGHT_BUOY || this == BUOY;
        }

        public boolean isBeacon() {
            return this == BEACON || this == STAKE;
        }
    }


    /********************************/
    /** Generating OSM AtoN        **/
    /********************************/

    /**
     * Updates the AtoN with the given tags, which must be in k,v,k,v,...,k,v order.
     *
     * Any occurrence of "${type}" in the keys will be substituted with the "seamark:type" value.
     *
     * @param aton the AtoN to update
     * @param tags the tags
     */
    private void updateAtonTags(AtonNode aton, String... tags) {

        if (tags.length % 2 != 0) {
            throw new IllegalArgumentException("Invalid number of key-value tag parameters " + tags.length);
        }

        // Build a map of the parameters (NB: preserve order)
        Map<String, String> tagLookup = new LinkedHashMap<>();
        for (int x = 0; x < tags.length; x += 2) {
            if (StringUtils.isNotBlank(tags[x]) && StringUtils.isNotBlank(tags[x + 1])) {
                tagLookup.put(tags[x], tags[x + 1]);
            }
        }

        // Ensure that the seamark:type is either specified in the tags parameters or already defined
        String type = StringUtils.isNotBlank(tagLookup.get("seamark:type"))
                ? tagLookup.get("seamark:type")
                : aton.getTagValue("seamark:type");

        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("No seamark:type defined for AtoN " + aton);
        }

        // Update the AtoN
        tagLookup.entrySet().stream()
                .forEach(tag -> {

                    // Substitute "${type}" in the key with the "seamark:type" value
                    String key = tag.getKey();
                    if (key.contains("${type}")) {
                        key = key.replace("${type}", type);
                    }

                    aton.updateTag(key, tag.getValue());
                });

    }


    /**
     * Generates type-specific AtoN OSM tags.
     *
     * Important: The light details are handled by other Excel imports.
     *
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Lights">OpenStreetMap Light definitions</a>
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Beacons">OpenStreetMap Beacon definitions</a>
     * @see <a href="http://wiki.openstreetmap.org/wiki/Seamarks/Buoys>OpenStreetMap Buoy definitions</a>
     *
     * @param shortDesc the FORKORTELSE field
     * @param description the BESKRIVELSE field
     * @param type the "KARAKNR" field
     */
    public void generateAton(AtonNode aton, String shortDesc, String description, int type) {
        if (StringUtils.isBlank(description)) {
            return;
        }

        // An AtoN consists of a master type (e.g. "light") and a set of slave types (e.g. AIS)
        // Currently, only master is handled, and the slave types are left for the other Excel imports.
        Set<AtonType> types = parseType(type);
        AtonType masterType = masterType(types);

        String osmType;
        switch (description) {

            case "AIS Syntetisk AtoN":
                updateAtonTags(aton,
                        "seamark:type",                 "radio_station",
                        "seamark:${type}:category",     "s-ais"
                );
                break;

            case "AIS Virtuel AtoN":
                updateAtonTags(aton,
                        "seamark:type",                 "radio_station",
                        "seamark:${type}:category",     "v-ais"
                );
                break;

            case "Anduvningsfyr":
                updateAtonTags(aton,
                        "seamark:type",                 "light_major",
                        "seamark:light:colour",         "white"
                );
                break;

            case "Bagbord båke - CAN":
                // topmark: 2004, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_lateral",
                        "seamark:${type}:category",     "port",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "cylinder",
                        "seamark:topmark:colour",       "orange"
                );
                break;

            case "Bagbord båke - Trekant ned":
                // topmark: 2164, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_lateral",
                        "seamark:${type}:category",     "port",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "triangle, point down",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Bagbord sideafmærkning":
                if (masterType.isLight()) {
                    updateAtonTags(aton,
                            "seamark:type",             "light_minor",
                            "seamark:light:colour",     "red"
                    );
                    return;
                } else if (masterType.isBeacon()) {
                    updateAtonTags(aton,
                            "seamark:type",             "beacon_lateral",
                            "seamark:${type}:shape",    getBeaconShape(masterType)
                    );
                } else {
                    updateAtonTags(aton,
                            "seamark:type",             "buoy_lateral",
                            "seamark:${type}:shape",    "can" // Guessing (may be "spar")
                    );
                }
                updateAtonTags(aton,
                        "seamark:${type}:category",     "port",
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "red"
                );
                if (shortDesc.equals("PORT m/top")) {
                    updateAtonTags(aton,
                            "seamark:topmark:shape",    "cylinder",
                            "seamark:topmark:colour",   "red"
                    );
                }
                break;

            case "Bagbord skillepkts.afmærkning":
            case "Bagbords skillepunktsafmærkn.":
                // topmark: 2, colour: 9
                updateAtonTags(aton,
                        "seamark:type",                 "buoy_lateral",
                        "seamark:${type}:category",     "preferred_channel_starboard",
                        "seamark:${type}:shape",        "can", // (may be "spar")
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "red;green;red",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "cylinder",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Bagbords molefyr":
                updateAtonTags(aton,
                        "seamark:type",                 "light_minor",
                        "seamark:light:colour",         "red"
                );
                break;

            case "Bifyr med vinkler":
            case "Bifyr, lokalt advarselsfyr":
            case "Bagfyr":
                updateAtonTags(aton,
                        "seamark:type",                 "light_minor"
                        // "seamark:light:colour",         "black"
                );
                break;

            case "Båke (Keglestub) u/top":
                // topmark: -, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "yellow"
                );
                break;

            case "Båke med firkantet plade-top":
                // topmark: 2054, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "board",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Bropassage, bagbord":
                // topmark: 29, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "light",
                        "seamark:light:colour",         "red",
                        "seamark:topmark:shape",        "square",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Bropassage, styrbord":
                // topmark: 28, colour: 2
                updateAtonTags(aton,
                        "seamark:type",                 "light",
                        "seamark:light:colour",         "green",
                        "seamark:topmark:shape",        "triangle, point up",
                        "seamark:topmark:colour",       "green"
                );
                break;

            case "Bropassagesignal":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "light",
                        "seamark:light:colour",         "red"
                );
                break;

            case "CAN-båke i gul tønde, m/top":
                // topmark: 2004, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:${type}:shape",        "pile",
                        "seamark:topmark:shape",        "cylinder",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Emergency Wreck Marking Buoy":
                // topmark: -, colour: 2224
                updateAtonTags(aton,
                        "seamark:type",                 "buoy_special_purpose",
                        "seamark:${type}:category",     "warning",
                        "seamark:${type}:colour",       "blue;yellow",
                        "seamark:${type}:colour_pattern", "vertical",
                        "seamark:${type}:shape",        "pillar"
                );
                break;

            case "Firkantet båke u/top":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "red"
                );
                break;

            case "Forbåke m/top":
                // topmark: 28, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "red", // TODO: verify
                        "seamark:${type}:shape",        "stake",
                        "seamark:topmark:shape",        "triangle, point up",
                        "seamark:topmark:colour",       "green"
                );
                break;

            case "Forfyr":
                // topmark: -, colour: 5
                updateAtonTags(aton,
                        "seamark:type",                 masterType == AtonType.LIGHT_MINOR ? "light_minor" : "light"
                        //"seamark:light:colour",         "black"
                );
                break;

            case "Fredningsbåke":
                // topmark: 4, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "yellow"
                );
                break;

            case "Fundet via Dansk Fyrliste":
                // topmark: -, colour: 23
                if (masterType.isBeacon()) {
                    // Might this be an error?
                    osmType = "beacon_special_purpose";
                } else {
                    osmType = masterType == AtonType.LIGHT_MINOR ? "light_minor" : "light";
                }
                updateAtonTags(aton,
                        "seamark:type",                 osmType,
                        "seamark:${type}:colour",       "amber"
                );
                break;

            case "Fyrtårn":
                // topmark: -, colour: 23
                updateAtonTags(aton,
                        "seamark:type",                 "light_major",
                        "seamark:${type}:colour",       "amber"
                );
                break;

            case "Grave-bagbåke":
            case "Grave-forbåke":
                // topmark: 20, colour: 3
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "white",
                        "seamark:${type}:shape",        "stake",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "white"
                );
                break;

            case "Havn / fredningsomr. bagbåke":
            case "Havn / fredningsomr. forbåke":
                // topmark: 4, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "yellow"
                );
                break;

            case "Hvid stage med rød kugletop":
                // topmark: 11, colour: 3
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "white",
                        "seamark:topmark:shape",        "sphere",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Isoleret fareafmærkning":
                // topmark: 27, colour: 15
                updateAtonTags(aton,
                        "seamark:type",                 "buoy_special_purpose",
                        "seamark:${type}:colour",       "black;red;black",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:${type}:shape",        "pillar", // may also be "spar"
                        "seamark:topmark:shape",        "2 spheres",
                        "seamark:topmark:colour",       "black"
                );
                break;

            case "Jernstang m. diamant-top":
                // topmark: 2104, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "pole",
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "rhombus",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Kabel bagbåke":
                // topmark: 14, colour: 7
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "cable",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "red;white",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "rhombus",
                        "seamark:topmark:colour",       "red;white",
                        "seamark:topmark:colour_pattern", "horizontal" // Verify...
                );
                break;

            case "Kabel forbåke":
                // topmark: 13, colour: 16
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "cable",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "white;red",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "sphere",
                        "seamark:topmark:colour",       "white;red",
                        "seamark:topmark:colour_pattern", "horizontal" // Verify...
                );
                break;

            case "Kabelskilt":
                // TODO: Not right
                // topmark: -, colour: -
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "cable"
                );
                break;

            case "Kompasafmærkning Ø for.":
                // topmark: 24, colour: 13
                updateAtonTags(aton,
                        "seamark:type",                 masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal",
                        "seamark:${type}:category",     "east",
                        "seamark:${type}:shape",        masterType.isBeacon() ? "pile" : "pillar",
                        "seamark:${type}:colour",       "black;yellow;black",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "2 cones base together",
                        "seamark:topmark:colour",       "black"
                );
                break;

            case "Kompasafmærkning N for.":
                // topmark: 23, colour: 12
                updateAtonTags(aton,
                        "seamark:type",                 masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal",
                        "seamark:${type}:category",     "north",
                        "seamark:${type}:shape",        masterType.isBeacon() ? "pile" : "pillar",
                        "seamark:${type}:colour",       "black;yellow",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "2 cones up",
                        "seamark:topmark:colour",       "black"
                );
                break;

            case "Kompasafmærkning S for.":
                // topmark: 25, colour: 11
                updateAtonTags(aton,
                        "seamark:type",                 masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal",
                        "seamark:${type}:category",     "south",
                        "seamark:${type}:shape",        masterType.isBeacon() ? "pile" : "pillar",
                        "seamark:${type}:colour",       "yellow;black",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "2 cones down",
                        "seamark:topmark:colour",       "black"
                );
                break;

            case "Kompasafmærkning V for.":
                // topmark: 26, colour: 10
                updateAtonTags(aton,
                        "seamark:type",                 masterType.isBeacon() ? "beacon_cardinal" : "buoy_cardinal",
                        "seamark:${type}:category",     "west",
                        "seamark:${type}:shape",        masterType.isBeacon() ? "pile" : "pillar",
                        "seamark:${type}:colour",       "yellow;black;yellow",
                        "seamark:${type}:colour_pattern", "horizontal",
                        "seamark:topmark:shape",        "2 cones point together",
                        "seamark:topmark:colour",       "black"
                );
                break;

            case "Meteorologimast":
                // topmark: -, colour: 5
                updateAtonTags(aton,
                        "seamark:type",                 "landmark",
                        "seamark:${type}:shape",        "mast",
                        "seamark:${type}:colour",       "black"
                );
                break;

            case "Midtfarvandsafmærkning":
                // topmark: 1, colour: 7
                updateAtonTags(aton,
                        "seamark:type",                 masterType.isBeacon() ? "beacon_safe_water" : "buoy_safe_water",
                        "seamark:${type}:shape",        masterType.isBeacon() ? "stake" : "pillar",
                        "seamark:${type}:colour",       "red;white",
                        "seamark:${type}:colour_pattern", "vertical",
                        "seamark:topmark:shape",        "sphere",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Pæl m. tværtræ-top":
                // topmark: 2094, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "red",
                        "seamark:${type}:shape",        "pole",
                        "seamark:topmark:shape",        "t-shape",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Pyramide":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:colour",       "red",
                        "seamark:${type}:shape",        "cairn"
                );
                break;

            case "Radiofyr":
                updateAtonTags(aton,
                        "seamark:type",                 "radio_station"
                );
                break;

            case "Rød stage":
                // TODO: Might also be: topmark=2144
                // topmark: 2134, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Rørledningsbåke":
                // topmark: 14, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "pipeline",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "rhombus",
                        "seamark:topmark:colour",       "yellow"
                );
                break;

            case "Retningsfyr":
                // topmark: -, colour: 5
                updateAtonTags(aton,
                        "seamark:type",                 "light_minor"
                        //"seamark:light:colour",         "black"
                );
                break;

            case "sejladsbagbåke":
                // topmark: 9, colour: 7
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "leading",
                        "seamark:${type}:shape",        "stake",
                        "seamark:topmark:shape",        "cone, point down",
                        "seamark:topmark:colour",       "red;white",
                        "seamark:topmark:colour_pattern", "horizontal"
                );
                break;

            case "Sejladsforbåke":
                // topmark: 12, colour: 7
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "leading",
                        "seamark:${type}:shape",        "stake",
                        "seamark:topmark:shape",        "cone, point up",
                        "seamark:topmark:colour",       "red;white",
                        "seamark:topmark:colour_pattern", "horizontal"
                );
                break;

            case "Skydesignal":
                // topmark: -, colour: 5
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:category",     "firing_danger_area",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "black"
                );
                break;

            case "Sluse- og kanalsignal":
                // topmark: -, colour: 5
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "stake",
                        "seamark:${type}:colour",       "black"
                );
                break;

            case "Specialafmærkning":
                // topmark: -/4, colour: 4
                if (masterType.isLight()) {
                    updateAtonTags(aton,
                            "seamark:type",             "light",
                            "seamark:light:colour",     "yellow"
                    );
                    return;
                } else if (masterType.isBeacon()) {
                    updateAtonTags(aton,
                            "seamark:type",             "beacon_special_purpose",
                            "seamark:${type}:shape",    getBeaconShape(masterType),
                            "seamark:${type}:colour",   "yellow"
                    );
                } else {
                    updateAtonTags(aton,
                            "seamark:type",             "buoy_special_purpose",
                            "seamark:${type}:shape",    "pillar",
                            "seamark:${type}:colour",   "yellow"
                    );
                }
                if (shortDesc.equals("SPEC m/top")) {
                    updateAtonTags(aton,
                            "seamark:topmark:shape",    "x-shape",
                            "seamark:topmark:colour",   "yellow"
                    );
                }
                break;

            case "Specialbåke":
                // topmark: -, colour: -
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType)
                );
                break;

            case "stage med X-top":
                // topmark: 2134, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "stage med Y-top":
                // topmark: 2144, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "besom, point down",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Stang m.firkantet plade-top":
                // topmark: 2054, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "pole",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "square",
                        "seamark:topmark:colour",       "yellow"
                );
                break;

            case "Sten-båke i gul tønde, m/top":
                // topmark: 2014, colour: 4
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "pile",
                        "seamark:${type}:colour",       "yellow",
                        "seamark:topmark:shape",        "x-shape",
                        "seamark:topmark:colour",       "yellow"
                );
                break;

            case "Styrbord båke - Trekant op":
                // topmark: 2034, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_lateral",
                        "seamark:${type}:category",     "starboard",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "triangle, point up",
                        "seamark:topmark:colour",       "orange"
                );
                break;

            case "Styrbord molefyr":
                // topmark: -, colour: 2
                updateAtonTags(aton,
                        "seamark:type",                 "light_minor",
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "green"
                );
                break;

            case "Styrbord sideafmærkning":
                if (masterType.isLight()) {
                    updateAtonTags(aton,
                            "seamark:type",             "light_minor",
                            "seamark:light:colour",     "green"
                    );
                    return;
                } else if (masterType.isBeacon()) {
                    updateAtonTags(aton,
                            "seamark:type",             "beacon_lateral",
                            "seamark:${type}:shape",    getBeaconShape(masterType)
                    );
                } else {
                    updateAtonTags(aton,
                            "seamark:type",             "buoy_lateral",
                            "seamark:${type}:shape",    "conical" // Guessing
                    );
                }
                updateAtonTags(aton,
                        "seamark:${type}:category",     "starboard",
                        "seamark:${type}:system",       "iala-a",
                        "seamark:${type}:colour",       "green"
                );
                if (shortDesc.equals("STAR m/top")) {
                    updateAtonTags(aton,
                            "seamark:topmark:shape",    "cone, point up",
                            "seamark:topmark:colour",   "green"
                    );
                }
                break;

            case "Supertønde":
                // topmark: -, colour: 7
                updateAtonTags(aton,
                        "seamark:type",                 "buoy_special_purpose",
                        "seamark:${type}:shape",        "super-buoy",
                        "seamark:${type}:colour",       "red;white",
                        "seamark:${type}:colour_pattern",  "vertical"
                );
                break;

            case "Tågelys":
                // topmark: -, colour: 4
                updateAtonTags(aton,
                        "seamark:type",             "light",
                        "seamark:light:colour",     "yellow"
                );
                break;

            case "Timeglas-båke, m/top":
                // topmark: 2044, colour: 6
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "pile",
                        "seamark:${type}:colour",       "orange",
                        "seamark:topmark:shape",        "2 cones point together",
                        "seamark:topmark:colour",       "orange"
                );
                break;

            case "Treben (jern) + trekant-top":
                // topmark: 2074, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:colour",       "red",
                        "seamark:topmark:shape",        "triangle, point down",
                        "seamark:topmark:colour",       "red"
                );
                break;

            case "Trebenet jernbåke":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:colour",       "red"
                );
                break;

            case "Tremmeværk":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        getBeaconShape(masterType),
                        "seamark:${type}:colour",       "red"
                );
                break;

            case "Varde":
                // topmark: -, colour: 1
                updateAtonTags(aton,
                        "seamark:type",                 "beacon_special_purpose",
                        "seamark:${type}:shape",        "cairn",
                        "seamark:${type}:colour",       "red"
                );
                break;

            case "Vindmølle":
                updateAtonTags(aton,
                        "seamark:type",                 "landmark",
                        "seamark:${type}:category",     "windmotor"
                );
                break;

            case "Vinkelfyr":
                // topmark: -, colour: 5
                if (masterType.isLight()) {
                    updateAtonTags(aton,
                            "seamark:type",             "minor_light"
                            ///"seamark:light:colour",     "black"
                    );
                } else if (masterType.isBeacon()) {
                    updateAtonTags(aton,
                            "seamark:type",             "beacon_special_purpose",
                            "seamark:${type}:shape",    getBeaconShape(masterType),
                            "seamark:${type}:colour",   "black"
                    );
                }
                break;
        }
    }


    /** Generates the default beacon shape */
    private String getBeaconShape(AtonType masterType) {
        // Best we can do without to proper data
        switch (masterType) {
            case STAKE: return "stake";
        }
        return null;
    }


    /*************************/
    /** AtoN Type Parsing   **/
    /*************************/

    /** Parses the "KARAKNR" field into individual types **/
    Set<AtonType> parseType(int type) {
        // Each digit denotes a separate type
        return Arrays.stream(String.valueOf(type).split(""))
                .map(Integer::valueOf)
                .map(AtonType::findByCode)
                .collect(Collectors.toSet());
    }


    /** Resolves the master type of the type set **/
    AtonType masterType(Set<AtonType> types) {
        // Single-type case
        if (types.size() == 1) {
            return types.iterator().next();
        }

        // Check for one of the master types.
        // The master types are ordered, so that e.g. stake+light will return stake.
        AtonType[] masterTypes = { AtonType.BEACON, AtonType.BUOY, AtonType.STAKE,
                AtonType.LIGHT, AtonType.LIGHT_MINOR, AtonType.LIGHT_BUOY };
        return Arrays.stream(masterTypes)
                .filter(types::contains)
                .findFirst()
                .orElse(types.iterator().next());
    }

}
