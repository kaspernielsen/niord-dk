package org.niord.importer.aton.batch;

import org.niord.core.batch.AbstractItemHandler;
import org.niord.core.model.AtonNode;
import org.niord.core.service.AtonService;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Filters out AtoNs that has not changed
 */
@Named
public class BatchAtonImportProcessor extends AbstractItemHandler {

    @Inject
    AtonService atonService;

    /** {@inheritDoc} **/
    @Override
    public Object processItem(Object item) throws Exception {
        AtonNode aton = (AtonNode) item;
        AtonNode orig = atonService.findByAtonUid(aton.getAtonUid());

        if (orig == null) {
            // Persist new AtoN
            getLog().info("Persisting new AtoN");
            return aton;

        } else if (orig.hasChanged(aton)) {
            // Update original
            getLog().info("Updating AtoN " + orig.getId());
            orig.updateNode(aton);
            return orig;
        }

        // No change, ignore...
        getLog().info("Ignoring unchanged AtoN " + orig.getId());
        return null;
    }
}
