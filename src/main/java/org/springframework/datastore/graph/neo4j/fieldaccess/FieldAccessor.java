package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;

public interface FieldAccessor<ENTITY, TARGET> {

	Object setValue(ENTITY entity, Object newVal);

	Object getValue(ENTITY entity);

}
