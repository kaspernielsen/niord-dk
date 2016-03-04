/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.niord.importer.aton;

import org.junit.Assert;
import org.junit.Test;
import org.niord.importer.aton.batch.DkLightParser;
import org.niord.importer.aton.batch.LightSeamark;

import java.util.Arrays;
import java.util.regex.Matcher;

/**
 * Test light import functionality
 */
public class LightImporterTest {

    String[] characteristics = {
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

    @Test
    public void testAtonSeamark() throws Exception {
        Arrays.stream(characteristics).forEach(c -> {
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
        Arrays.stream(characteristics).forEach(c -> {
            LightSeamark light = DkLightParser.parseLightCharacteristics(c);
            Assert.assertNotNull(light);

            System.out.println("===== " + c + "=====");
            light.toOsm().forEach(t -> System.out.printf("<tag k='%s' v='%s'/>%n", t.getK(), t.getV()));
        });
    }

}
