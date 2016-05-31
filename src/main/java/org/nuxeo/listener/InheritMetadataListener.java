package org.nuxeo.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.operations.InheritMetadataFromParentOperation;
import org.nuxeo.operations.InheritMetadataOperation;
import org.nuxeo.runtime.api.Framework;

/**
 * Created by victorsanchez on 31/5/16.
 */
public class InheritMetadataListener implements EventListener {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritMetadataListener.class);

    /** Handler. */
    @Override
    public void handleEvent(Event event) throws ClientException {
        // Check document event context
        if (event.getContext() instanceof DocumentEventContext) {
            // Get current document
            DocumentModel currentDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            // Check document to know it it is container of other to start inhertance to his children
            if (parentDocumentMustBeApplied(currentDoc)) {
                LOG.info("Inheritable...");
                // Execute operation
                InheritMetadataFromParentOperation op = new InheritMetadataFromParentOperation();
                try {
                    op.setSession(event.getContext().getCoreSession());
                    op.run(currentDoc);
                } catch (Exception e) {
                    LOG.error("Unable to execute inherit metadata from parent operation", e);
                }
            } else if (documentMustBeApplied(currentDoc)) {
                LOG.info("Inheritor...");
                // FIXME: Set property in ADMINISTRATION PANEL
                String ignoredMetadatas = Framework.getProperty("athento.metadata.inheritance.ignoredMetadatas");
                // Execute operation
                InheritMetadataOperation op = new InheritMetadataOperation();
                try {
                    op.setSession(event.getContext().getCoreSession());
                    op.setParamIgnoreMetadatas(ignoredMetadatas);
                    // FIX: Add only schemas here if it is necessary
                    op.run(currentDoc);
                } catch (Exception e) {
                    LOG.error("Unable to execute inherit metadata operation", e);
                }
            }
        }
    }

    /**
     * Check if document type is valid to start inheritance.
     *
     * @param currentDoc
     * @return
     */
    private boolean documentMustBeApplied(DocumentModel currentDoc) {
        if (currentDoc != null) {
           return currentDoc.hasFacet("inheritor");
        }
        return false;
    }

    /**
     * Check if parent document type is valid to start inheritance.
     *
     * @param currentDoc
     * @return
     */
    private boolean parentDocumentMustBeApplied(DocumentModel currentDoc) {
        if (currentDoc != null) {
            return currentDoc.hasFacet("inheritable");
        }
        return false;
    }
}
