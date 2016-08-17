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
package org.niord.importer.aton;

import org.junit.Assert;
import org.junit.Test;
import org.niord.importer.aton.batch.DkFogSignalParser;
import org.niord.importer.aton.batch.DkLightParser;
import org.niord.importer.aton.batch.FogSignalSeamark;
import org.niord.importer.aton.batch.LightSeamark;

import java.util.Arrays;
import java.util.regex.Matcher;

/**
 * Test light import functionality
 */
public class LightImporterTest {

    String[] LIGHT_CHARATERISTICS = {
            "Mo(U)15s",
            "Fl(2+1)W.10s",
            "Iso.WRG.4s",
            "Al Fl.WR.4s",
            "Fl.G.3s",
            "F.R",
            "VQ+LFl.R",
            "Oc.WRG.5s",
            "2 Oc.W.R G.1,5s",
    };

    String[] FOG_SIGNALS = {
            "HORN(3)30s   (2+2+2+2+2+20)",
            "SIREN(1)30s   (5+25)",
            "BELL.15s   (2,5+12,5)",
            "HORN   MO(U)30s   (0,75+1+0,75+1+2,5+24)",
            "HORN",
            "Horn(2)60s (5+5+5+45)"
    };

    @Test
    public void testLightFormat() throws Exception {
        Arrays.stream(LIGHT_CHARATERISTICS).forEach(c -> {
            Matcher m = DkLightParser.LIGHT_CHARACTER_FORMAT.matcher(c);
            System.out.println("===== " + c + "=====");
            if (m.find()) {
                try {
                    if (m.group("multiple") != null) System.out.println("\tmultiple " + m.group("multiple"));
                    if (m.group("phase") != null) System.out.println("\tphase " + m.group("phase"));
                    if (m.group("group") != null) System.out.println("\tgroup " + m.group("group"));
                    if (m.group("colors") != null) System.out.println("\tcolors " + m.group("colors"));
                    if (m.group("period") != null) System.out.println("\tperiod " + m.group("period"));
                } catch (Exception e) {
                    System.out.println("\t-> " + e.getMessage());
                }
            }
        });
    }

    @Test
    public void testLightParsing() throws Exception {
        Arrays.stream(LIGHT_CHARATERISTICS).forEach(c -> {
            LightSeamark light = DkLightParser.newInstance();
            DkLightParser.parseLightCharacteristics(light, c);
            Assert.assertTrue(light.isValid());

            System.out.println("===== " + c + "=====");
            light.toOsm().forEach(t -> System.out.printf("<tag k='%s' v='%s'/>%n", t.getK(), t.getV()));
        });
    }

    @Test
    public void testFogSignalFormat() throws Exception {
        Arrays.stream(FOG_SIGNALS).forEach(f -> {
            Matcher m = DkFogSignalParser.FOG_SIGNAL_FORMAT.matcher(f);
            System.out.println("===== " + f + "=====");
            if (m.find()) {
                try {
                    if (m.group("category") != null) System.out.println("\tcategory " + m.group("category"));
                    if (m.group("morse") != null) System.out.println("\tmorse " + m.group("morse"));
                    if (m.group("group") != null) System.out.println("\tgroup " + m.group("group"));
                    if (m.group("period") != null) System.out.println("\tperiod " + m.group("period"));
                } catch (Exception e) {
                    System.out.println("\t-> " + e.getMessage());
                }
            }
        });
    }

    @Test
    public void testFogSignalParsing() throws Exception {
        Arrays.stream(FOG_SIGNALS).forEach(f -> {
            FogSignalSeamark fogSignal = DkFogSignalParser.newInstance();
            DkFogSignalParser.parseFogSignal(fogSignal, f);
            Assert.assertTrue(fogSignal.isValid());

            System.out.println("===== " + f + "=====");
            fogSignal.toOsm().forEach(t -> System.out.printf("<tag k='%s' v='%s'/>%n", t.getK(), t.getV()));
        });
    }

}
