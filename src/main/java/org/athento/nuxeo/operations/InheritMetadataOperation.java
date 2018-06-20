package org.athento.nuxeo.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.athento.nuxeo.utils.InheritUtil;

@Operation(id = InheritMetadataOperation.ID, category = Constants.CAT_FETCH, label = "Inherit metadatas", description = "Inherit metadatas from parent")
public class InheritMetadataOperation {

    /**
     * Log.
     */
    private static final Log LOG = LogFactory.getLog(InheritMetadataOperation.class);

    /**
     * ID.
     */
    public static final String ID = "InheritMetadata";

    /**
     * Session.
     */
    @Context
    protected CoreSession session;

    /**
     * Schemas to inherit from parent to child.
     */
    @Param(name = "schemas", required = false)
    protected String paramSchemas;

    /**
     * Ignored metadatas in propagation process.
     */
    @Param(name = "ignoreMetadatas", required = false)
    protected String paramIgnoreMetadatas;

    /**
     * Param schemas.
     */
    private String[] schemas;

    /**
     * Param ignored metadatas.
     */
    private String[] ignoredMetadatas;

    /**
     * Run operation.
     *
     * @param doc is the document
     * @return document model with changes
     * @throws Exception on error
     */
    @OperationMethod
    public DocumentModel run(DocumentModel doc) throws Exception {

        // Check document
        if (doc == null) {
            throw new OperationException(
                    "No DocumentModel received. Operation chain must inject a Document in Context");
        }

        // Check parent reference
        if (doc.getParentRef() == null) {
            throw new OperationException("Unable to execute metadata inheritance" +
                    " because document '" + doc.getId() + " has no parent document");
        }

        // Get "inheritable" parent for the document
        DocumentModel parent = getInheritableParent(session, doc);

        // Check if parent has "inheritable" fact
        if (parent == null) {
            throw new OperationException("There is a no parent with facet "
                    + "'inheritable' for the document " + doc.getId());
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("Inherit metadata for " + doc.getId() + " from Parent inheritable " + parent.getId() + ", " + parent.getName());
        }

        // Get ignored from param metadata
        this.ignoredMetadatas = getIgnoredMetadatasFromParam();

        // Get schemas from param in creation mode
        this.schemas = getSchemasFromParam();
        if (this.schemas == null) {
            this.schemas = parent.getSchemas();
        }

        // Get property to check override
        Boolean overrideEmpty = InheritUtil.readConfigValue(session, "metadataInheritanceConfig:overrideValues", true);

        // Propagate schemas from parent to child (only empty properties of document)
        InheritUtil.propagateSchemas(session, parent, doc, this.schemas, this.ignoredMetadatas, !overrideEmpty);

        // Add parentId of inherit schema with parent Id
        InheritUtil.updateProperty(doc, "inheritance:parentId", parent.getId());
        // Refresh update parent metadata to modify inheritable always
        InheritUtil.updateProperty(doc, "inheritance:updateParent", true);

        return doc;
    }

    /**
     * Get inheritable parent of a document.
     *
     * @param session
     * @param doc
     * @return
     */
    private DocumentModel getInheritableParent(CoreSession session, DocumentModel doc) {
        if (doc == null || "Domain".equals(doc.getType())) {
            return null;
        } else {
            DocumentModel parent = session.getDocument(doc.getParentRef());
            if (parent.hasFacet("inheritable")) {
                return parent;
            } else {
                return getInheritableParent(session, parent);
            }
        }
    }

    /**
     * Get ignored metadatas.
     *
     * @return
     */
    private String[] getIgnoredMetadatasFromParam() {
        String[] paramMetadatas = null;
        if (this.paramIgnoreMetadatas != null) {
            paramMetadatas = this.paramIgnoreMetadatas.split(",");
        }
        return paramMetadatas;
    }


    /**
     * Get schemas from parameter.
     *
     * @return
     */
    private String[] getSchemasFromParam() {
        String[] paramSchemas = null;
        if (this.paramSchemas != null) {
            paramSchemas = this.paramSchemas.split(",");
        }
        return paramSchemas;
    }


    /**
     * Check if there are valid schemas to use in propagation process.
     *
     * @return
     */
    private boolean hasSchemas() {
        return this.schemas != null;
    }


    public String getParamIgnoreMetadatas() {
        return paramIgnoreMetadatas;
    }

    public void setParamIgnoreMetadatas(String paramIgnoreMetadatas) {
        this.paramIgnoreMetadatas = paramIgnoreMetadatas;
    }

    public String getParamSchemas() {
        return paramSchemas;
    }

    public void setParamSchemas(String paramSchemas) {
        this.paramSchemas = paramSchemas;
    }

    public void setSession(CoreSession session) {
        this.session = session;
    }


}
