package org.springframework.datastore.graph.neo4j.fieldaccess;

import java.util.Collections;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

public class OneToNRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor<NodeBacked> {

	public OneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        super(elementClass, graphEntityInstantiator, direction, type);
	}

	public Object setValue(final NodeBacked entity, final Object newVal) {
        final Node node = checkUnderlyingNode(entity);
        if (newVal==null) {
            removeMissingRelationships(node, Collections.<Node>emptySet());
            return null;
        }
        final Set<Node> targetNodes = checkTargetIsSetOfNodebacked(newVal);
        checkNoCircularReference(node,targetNodes);
        removeMissingRelationships(node, targetNodes);
		createAddedRelationships(node,targetNodes);
        return createManagedSet(entity, (Set<NodeBacked>)newVal);
	}

    @Override
	public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
		return createManagedSet(entity, result);
	}
}

	