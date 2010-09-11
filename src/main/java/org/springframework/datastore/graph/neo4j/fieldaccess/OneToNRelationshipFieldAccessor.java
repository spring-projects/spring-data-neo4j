package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.util.Collections;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class OneToNRelationshipFieldAccessor extends AbstractFieldAccessor {

	public OneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        super(elementClass, graphEntityInstantiator, direction, type);
	}

	public Object setValue(final NodeBacked entity, final Object newVal) {
        checkUnderlyingNode(entity);
        if (newVal==null) {
            removeMissingRelationships(entity, Collections.<NodeBacked>emptySet());
            return null;
        }
        final Set<NodeBacked> target = checkTargetIsSetOfNodebacked(newVal);
        checkNoCircularReference(entity,target);
        removeMissingRelationships(entity, target);
		createNewRelationshipsFrom(entity,target);
        return createManagedSet(entity, target);
	}

    @Override
	public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
		return createManagedSet(entity, result);
	}
}

	