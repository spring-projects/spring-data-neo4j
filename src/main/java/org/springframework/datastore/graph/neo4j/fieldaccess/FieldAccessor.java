package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

public interface FieldAccessor {

	Object setValue(NodeBacked entity, Object newVal);

	Object getValue(NodeBacked entity);

}
