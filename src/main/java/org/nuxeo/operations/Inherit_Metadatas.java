package org.nuxeo.operations;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.model.Property;
import java.util.Map;
import java.util.Set;

@Operation(id = Inherit_Metadatas.ID, category = Constants.CAT_FETCH, label = "Inherit_Metadatas", description = "Inherit metadatas from his parent document type")
public class Inherit_Metadatas {
	public static final String ID = "Inherit_Metadatas";
	
	@Context
	protected CoreSession session;
		
	@OperationMethod
	public void run(DocumentModel doc) throws Exception {
		
		if (doc == null) {
			throw new OperationException(
				"No DocumentModel received. Operation chain must inject a Document in Context");
		}
		
		DocumentRef refParent = doc.getParentRef();
		DocumentModel parent = session.getDocument(refParent);
		
		String[] parent_schemas = parent.getSchemas();
		String[] child_schemas = doc.getSchemas();
		
		String parentSchema, childSchema;
		String key;
		String value;
		
		Map<String, Object> properties;
		
		int size_parent_schemas = parent_schemas.length;
		int size_child_schemas = child_schemas.length;
		int properties_size = 0;
		
		for(int i=0; i<size_parent_schemas; i++){
			
			parentSchema = parent_schemas[i];
			
			for(int j=0; j<size_child_schemas;j++){
				
				childSchema = child_schemas[j];
				
				if(parentSchema.equals(childSchema) && !parentSchema.equals("dublincore") 
						&& !parentSchema.equals("common") && !parentSchema.equals("uid")
						&& !parentSchema.equals("file") && !parentSchema.equals("files")){
					
					properties = parent.getProperties(parentSchema);
					properties_size = properties.size();
					Set<String> keys = properties.keySet();
					Object[] arraykeys = keys.toArray();
					
					for(int k=0; k<properties_size; k++){
						
						key = arraykeys[k].toString();
						value = parent.getPropertyValue(key).toString();
						doc = updateProperty(key, value, doc);
						
					}
				}
			}
		}
		
		doc = session.saveDocument(doc);
        session.save();
	}
	
    private static DocumentModel updateProperty(String xpath, String value, DocumentModel doc) throws Exception {
        Property p = doc.getProperty(xpath);
        
        p.setValue(value);

        return doc;
    }
}
