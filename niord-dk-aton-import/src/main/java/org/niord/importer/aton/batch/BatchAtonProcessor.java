package org.niord.importer.aton.batch;

import org.niord.core.batch.IBatchable;
import org.niord.core.model.AtonNode;
import org.niord.core.service.AtonService;

import javax.batch.api.chunk.ItemProcessor;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Filters out AtoNs that has not changed
 */
@Named
public class BatchAtonProcessor implements ItemProcessor, IBatchable {

    @Inject
    AtonService atonService;

    /** {@inheritDoc} **/
    @Override
    public Object processItem(Object item) throws Exception {
        AtonNode aton = (AtonNode) item;
        AtonNode orig = atonService.findByAtonUid(aton.getAtonUid());

        if (orig == null) {
            // Persist new AtoN
            return aton;
        } else if (orig.hasChanged(aton)) {
            // Update original
            orig.updateNode(aton);
            return orig;
        }

        // No change, ignore...
        return null;
    }
}
