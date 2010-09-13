package org.springframework.datastore.graph.neo4j.support;

import org.neo4j.graphdb.*;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.util.GraphDatabaseUtil;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.NodeTypeStrategy;

import java.util.Collections;

/**
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class SubReferenceNodeTypeStrategy implements NodeTypeStrategy {
    public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");

    public final static String SUBREFERENCE_NODE_COUNTER_KEY = "count";
    public static final String SUBREF_PREFIX = "SUBREF_";

    private final GraphDatabaseContext graphDatabaseContext;

    public SubReferenceNodeTypeStrategy(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public void postEntityCreation(final NodeBacked entity) {
        final Node subReference = obtainSubreferenceNode(entity.getClass());
        entity.getUnderlyingNode().createRelationshipTo(subReference, INSTANCE_OF_RELATIONSHIP_TYPE);
        GraphDatabaseUtil.incrementAndGetCounter(subReference, SUBREFERENCE_NODE_COUNTER_KEY);
    }

    @Override
    public long count(final Class<? extends NodeBacked> entityClass) {
        final Node subrefNode = findSubreferenceNode(entityClass);
        if (subrefNode == null) return 0;
        return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
    }

    @Override
    public <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz) {
        final Node subrefNode = findSubreferenceNode(clazz);
        if (subrefNode == null) return Collections.emptyList();
        return new IterableWrapper<T, Relationship>(subrefNode.getRelationships(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            @Override
            protected T underlyingObjectToObject(final Relationship rel) {
                final Node node = rel.getStartNode();
                return graphDatabaseContext.createEntityFromState(node, clazz);
            }
        };
    }


    public Node obtainSubreferenceNode(final Class<? extends NodeBacked> entityClass) {
        return graphDatabaseContext.getOrCreateSubReferenceNode(subRefRelationshipType(entityClass));
    }

    public Node findSubreferenceNode(final Class<? extends NodeBacked> entityClass) {
        final Relationship subrefRelationship = graphDatabaseContext.getReferenceNode().getSingleRelationship(subRefRelationshipType(entityClass), Direction.OUTGOING);
        return subrefRelationship != null ? subrefRelationship.getEndNode() : null;
    }

    private DynamicRelationshipType subRefRelationshipType(Class<?> clazz) {
        return DynamicRelationshipType.withName(SUBREF_PREFIX + clazz.getName());
    }
}
