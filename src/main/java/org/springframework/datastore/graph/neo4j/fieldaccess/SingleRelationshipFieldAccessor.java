package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.GraphEntityRelationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Set;

public class SingleRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor<NodeBacked> {
    public SingleRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> clazz, final GraphDatabaseContext graphDatabaseContext) {
        super(clazz, graphDatabaseContext, direction, type);
    }

	@Override
    public Object setValue(final NodeBacked entity, final Object newVal) {
        final Node node=checkUnderlyingNode(entity);
        if (newVal == null) {
            removeMissingRelationships(node, Collections.<Node>emptySet());
            return null;
        }
        final Set<Node> target=checkTargetIsSetOfNodebacked(Collections.singleton(newVal));
        checkNoCircularReference(node,target);
        removeMissingRelationships(node, target);
		createAddedRelationships(node,target);
        return newVal;
	}

    @Override
	public Object getValue(final NodeBacked entity) {
        checkUnderlyingNode(entity);
        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
        return result.isEmpty() ? null : result.iterator().next();
	}

    public static FieldAccessorFactory<NodeBacked> factory() {
        return new RelationshipFieldAccessorFactory() {
            @Override
            public boolean accept(final Field f) {
                return NodeBacked.class.isAssignableFrom(f.getType());
            }

            @Override
            public FieldAccessor<NodeBacked, ?> forField(final Field field) {
                final GraphEntityRelationship relAnnotation = getRelationshipAnnotation(field);
                if (relAnnotation == null)
                    return new SingleRelationshipFieldAccessor(typeFrom(field), Direction.OUTGOING, targetFrom(field), graphDatabaseContext);
                return new SingleRelationshipFieldAccessor(typeFrom(relAnnotation), dirFrom(relAnnotation), targetFrom(field), graphDatabaseContext);
            }
        };
    }

}