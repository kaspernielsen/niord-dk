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
package org.niord.importer.nw.batch;

import org.niord.core.batch.AbstractItemHandler;
import org.niord.core.message.Message;
import org.niord.core.util.JsonUtils;
import org.niord.importer.nw.LegacyNwImportRestService.ImportLegacyNwData;
import org.niord.importer.nw.LegacyNwImportService;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;

/**
 * Imports a list of legacy NW messages.
 * <p>
 * Please note, the actual dk-nw-import.xml job file is not placed in the META-INF/batch-jobs of this project,
 * but rather, in the META-INF/batch-jobs folder of the niord-web project.<br>
 * This is because of a class-loading bug in the Wildfly implementation. See e.g.
 * https://issues.jboss.org/browse/WFLY-4988
 * <p>
 * Format of json file is defined by the ImportActiveLegacyNwParams class. Example:
 * <pre>
 * {
 *     "seriesId": "dma-legacy-nw",
 *     "tagName": "NW import 2. april",
 *     "ids": [ 14831,14782,14722,14714,14824,14829,14846,14809,14719 ]
 * }
 * </pre>
 */
@Named
public class BatchDkNwImportReader extends AbstractItemHandler {

    @Inject
    LegacyNwImportService importService;

    ImportLegacyNwData importParams;
    int nwNo = 0;

    /** {@inheritDoc} **/
    @Override
    public void open(Serializable prevCheckpointInfo) throws Exception {

        // Load the import params from the batch data
        importParams = JsonUtils.readJson(
                ImportLegacyNwData.class,
                batchService.getBatchJobDataFile(jobContext.getInstanceId()));

        if (prevCheckpointInfo != null) {
            nwNo = (Integer) prevCheckpointInfo;
        }

        getLog().info("Start processing " + importParams.getIds().size() + " legacy NWs from index " + nwNo);
    }

    /** {@inheritDoc} **/
    @Override
    public Object readItem() throws Exception {
        if (nwNo < importParams.getIds().size()) {

            Integer id = importParams.getIds().get(nwNo++);
            Message message = importService.readMessage(importParams, id);

            // Every now and then, update the progress
            if (nwNo % 10 == 0) {
                updateProgress((int)(100.0 * nwNo / importParams.getIds().size()));
            }

            getLog().info("Reading legacy NW no " + nwNo);
            return message;
        }
        return null;
    }

    /** {@inheritDoc} **/
    @Override
    public Serializable checkpointInfo() throws Exception {
        return nwNo;
    }
}
