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
package org.niord.importer.aton.batch;

import org.niord.core.aton.AtonNode;
import org.niord.model.aton.AtonNodeVo;
import org.slf4j.Logger;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.util.List;

/**
 * Dummy no-op item writer that does not persist  the item.
 *
 * Useful for development purposes.
 */
@Named
public class NoopAtonImportItemWriter extends AbstractItemWriter {

    @Inject
    Logger log;

    /** {@inheritDoc} */
    @Override
    public void writeItems(List<Object> items) throws Exception {

        for (Object i : items) {
            AtonNode aton = (AtonNode) i;
            log.info("\n== Ignoring AtonItem ==\n" + printResult(aton.toVo()));
        }
    }

    /** Formats the aton as XML */
    private String printResult(AtonNodeVo aton) {

        StringWriter w = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AtonNodeVo.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(aton, w);
        } catch (JAXBException ignored) {
        }
        return w.toString();
    }

}
