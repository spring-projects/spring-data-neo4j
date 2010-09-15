package org.springframework.datastore.graph.neo4j.fieldaccess;

public interface FieldAccessor<ENTITY, TARGET> {

	Object setValue(ENTITY entity, Object newVal);

	Object getValue(ENTITY entity);

    boolean isWriteable(ENTITY entity);
}
