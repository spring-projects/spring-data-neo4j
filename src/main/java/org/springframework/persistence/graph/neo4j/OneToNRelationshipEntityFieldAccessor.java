package org.springframework.persistence.graph.neo4j;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.persistence.support.EntityInstantiator;

public class OneToNRelationshipEntityFieldAccessor implements FieldAccessor {

	private final RelationshipType type;
	private final Direction direction;
	private final Class<? extends RelationshipBacked> elementClass;
	private final EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;

	public OneToNRelationshipEntityFieldAccessor(RelationshipType type, Direction direction, Class<? extends RelationshipBacked> elementClass, EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.type = type;
		this.direction = direction;
		this.elementClass = elementClass;
		this.relationshipEntityInstantiator = relationshipEntityInstantiator;
	}

	@Override
	public Object apply(NodeBacked entity, Object newVal) {
		throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	}

	@Override
	public Object readObject(NodeBacked entity) {
		Set<RelationshipBacked> result = new HashSet<RelationshipBacked>();
		for (Relationship rel : entity.getUnderlyingNode().getRelationships(type, direction)) {
			result.add(relationshipEntityInstantiator.createEntityFromState(rel, elementClass));
		}
		return new ManagedFieldAccessorSet<RelationshipBacked>(entity, result, this);
	}
}
