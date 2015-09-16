package org.nuxeo.operations;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Context;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.Property;

import java.util.Map;
import java.util.Set;

@Operation(id = Inherit_Metadatas_From_Parent.ID, category = Constants.CAT_FETCH, label = "Inherit_Metadatas_From_Parent", description = "Inherit metadatas from his parent document type")
public class Inherit_Metadatas_From_Parent {
	public static final String ID = "Inherit_Metadatas_From_Parent";
	
	@Context
	protected static CoreSession session;
		
	@OperationMethod
	public void run(DocumentModel doc) throws Exception {
		
		if (doc == null) {
			throw new OperationException(
				"No DocumentModel received. Operation chain must inject a Document in Context");
		}
		
		String path_Parent = doc.getPathAsString();
		String id_Parent = doc.getId();
		
		String[] parent_schemas = doc.getSchemas();
		String[] child_schemas;
		
		String parentSchema, childSchema;
		String key;
		String value;
		
		Map<String, Object> properties;
		
		StringBuilder myquery = new StringBuilder();
		
		myquery.append("SELECT * FROM Document WHERE ecm:mixinType != 'HiddenInNavigation' "
				+ "AND ecm:currentLifeCycleState != 'deleted' AND ecm:path STARTSWITH '" + path_Parent + "/'"
				+ "AND ecm:uuid != '"+ id_Parent + "'");
		
		String q = myquery.toString(); 
		DocumentModelList docList;
		DocumentModel child;
		docList = session.query(q);
		
		int number_Children = docList.size();
		
		int size_parent_schemas = parent_schemas.length;
		int size_child_schemas;
		int properties_size = 0;
		
		for(int children=0; children < number_Children; children++){
			child = docList.get(children);
			child_schemas = child.getSchemas();
			size_child_schemas = child_schemas.length;
			for(int i=0; i<size_parent_schemas; i++){
				
				parentSchema = parent_schemas[i];
				
				for(int j=0; j<size_child_schemas;j++){
					
					childSchema = child_schemas[j];
					
					if(parentSchema.equals(childSchema) && !parentSchema.equals("dublincore") 
							&& !parentSchema.equals("common") && !parentSchema.equals("uid")
							&& !parentSchema.equals("file") && !parentSchema.equals("files")){
						
						properties = doc.getProperties(parentSchema);
						properties_size = properties.size();
						Set<String> keys = properties.keySet();
						Object[] arraykeys = keys.toArray();
						
						for(int k=0; k<properties_size; k++){
							
							key = arraykeys[k].toString();
							value = doc.getPropertyValue(key).toString();
							child = updateProperty(key, value, child);
						}
					}
				}
			}
		}
	}
	
    private static DocumentModel updateProperty(String xpath, String value, DocumentModel doc) throws Exception {
        Property p = doc.getProperty(xpath);
        
        p.setValue(value);
        doc = session.saveDocument(doc);

        return doc;
    }
}