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
     * IGNORED SCHEMAS.
     */
    private static String[] DEFAULT_IGNORED_SCHEMAS = { "dublincore", "common", "uid", "file", "files" };

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

        // Get parent schemas
        String[] parentSchemas = parent.getSchemas();
        String[] childSchemas = doc.getSchemas();

        // Get schemas from param
        this.schemas = getSchemasFromParam();

        // Get ignored from param metadata
        this.ignoredMetadatas = getIgnoredMetadatasFromParam();

        // Propagate schemas
        for (String parentSchema : parentSchemas) {
            for (String childSchema : childSchemas) {
                if (parentSchema.equals(childSchema)
                        && isValidToPropagateSchema(parentSchema)) {
                    // Get properties from valid schema to propagate to child
                    Map<String, Object> properties = parent.getProperties(parentSchema);
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        String metadata = entry.getKey();
                        if (!metadataMustBeIgnored(metadata)) {
                            Object value = parent.getPropertyValue(metadata);
                            LOG.info("Update metadata " + metadata + " with " + value);
                            // Update property of child document
                            updateProperty(doc, metadata, value);
                        }
                    }
                }
            }
        }

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
     * Check metadata to be ignored.
     *
     * @param metadata
     * @return
     */
    private boolean metadataMustBeIgnored(String metadata) {
        boolean ignore = false;
        if (metadata == null) {
            ignore = true;
        }
        for (String ignoredMetadata : this.ignoredMetadatas) {
            if (metadata.equals(ignoredMetadata.trim())) {
                ignore = true;
                break;
            }
        }
        return ignore;
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
     * Check if parent document type schema is valid to propagate to child document.
     *
     * @param schema
     * @return
     */
    private boolean isValidToPropagateSchema(String schema) {
        boolean valid = false;
        if (!schemaMustBeIgnored(schema)) {
            if (hasSchemas()) {
                for (String paramSchema : this.schemas) {
                    if (schema.equals(paramSchema.trim())) {
                        valid = true;
                        break;
                    }
                }
            } else {
                valid = true;
            }
        }
        return valid;
    }

    /**
     * Check if there are valid schemas to use in propagation process.
     *
     * @return
     */
    private boolean hasSchemas() {
        return this.schemas != null;
    }


    /**
     * Check if schema must be ignored.
     *
     * @param schema
     * @return
     */
    private boolean schemaMustBeIgnored(String schema) {
        if (schema == null) {
            return true;
        }
        return Arrays.asList(DEFAULT_IGNORED_SCHEMAS).contains(schema);
    }

    /**
     * Update property.
     *
     * @param xpath
     * @param value
     * @param doc
     * @return
     * @throws Exception on error
     */
    private DocumentModel updateProperty(DocumentModel doc, String xpath, Object value) throws Exception {
        Property p = doc.getProperty(xpath);
        p.setValue(value);
        return doc;
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
