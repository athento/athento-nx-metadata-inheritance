#athento-nx-metadata-inheritance

#Synopsis

This plugin allows metadata inheritance from a parent documentary type to all children documentary types which contains. For that, parents and children documentary types must share at least one schema. When a child documentary type is created, all metadatas which are in the common schemas are inherited from the parent to the child. If a parent documentary type is modified and metadatas of the common schemes have been changed, inherited metadatas when the children were created are updated.

It is neccesary to perform an event that will be executed at the time when a child documentary type was created. The xml code implemented for this is as follows:

	<extension target="org.nuxeo.ecm.core.operation.OperationServiceComponent" point="event-handlers">
    		<handler chainId="Chain_Inherit_Metadatas">
      			<event>documentCreated</event>
      			<filters>
        			<doctype>Child</doctype>
      			</filters>
    		</handler>
	</extension>

In this case, this event is executed when a documentary type called "Child" is created. You can create other child documentary type and change "Child" to the name of the new documentary type implemented.

To update inherited metadatas of child documentary types, it is neccesary to perform another event which will be run when a parent documentary type is modified. The xml code implemented to accomplish this is:

	<extension target="org.nuxeo.ecm.core.operation.OperationServiceComponent" point="event-handlers">
		<handler chainId="Chain_Inherit_Metadatas_From_Parent">
      			<event>documentModified</event>
      			<filters>
        			<doctype>Parent</doctype>
      			</filters>
    		</handler>
  	</extension>

This event is executed when a documentary type called "Parent" is modified. You can create other parent documentary type and change "Parent" to the name of the new documentary type implemented.

#Installation

You just have to compile the pom.xml using Maven and deploy the plugin in. To do this, you must use the following script:

	cd athento-nx-metadata-inheritance-master
	mvn clean install
	cp target/inheritMetadata-*.jar $NUXEO_HOME/nxserver/plugins

And then, restart your nuxeo server and enjoy.







