package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class OneToNRelationshipEntityFieldAccessor extends AbstractRelationshipFieldAccessor<NodeBacked, Node, RelationshipBacked,Relationship> {

	public OneToNRelationshipEntityFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends RelationshipBacked> elementClass, final EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
        super(elementClass,relationshipEntityInstantiator,direction,type);
	}

	@Override
	public Object setValue(final NodeBacked entity, final Object newVal) {
		throw new InvalidDataAccessApiUsageException("Cannot set read-only relationship entity field.");
	}

	@Override
	public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        final Set<RelationshipBacked> result = createEntitySetFromRelationships(entity);
		return new ManagedFieldAccessorSet<NodeBacked, RelationshipBacked>(entity, result, this);
	}

    private Set<RelationshipBacked> createEntitySetFromRelationships(final NodeBacked entity) {
        final Set<RelationshipBacked> result = new HashSet<RelationshipBacked>();
        for (final Relationship rel : getStatesFromEntity(entity)) {
            result.add(graphEntityInstantiator.createEntityFromState(rel, relatedType));
        }
        return result;
    }

    @Override
    protected Iterable<Relationship> getStatesFromEntity(NodeBacked entity) {
        return entity.getUnderlyingNode().getRelationships(type, direction);
    }

    @Override
    protected Relationship obtainSingleRelationship(Node start, Relationship end) {
        return null;
    }

    @Override
    protected Node getState(NodeBacked nodeBacked) {
        return nodeBacked.getUnderlyingNode();
    }
}
