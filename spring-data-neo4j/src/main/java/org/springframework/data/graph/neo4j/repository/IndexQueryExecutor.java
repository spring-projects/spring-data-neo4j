package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;

/**
 * @author mh
 * @since 29.03.11
 */
public interface IndexQueryExecutor<S extends PropertyContainer,T extends GraphBacked<S>> {
    T findByPropertyValue(String indexName, String property, Object value);

    Iterable<T> findAllByPropertyValue(String indexName, String property, Object value);

    Iterable<T> findAllByQuery(String indexName, String key, Object query);

    Iterable<T> findAllByRange(String indexName, String property, Number from, Number to);
}
