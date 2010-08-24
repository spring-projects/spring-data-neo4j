package org.springframework.persistence.graph.neo4j;

public interface FieldAccessor {

	// Set entity field to newVal
	Object apply(NodeBacked entity, Object newVal);

	// Read object from entity field
	Object readObject(NodeBacked entity);

}
