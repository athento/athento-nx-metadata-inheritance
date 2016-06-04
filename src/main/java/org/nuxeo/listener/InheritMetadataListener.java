package org.nuxeo.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.operations.InheritMetadataFromParentOperation;
import org.nuxeo.operations.InheritMetadataOperation;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.utils.InheritUtil;

import java.util.Map;

/**
 * Created by victorsanchez on 31/5/16.
 */
public class InheritMetadataListener implements EventListener {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritMetadataListener.class);

    /** Events. */
    private static final String CREATION_EVENT_NAME = "documentCreated";
    private static final String MODIFICATION_EVENT_NAME = "documentModified";

    /** Handler. */
    @Override
    public void handleEvent(Event event) throws ClientException {
        // Check document event context
        if (event.getContext() instanceof DocumentEventContext) {
            String eventName = event.getName();
            // Get current document
            DocumentModel currentDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            // Check document to know it it is container of other to start inheritance to his children
            if (parentDocumentMustBeApplied(currentDoc)) {
                // Execute operation
                InheritMetadataFromParentOperation op = new InheritMetadataFromParentOperation();
                try {
                    op.setSession(event.getContext().getCoreSession());
                    op.run(currentDoc);
                } catch (Exception e) {
                    LOG.error("Unable to execute inherit metadata from parent operation", e);
                }
            } else if (documentMustBeApplied(currentDoc)) {
                // FIXME: Set property in ADMINISTRATION PANEL
                String ignoredMetadatas = Framework.getProperty("athento.metadata.inheritance.ignoredMetadatas");
                if (MODIFICATION_EVENT_NAME.equals(eventName)) {
                    boolean updateParent = false;
                    // Check if inheritor document must be update inheritable parent document
                    if (currentDoc.getPropertyValue("inherit:updateParent") != null) {
                        updateParent = (Boolean) currentDoc.getPropertyValue("inherit:updateParent");
                    }
                    // Check update parent
                    if (updateParent) {
                        String inheritableParentId = (String) currentDoc.getPropertyValue("inherit:parentId");
                        if (inheritableParentId != null && !inheritableParentId.isEmpty()) {
                            CoreSession session = event.getContext().getCoreSession();
                            // Find first inheritable parent
                            DocumentModel parentDoc = session.getDocument(new IdRef(inheritableParentId));
                            // Update parent document with current document schemas
                            InheritUtil.propagateSchemas(currentDoc, parentDoc, currentDoc.getSchemas(), ignoredMetadatas.split(","));
                            session.saveDocument(parentDoc);
                        }
                    }
                } else if (CREATION_EVENT_NAME.equals(eventName)) {
                    // Execute operation
                    InheritMetadataOperation op = new InheritMetadataOperation();
                    try {
                        op.setSession(event.getContext().getCoreSession());
                        op.setParamIgnoreMetadatas(ignoredMetadatas);
                        // FIX: Add only schemas here if it is necessary
                        op.run(currentDoc);
                    } catch (Exception e) {
                        throw new ClientException("Unable to execute inherit metadata operation", e);
                    }
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
