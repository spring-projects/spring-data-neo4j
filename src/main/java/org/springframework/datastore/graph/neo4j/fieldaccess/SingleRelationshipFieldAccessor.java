package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class SingleRelationshipFieldAccessor extends AbstractFieldAccessor {
    public SingleRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> clazz, final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        super(clazz, graphEntityInstantiator, direction, type);
    }

	@Override
    public Object setValue(final NodeBacked entity, final Object newVal) {
        checkUnderlyingNode(entity);
        if (newVal == null) {
            removeRelationships(entity);
            return null;
        }

        final NodeBacked target=checkTargetTypeNodebacked(newVal);
        if (isExistingRelationship(entity, target)) return target;
        checkCircularReference(entity, target);
        removeRelationships(entity);
        return createSingleRelationship(entity, target);
	}

    @Override
	public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        return createEntityFromRelationshipEndNode(entity.getUnderlyingNode());
	}
}