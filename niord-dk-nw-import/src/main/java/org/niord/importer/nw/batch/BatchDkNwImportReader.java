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
 *     "tagId": "8a1e1610-1521-41fb-ac46-a46450bbd998",
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
