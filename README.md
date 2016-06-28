#athento-nx-metadata-inheritance

#Synopsis

This plugin allows metadata inheritance from a parent documentary type to all children documentary types which contains. For that, parents and children documentary types must share at least one schema. When a child documentary type is created, all metadatas which are in the common schemas are inherited from the parent to the child. If a parent documentary type is modified and metadatas of the common schemes have been changed, inherited metadatas when the children were created are updated.

#Configuration

##Extended Config##
* ignoredMetadatas: it is a comma-separated value with metadata which it will be ignored.
* propagateNullValues: this value indicates to inheritance handler to propagate null values between document fields.
* enableSiblingInheritance: this value enables sibling inheritance. When an "inheritor" document is modified, the "inheritable" parent (inherit:parentId) and siblings are updated with his changes.

#Installation

You just have to compile the pom.xml using Maven and deploy the plugin in. To do this, you must use the following script:

	cd athento-nx-metadata-inheritance-master
	mvn clean install
	cp target/inheritMetadata-*.jar $NUXEO_HOME/nxserver/plugins

And then, restart your nuxeo server and enjoy.







