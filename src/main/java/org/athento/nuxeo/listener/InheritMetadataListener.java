package org.athento.nuxeo.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.athento.nuxeo.utils.InheritUtil;
import org.athento.nuxeo.worker.PropagateMetadataFromParentWorker;
import org.athento.nuxeo.worker.PropagateMetadataWorker;
import org.nuxeo.ecm.core.api.*;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.PostCommitFilteringEventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;
import org.nuxeo.ecm.core.work.api.Work;
import org.nuxeo.ecm.core.work.api.WorkManager;
import org.nuxeo.runtime.api.Framework;

/**
 * Created by victorsanchez on 31/5/16.
 */
public class InheritMetadataListener implements PostCommitFilteringEventListener {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(InheritMetadataListener.class);

    /**
     * Handle event.
     *
     * @param events
     * @throws ClientException
     */
    @Override
    public void handleEvent(EventBundle events) throws ClientException {
        for (Event event : events) {
            if (acceptEvent(event)) {
                handleEvent(event);
            }
        }
    }
    /**
     * Handler.
     */
    public void handleEvent(Event event) throws ClientException {
        CoreSession session = event.getContext().getCoreSession();
        // Check enabled
        if (!InheritUtil.readConfigValue(session, "metadataInheritanceConfig:enableInheritance", true)) {
            return;
        }
        // Check document event context
        if (event.getContext() instanceof DocumentEventContext) {
            String eventName = event.getName();
            // Get current document
            final DocumentModel currentDoc = ((DocumentEventContext) event.getContext()).getSourceDocument();
            // Ignore versions
            if (currentDoc.isVersion()) {
                return;
            }
            // Check document to know it it is container of other to start inheritance to its children
            if (parentDocumentMustBeApplied(currentDoc)) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Inheritable " + currentDoc.getId() + " executing inheritance...");
                }
                try {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Worker for inherit parent metadata...");
                    }
                    boolean ignoreVersions = InheritUtil.readConfigValue(session, "metadataInheritanceConfig:ignoreVersions", true);
                    PropagateMetadataFromParentWorker worker = new PropagateMetadataFromParentWorker("default", currentDoc.getId());
                    worker.setIgnoreVersions(ignoreVersions);
                    WorkManager workManager = Framework.getLocalService(WorkManager.class);
                    workManager.schedule(worker, WorkManager.Scheduling.IF_NOT_SCHEDULED);
                    String workId = worker.getId();
                    Work.State workState = workManager.getWorkState(workId);
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Work from parent [" + workId + "] queued in state [" + workState + "]");
                    }

                } catch (Exception e) {
                    LOG.error("Unable to execute inherit metadata from parent operation", e);
                }
            } else if (documentMustBeApplied(currentDoc)) {
                final String ignoredMetadatas = InheritUtil.readConfigValue(session, "metadataInheritanceConfig:ignoredMetadatas", "");
                if (DocumentEventTypes.DOCUMENT_UPDATED.equals(eventName)) {
                    // Check sibling inheritance
                    if (isSiblingInheritanceEnabled(session)) {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Sibling is enabled: inheritor " + currentDoc.getId() + " updated, check updated parent...");
                        }
                        // Check update parent
                        if (!InheritUtil.hasRelation(currentDoc)) {
                            if (updateInheritableParent(currentDoc)) {
                                String inheritableParentId = (String) currentDoc.getPropertyValue("inheritance:parentId");
                                if (inheritableParentId != null && !inheritableParentId.isEmpty()) {
                                    // Find first inheritable parent
                                    DocumentModel parentDoc = session.getDocument(new IdRef(inheritableParentId));
                                    // When update document only the updated metadata will be propagated
                                    if (currentDoc.hasSchema("inheritance")) {
                                        if (LOG.isInfoEnabled()) {
                                            LOG.info("Propagate all allowed schemas: " + parentDoc.getRef());
                                        }
                                        // Propagate all allowed parent schemas
                                        InheritUtil.propagateSchemas(session, currentDoc, parentDoc, parentDoc.getSchemas(), ignoredMetadatas.split(","), false);
                                        // Increase version
                                        if (parentDoc.hasFacet(FacetNames.VERSIONABLE)) {
                                            parentDoc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                                        }
                                        // Save parent doc
                                        session.saveDocument(parentDoc);
                                    } else {
                                        LOG.warn("Inheritance metadata is not found into document inherited.");
                                    }
                                }
                            }
                        }
                    }
                } else if (DocumentEventTypes.DOCUMENT_CREATED.equals(eventName)
                        || DocumentEventTypes.DOCUMENT_MOVED.equals(eventName)
                        || DocumentEventTypes.DOCUMENT_DUPLICATED.equals(eventName)) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Inheritor " + currentDoc.getId() + " created, duplicated or moved...");
                    }
                    try {
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Worker for inherit metadata from " + eventName);
                        }
                        // Load add for document
                        PropagateMetadataWorker worker = new PropagateMetadataWorker("default", currentDoc.getId());
                        worker.setIgnoredMetadatas(ignoredMetadatas);
                        WorkManager workManager = Framework.getLocalService(WorkManager.class);
                        workManager.schedule(worker, WorkManager.Scheduling.IF_NOT_SCHEDULED);
                        String workId = worker.getId();
                        Work.State workState = workManager.getWorkState(workId);
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Work [" + workId + "] queued in state [" + workState + "]");
                        }
                    } catch (Exception e) {
                        throw new ClientException("Unable to execute inherit metadata operation", e);
                    }
                }
            }
        }
    }

    /**
     * Check if sibling inheritance is enabled.
     *
     * @param session
     * @return
     */
    private boolean isSiblingInheritanceEnabled(CoreSession session) {
        return InheritUtil.readConfigValue(session, "metadataInheritanceConfig:enableSiblingInheritance", false);
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
        if (doc.getPropertyValue("inheritance:updateParent") != null) {
            updateParent = (Boolean) doc.getPropertyValue("inheritance:updateParent");
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

    /**
     * Accept events.
     *
     * @param event
     * @return
     */
    @Override
    public boolean acceptEvent(Event event) {
        return DocumentEventTypes.DOCUMENT_CREATED.equals(event.getName())
                || DocumentEventTypes.DOCUMENT_MOVED.equals(event.getName())
                || DocumentEventTypes.DOCUMENT_DUPLICATED.equals(event.getName())
                || DocumentEventTypes.DOCUMENT_UPDATED.equals(event.getName());
    }
}
