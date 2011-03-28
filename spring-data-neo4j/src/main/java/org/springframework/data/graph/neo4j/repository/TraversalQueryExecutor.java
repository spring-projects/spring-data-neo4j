package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 29.03.11
 */
public interface TraversalQueryExecutor<S extends PropertyContainer,T extends GraphBacked<S>> {
    /**
     * Traversal based finder that returns a lazy Iterable over the traversal results
     *
     * @param startNode            the node to start the traversal from
     * @param traversalDescription
     * @param <N>                  Start node entity type
     * @return Iterable over traversal result
     */
    <N extends NodeBacked> Iterable<T> findAllByTraversal(N startNode, TraversalDescription traversalDescription);
}
