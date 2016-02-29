package org.niord.importer.aton.batch;

import org.niord.core.batch.BatchService;
import org.niord.core.batch.IBatchable;
import org.niord.core.model.AtonNode;
import org.slf4j.Logger;

import javax.batch.api.chunk.AbstractItemReader;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Reads AtoNs from the DB
 */
@Named
public class BatchAtonReader extends AbstractItemReader implements IBatchable {

    @Inject
    private JobContext jobContext;

    @Inject
    Logger log;

    @Inject
    BatchService batchService;

    List<AtonNode> atons;
    int atonNo = 0;

    /** {@inheritDoc} **/
    @Override
    public void open(Serializable prevCheckpointInfo) throws Exception {

        atons = batchService.readBatchJobDeflatedDataFile(jobContext.getInstanceId());

        if (prevCheckpointInfo != null) {
            atonNo = (Integer) prevCheckpointInfo;
        }

        log.info("Start processing AtoNs from index " + atonNo);
    }

    /** {@inheritDoc} **/
    @Override
    public Object readItem() throws Exception {
        Thread.sleep(1000); // TEST
        if (atonNo < atons.size() && atonNo < 200) {
            return atons.get(atonNo++);
        }
        return null;
    }

    /** {@inheritDoc} **/
    @Override
    public Serializable checkpointInfo() throws Exception {
        return atonNo;
    }
}
