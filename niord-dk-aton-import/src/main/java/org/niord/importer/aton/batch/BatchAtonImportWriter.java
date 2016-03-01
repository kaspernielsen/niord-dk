package org.niord.importer.aton.batch;

import org.niord.core.aton.AtonNode;
import org.niord.core.aton.AtonService;
import org.niord.core.batch.AbstractItemHandler;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * Persists the AtoNs to the database
 */
@Named
public class BatchAtonImportWriter extends AbstractItemHandler {

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
        getLog().info(String.format("Persisted %d AtoNs in %d s", items.size(), (System.currentTimeMillis() - t0) / 1000L));
    }
}
