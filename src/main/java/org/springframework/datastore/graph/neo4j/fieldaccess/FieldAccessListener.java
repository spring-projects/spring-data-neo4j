package org.springframework.datastore.graph.neo4j.fieldaccess;

public interface FieldAccessListener<ENTITY, TARGET> {

	void valueChanged(ENTITY entity, Object oldVal, Object newVal);

}
