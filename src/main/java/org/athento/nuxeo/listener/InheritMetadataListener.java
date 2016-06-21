package org.athento.nuxeo.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.athento.nuxeo.operations.InheritMetadataFromParentOperation;
import org.athento.nuxeo.operations.InheritMetadataOperation;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.runtime.api.Framework;
import org.athento.nuxeo.utils.InheritUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by victorsanchez on 31/5/16.
 */
public class InheritMetadataListener implements EventListener {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(InheritMetadataListener.class);

    /**
     * Ignored documents to update.
     */
    private static List<DocumentModel> ignoredForUpdates = Collections.synchronizedList(new ArrayList<DocumentModel>());

    /**
     * Handler.
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
        // Check document event context
        if (event.getContext() instanceof DocumentEventContext) {
            String eventName = event.getName();
            // Get current document
            DocumentModel currentDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            if (ignoredForUpdate(currentDoc)) {
                ignoredForUpdates.remove(currentDoc);
                return;
            }
            // Ignore versions
            if (currentDoc.isVersion()) {
                return;
            }
            // Check document to know it it is container of other to start inheritance to his children
            if (parentDocumentMustBeApplied(currentDoc)) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Inheritable " + currentDoc.getId() + " executing inheritance...");
                }
                // Execute operation
                InheritMetadataFromParentOperation op = new InheritMetadataFromParentOperation();
                try {
                    op.setSession(event.getContext().getCoreSession());
                    op.run(currentDoc);
                } catch (Exception e) {
                    LOG.error("Unable to execute inherit metadata from parent operation", e);
                }
            } else if (documentMustBeApplied(currentDoc)) {
                CoreSession session = event.getContext().getCoreSession();
                // FIXME: Set property in ADMINISTRATION PANEL
                String ignoredMetadatas = Framework.getProperty("athento.metadata.inheritance.ignoredMetadatas");
                if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventName)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Inheritor " + currentDoc.getId() + " updated...");
                    }
                    // Check update parent
                    if (updateInheritableParent(currentDoc)) {
                        String inheritableParentId = (String) currentDoc.getPropertyValue("inherit:parentId");
                        if (inheritableParentId != null && !inheritableParentId.isEmpty()) {
                            // Find first inheritable parent
                            DocumentModel parentDoc = session.getDocument(new IdRef(inheritableParentId));
                            // Update parent document with current document schemas
                            InheritUtil.propagateSchemas(session, currentDoc, parentDoc, currentDoc.getSchemas(), ignoredMetadatas.split(","));
                            // Increase version
                            if (parentDoc.hasFacet(FacetNames.VERSIONABLE)) {
                                parentDoc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                            }
                            session.saveDocument(parentDoc);
                        }
                    } else {
                        currentDoc.setPropertyValue("inherit:updateParent", true);
                        ignoredForUpdates.add(currentDoc);
                        session.saveDocument(currentDoc);
                    }
                } else if (DocumentEventTypes.DOCUMENT_CREATED.equals(eventName)
                        || DocumentEventTypes.DOCUMENT_MOVED.equals(eventName)
                        || DocumentEventTypes.DOCUMENT_DUPLICATED.equals(eventName)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Inheritor " + currentDoc.getId() + " created, duplicated or moved...");
                    }
                    // Execute operation
                    InheritMetadataOperation op = new InheritMetadataOperation();
                    try {
                        op.setSession(event.getContext().getCoreSession());
                        op.setParamIgnoreMetadatas(ignoredMetadatas);
                        // FIX: Add only schemas here if it is necessary
                        op.run(currentDoc);
                        // Save document to propagate update event
                        session.saveDocument(currentDoc);
                    } catch (Exception e) {
                        throw new ClientException("Unable to execute inherit metadata operation", e);
                    }
                }
            }
        }
    }

    /**
     * Check if a document is ignored for update.
     *
     * @param currentDoc
     * @return
     */
    private boolean ignoredForUpdate(DocumentModel currentDoc) {
        return ignoredForUpdates.contains(currentDoc);
    }

    /**
     * Check if document must be modify "inheritable" parent document.
     *
     * @param doc
     * @return
     */
    private boolean updateInheritableParent(DocumentModel doc) {
        // By default, always update "inheritable" parent document
        boolean updateParent = true;
        // Check if inheritor document must be update inheritable parent document
        if (doc.getPropertyValue("inherit:updateParent") != null) {
            updateParent = (Boolean) doc.getPropertyValue("inherit:updateParent");
        }
        return updateParent;
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
