package org.niord.importer.aton.batch;

import org.niord.core.model.AtonNode;
import org.niord.core.service.AtonService;
import org.slf4j.Logger;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Persists the AtoNs to the database
 */
@Named
public class BatchAtonWriter extends AbstractItemWriter {

    @Inject
    Logger log;

    @Inject
    AtonService atonService;

    /** {@inheritDoc} **/
    @Override
    public void writeItems(List<Object> items) throws Exception {
        long t0 = System.currentTimeMillis();
        for (Object i : items) {
            AtonNode aton = (AtonNode) i;
            atonService.saveEntity(aton);
        }
        log.info(String.format("Persisted %d AtoNs in %d s", items.size(), (System.currentTimeMillis() - t0) / 1000L));
    }
}
