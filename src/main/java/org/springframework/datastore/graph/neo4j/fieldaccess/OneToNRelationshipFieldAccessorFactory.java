package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.GraphEntityRelationship;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

public class OneToNRelationshipFieldAccessorFactory extends RelationshipFieldAccessorFactory {
	@Override
	public boolean accept(final Field f) {
	    return Collection.class.isAssignableFrom(f.getType()) && hasValidRelationshipAnnotation(f);
	}

	@Override
	public FieldAccessor<NodeBacked, ?> forField(final Field field) {
	    final GraphEntityRelationship relAnnotation = getRelationshipAnnotation(field);
	    return new OneToNRelationshipFieldAccessor(typeFrom(relAnnotation), dirFrom(relAnnotation), targetFrom(relAnnotation), graphDatabaseContext);
	}

	public static class OneToNRelationshipFieldAccessor extends NodeToNodesRelationshipFieldAccessor<NodeBacked> {

	    public OneToNRelationshipFieldAccessor(final RelationshipType type, final Direction direction, final Class<? extends NodeBacked> elementClass, final GraphDatabaseContext graphDatabaseContext) {
	        super(elementClass, graphDatabaseContext, direction, type);
	    }

	    public Object setValue(final NodeBacked entity, final Object newVal) {
	        final Node node = checkUnderlyingNode(entity);
	        if (newVal == null) {
	            removeMissingRelationships(node, Collections.<Node>emptySet());
	            return null;
	        }
	        final Set<Node> targetNodes = checkTargetIsSetOfNodebacked(newVal);
	        checkNoCircularReference(node, targetNodes);
	        removeMissingRelationships(node, targetNodes);
	        createAddedRelationships(node, targetNodes);
	        return createManagedSet(entity, (Set<NodeBacked>) newVal);
	    }

	    @Override
	    public Object getValue(final NodeBacked entity) {
	        checkUnderlyingNode(entity);
	        final Set<NodeBacked> result = createEntitySetFromRelationshipEndNodes(entity);
	        return doReturn(createManagedSet(entity, result));
	    }

	}
}
