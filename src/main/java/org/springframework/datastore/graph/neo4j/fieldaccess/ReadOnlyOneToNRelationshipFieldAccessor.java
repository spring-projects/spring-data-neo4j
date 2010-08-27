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

public class ReadOnlyOneToNRelationshipFieldAccessor implements FieldAccessor {

	private final RelationshipType type;
	private final Direction direction;
	private final Class<? extends NodeBacked> relatedType;
	private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

	public ReadOnlyOneToNRelationshipFieldAccessor(RelationshipType type, Direction direction, Class<? extends NodeBacked> elementClass, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.type = type;
		this.direction = direction;
		this.relatedType = elementClass;
		this.graphEntityInstantiator = graphEntityInstantiator;
	}

	public Object apply(final NodeBacked entity, final Object newVal) {
		throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	}
	
	@Override
	public Object readObject(NodeBacked entity) {
		Node entityNode = entity.getUnderlyingNode();
		if (entityNode == null) {
			throw new IllegalStateException("Entity must have a backing Node");
		}
		Set<NodeBacked> result = new HashSet<NodeBacked>();
		for (Relationship rel : entityNode.getRelationships(type, direction)) {
			result.add(graphEntityInstantiator.createEntityFromState(rel.getOtherNode(entityNode), relatedType));
		}
		return new ManagedFieldAccessorSet<NodeBacked>(entity, result, this);
	}
	
}
