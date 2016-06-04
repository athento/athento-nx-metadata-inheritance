package org.nuxeo.operations;

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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.utils.InheritUtil;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

@Operation(id = InheritMetadataOperation.ID, category = Constants.CAT_FETCH, label = "Inherit metadatas", description = "Inherit metadatas from parent")
public class InheritMetadataOperation {

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritMetadataOperation.class);

    /** ID. */
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
    private String [] schemas;

    /**
     * Param ignored metadatas.
     */
    private String [] ignoredMetadatas;


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

        // Get parent document
        DocumentModel parent = session.getDocument(doc.getParentRef());

        // Check if parent has "inheritable" fact
        if (!parent.hasFacet("inheritable")) {
            throw new OperationException("Parent document of " + doc.getId() + " has no facet 'inheritable'");
        }

        // Get schemas from param
        this.schemas = getSchemasFromParam();
        if (this.schemas == null) {
            this.schemas = parent.getSchemas();
        }

        // Get ignored from param metadata
        this.ignoredMetadatas = getIgnoredMetadatasFromParam();

        // Propagate schemas from parent to child
        InheritUtil.propagateSchemas(parent, doc, this.schemas, this.ignoredMetadatas);

        // Add parentId of inherit schema with parent Id
        String parentId = parent.getId();
        InheritUtil.updateProperty(doc, "inherit:parentId", parentId);
        // Refresh update parent metadata
        InheritUtil.updateProperty(doc, "inherit:updateParent", false);

        return doc;
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
