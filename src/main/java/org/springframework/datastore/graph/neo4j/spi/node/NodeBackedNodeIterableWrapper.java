package org.springframework.datastore.graph.neo4j.spi.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

/**
 * @author Michael Hunger
 * @since 14.09.2010
 */
public class NodeBackedNodeIterableWrapper extends IterableWrapper<NodeBacked, Node> {
    private final Class<? extends NodeBacked> targetType;
    private final GraphDatabaseContext graphDatabaseContext;

    public NodeBackedNodeIterableWrapper(Traverser traverser, Class<? extends NodeBacked> targetType, final GraphDatabaseContext graphDatabaseContext) {
        super(traverser.nodes());
        this.targetType = targetType;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    protected NodeBacked underlyingObjectToObject(Node node) {
        return graphDatabaseContext.createEntityFromState(node, targetType);
    }
}
