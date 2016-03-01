package org.niord.importer.aton.batch;

import org.niord.core.batch.AbstractItemHandler;
import org.niord.core.model.AtonNode;

import javax.inject.Named;
import java.io.Serializable;
import java.util.List;

/**
 * Reads AtoNs from the DB
 */
@Named
public class BatchAtonImportReader extends AbstractItemHandler {

    List<AtonNode> atons;
    int atonNo = 0;

    /** {@inheritDoc} **/
    @Override
    public void open(Serializable prevCheckpointInfo) throws Exception {

        atons = batchService.readBatchJobDeflatedDataFile(jobContext.getInstanceId());

        if (prevCheckpointInfo != null) {
            atonNo = (Integer) prevCheckpointInfo;
        }

        getLog().info("Start processing AtoNs from index " + atonNo);
    }

    /** {@inheritDoc} **/
    @Override
    public Object readItem() throws Exception {
        Thread.sleep(1000); // TEST
        if (atonNo < atons.size() && atonNo < 200) {
            getLog().info("Reading AtoN no " + atonNo);
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
