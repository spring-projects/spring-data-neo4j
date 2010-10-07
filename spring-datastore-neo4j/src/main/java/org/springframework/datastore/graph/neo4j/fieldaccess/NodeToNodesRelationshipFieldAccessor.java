package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class NodeToNodesRelationshipFieldAccessor<TARGET> extends AbstractNodeRelationshipFieldAccessor<NodeBacked, Node, TARGET, Node> {
    public NodeToNodesRelationshipFieldAccessor(final Class<? extends TARGET> clazz, final GraphDatabaseContext graphDatabaseContext, final Direction direction, final RelationshipType type) {
        super(clazz, graphDatabaseContext, direction, type);
    }

    @Override
    protected Relationship obtainSingleRelationship(final Node start, final Node end) {
        final Iterable<Relationship> existingRelationships = start.getRelationships(type, direction);
        for (final Relationship existingRelationship : existingRelationships) {
            if (existingRelationship!=null && existingRelationship.getOtherNode(start).equals(end)) return existingRelationship;
        }
        return start.createRelationshipTo(end, type);
    }

    @Override
    protected Iterable<Node> getStatesFromEntity(final NodeBacked entity) {
        final Node entityNode = getState(entity);
        final Set<Node> result = new HashSet<Node>();
        for (final Relationship rel : entityNode.getRelationships(type, direction)) {
            result.add(rel.getOtherNode(entityNode));
		}
        return result;
    }

    @Override
    protected Node getState(final NodeBacked entity) {
        return entity.getUnderlyingState();
    }
}
