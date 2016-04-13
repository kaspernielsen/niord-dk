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
package org.niord.importer.nm.batch;

import org.niord.core.message.Message;
import org.niord.core.message.batch.BatchMessageImportReader;
import org.niord.importer.nm.extract.NmHtmlExtractor;
import org.niord.model.DataFilter;
import org.niord.model.vo.MessageVo;

import javax.inject.Named;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Imports a list of legacy NM messages from HTML file generated by saving an NM Word document as HTML.
 * <p>
 * Please note, the actual dk-nm-import.xml job file is not placed in the META-INF/batch-jobs of this project,
 * but rather, in the META-INF/batch-jobs folder of the niord-web project.<br>
 * This is because of a class-loading bug in the Wildfly implementation. See e.g.
 * https://issues.jboss.org/browse/WFLY-4988
 */
@Named
public class BatchDkNmImportReader extends BatchMessageImportReader {

    /** Reads in the batch import messages */
    @Override
    protected List<MessageVo> readMessages() throws Exception {
        // Extract messages from the HTML
        Path path = batchService.getBatchJobDataFile(jobContext.getInstanceId());
        NmHtmlExtractor extractor = new NmHtmlExtractor(path.toFile());

        getLog().info("Parsing NMs for year " + extractor.getYear() +  ", week " + extractor.getWeek());
        List<Message> messages = extractor.extractNms();

        return messages.stream()
                .map(m -> m.toVo(DataFilter.get().fields(DataFilter.DETAILS, DataFilter.GEOMETRY, "Area.parent")))
                .collect(Collectors.toList());
    }
}
