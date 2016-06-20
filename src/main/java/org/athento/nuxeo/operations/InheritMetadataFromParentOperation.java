package org.athento.nuxeo.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.athento.nuxeo.utils.InheritUtil;
import org.nuxeo.ecm.core.api.VersioningOption;
import org.nuxeo.ecm.core.schema.FacetNames;
import org.nuxeo.ecm.core.versioning.VersioningService;

@Operation(id = InheritMetadataFromParentOperation.ID, category = Constants.CAT_FETCH, label = "Inherit metadatas from parent", description = "Inherit metadatas from parent")
public class InheritMetadataFromParentOperation {

    /** ID. */
    public static final String ID = "InheritMetadataFromParent";

    /**
     * Session.
     */
    @Context
    protected CoreSession session;


    /**
     * Run operation.
     *
     * @param doc is the document
     * @return document model with changes
     * @throws Exception on error
     */
    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {

        // Check parent document
        if (doc == null) {
            throw new OperationException(
                    "No DocumentModel received. Operation chain must inject a Document (parent) in Context");
        }

        // Check if parent has "inheritable" fact
        if (!doc.hasFacet("inheritable")) {
            throw new OperationException("Document " + doc.getId() + " has no facet 'inheritable'");
        }

        // Find children with facet "inheritor"
        DocumentModelList inheritorDocs = getChildren(doc);

        // Get ignored metadatas
        String ignoredMetadatas = InheritUtil.readConfigValue(session, "metadataInheritanceConfig:ignoredMetadatas", "");

        for (DocumentModel inheritorDoc : inheritorDocs) {
            // Execute operation
            InheritMetadataOperation op = new InheritMetadataOperation();
            try {
                op.setSession(session);
                op.setParamIgnoreMetadatas(ignoredMetadatas);
                op.run(inheritorDoc);
                // Set updateParent to false for children modification. It does document modification
                // ignores parent "inheritable" modification.
                inheritorDoc.setPropertyValue("inherit:updateParent", false);
                // Increase version
                if (inheritorDoc.hasFacet(FacetNames.VERSIONABLE)) {
                    inheritorDoc.putContextData(VersioningService.VERSIONING_OPTION, VersioningOption.MINOR);
                }
                session.saveDocument(inheritorDoc);
            } catch (Exception e) {
                LOG.error("Unable to execute inherit metadata operation", e);
            }
        }

        return doc;
    }


    
    
    /**
     * Get children (query TREE mode).
     *
     * @return document list
     */
    private DocumentModelList getChildren(DocumentModel doc) {
        String NXQL = String.format("SELECT * FROM Document WHERE " +
                "ecm:mixinType = 'inheritor' AND ecm:path STARTSWITH '%s' AND " +
                "ecm:currentLifeCycleState != 'deleted'", doc.getPathAsString());
        return session.query(NXQL);
    }

    public void setSession(CoreSession session) {
        this.session = session;
    }

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritMetadataFromParentOperation.class);

}
