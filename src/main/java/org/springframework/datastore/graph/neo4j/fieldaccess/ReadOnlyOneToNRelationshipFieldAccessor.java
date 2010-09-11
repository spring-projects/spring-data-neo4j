package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class ReadOnlyOneToNRelationshipFieldAccessor extends OneToNRelationshipFieldAccessor {

	public ReadOnlyOneToNRelationshipFieldAccessor(RelationshipType type, Direction direction, Class<? extends NodeBacked> elementClass, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        super(type,direction,elementClass,graphEntityInstantiator);
	}

	public Object setValue(final NodeBacked entity, final Object newVal) {
		throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	}
}
