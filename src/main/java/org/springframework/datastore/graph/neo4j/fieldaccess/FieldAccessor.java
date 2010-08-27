package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

public interface FieldAccessor {

	// Set entity field to newVal
	Object apply(NodeBacked entity, Object newVal);

	// Read object from entity field
	Object readObject(NodeBacked entity);

}
