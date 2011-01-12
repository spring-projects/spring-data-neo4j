package org.springframework.data.graph.neo4j.finder;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;

/**
 * @author mh
 * @since 12.01.11
 */
public interface Finder<S extends PropertyContainer,T extends GraphBacked<S>> {
    long count();

    Iterable<T> findAll();

    T findById(long id);

    T findByPropertyValue(String indexName, String property, Object value);

    Iterable<T> findAllByPropertyValue(String indexName, String property, Object value);

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
