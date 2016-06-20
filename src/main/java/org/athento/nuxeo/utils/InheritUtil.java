package org.athento.nuxeo.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.model.Property;

import java.util.Arrays;
import java.util.Map;

/**
 * Created by victorsanchez on 3/6/16.
 */
public final class InheritUtil {

    /** Extended config path. */
    public static final String CONFIG_PATH = "/ExtendedConfig";

    /** Log. */
    private static final Log LOG = LogFactory.getLog(InheritUtil.class);

    /**
     * IGNORED SCHEMAS.
     */
    public static String[] DEFAULT_IGNORED_SCHEMAS = { "dublincore", "common", "uid", "file", "files" };

    /**
     * Propagate schemas.
     *
     * @param origin
     * @param destiny
     * @param schemas
     */
    public static void propagateSchemas(CoreSession session, DocumentModel origin, DocumentModel destiny, String [] schemas, String [] ignoredMetadatas) {
        // Propagate schemas
        for (String schema : schemas) {
            if (documentsHaveSchema(origin, destiny, schema)) {
                if (isValidToPropagateSchema(schema, schemas)) {
                    // Get properties from valid schema to propagate to child
                    Map<String, Object> properties = origin.getProperties(schema);
                    for (Map.Entry<String, Object> entry : properties.entrySet()) {
                        String metadata = entry.getKey();
                        if (!metadataMustBeIgnored(metadata, ignoredMetadatas)) {
                            Object value = origin.getPropertyValue(metadata);
                            // From #AT-921
                            if (allowToSaveValue(session, metadata, value)) {
                                // Update property of destiny document
                                updateProperty(destiny, metadata, value);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if null value of metadata must be propagated using Extendedconfig property.
     * <i>From #AT-921</i>
     *
     * @param session
     * @param value
     * @return
     */
    private static boolean allowToSaveValue(CoreSession session, String metadata, Object value) {
        if (value != null && !"null".equals(value)) {
            return true;
        } else {
            return InheritUtil.readConfigValue(session, "metadataInheritanceConfig:propagateNullValues", false);
        }
    }

    /**
     * Read a extended config value.
     *
     * @param session
     * @param key
     * @param defaultValue
     * @return
     */
    public static <T> T readConfigValue(CoreSession session, String key, T defaultValue) {
        DocumentModel conf = session.getDocument(new PathRef(CONFIG_PATH));
        T value = (T) conf.getPropertyValue(key);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    /**
     * Check if origin and destiny documents have schema.
     *
     * @param origin
     * @param destiny
     * @param schema
     * @return
     */
    private static boolean documentsHaveSchema(DocumentModel origin, DocumentModel destiny, String schema) {
        return origin != null && destiny != null && origin.hasSchema(schema) && destiny.hasSchema(schema);
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
    public static DocumentModel updateProperty(DocumentModel doc, String xpath, Object value) {
        Property p = doc.getProperty(xpath);
        p.setValue(value);
        return doc;
    }

    /**
     * Check if parent document type schema is valid to propagate to child document.
     *
     * @param schema
     * @return
     */
    public static boolean isValidToPropagateSchema(String schema, String [] schemas) {
        boolean valid = false;
        if (!schemaMustBeIgnored(schema)) {
            if (schemas != null && schemas.length > 0) {
                for (String paramSchema : schemas) {
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
     * Check if schema must be ignored.
     *
     * @param schema
     * @return
     */
    public static boolean schemaMustBeIgnored(String schema) {
        if (schema == null) {
            return true;
        }
        return Arrays.asList(DEFAULT_IGNORED_SCHEMAS).contains(schema);
    }

    /**
     * Check metadata to be ignored.
     *
     * @param metadata
     * @return
     */
    public static boolean metadataMustBeIgnored(String metadata, String [] ignoredMetadatas) {
        boolean ignore = false;
        if (metadata == null) {
            ignore = true;
        }
        for (String ignoredMetadata : ignoredMetadatas) {
            if (metadata.equals(ignoredMetadata.trim())) {
                ignore = true;
                break;
            }
        }
        return ignore;
    }
}
