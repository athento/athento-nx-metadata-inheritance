package org.nuxeo.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.operations.InheritMetadataOperation;
import org.nuxeo.runtime.api.Framework;

/**
 * Created by victorsanchez on 31/5/16.
 */
public class InheritMetadataListener implements EventListener {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritMetadataListener.class);

    /** Ignored metadatas. */
    private static String INGORED_METADATAS = "eservicing:category, eservicing:type, eservicing:subtype";

    /** Document types to apply. */
    private static String DOCUMENT_TYPES =  "DocumentEServicing, PhotoEServicing";



    /** Handler. */
    @Override
    public void handleEvent(Event event) throws ClientException {
        // Check document event context
        if (event.getContext() instanceof DocumentEventContext) {
            // FIXME: Set property in ADMINISTRATION PANEL
            String ignoredMetadatas = Framework.getProperty("athento.metadata.inheritance.ignoredMetadatas", INGORED_METADATAS);
            String enabledDocTypes = Framework.getProperty("athento.metadata.inheritance.enabledDoctypes", DOCUMENT_TYPES);
            DocumentModel currentDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            if (documentMustBeApplied(currentDoc, enabledDocTypes)) {
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
    private boolean documentMustBeApplied(DocumentModel currentDoc, String enabledDoctypes) {
        if (currentDoc != null && enabledDoctypes != null) {
            String [] doctypes = enabledDoctypes.split(",");
            for (String docType : doctypes) {
                if (docType.equals(currentDoc.getType())) {
                    return true;
                }
            }
        }
        return false;
    }
}
