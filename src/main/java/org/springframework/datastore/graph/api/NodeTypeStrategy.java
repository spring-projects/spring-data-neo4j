package org.springframework.datastore.graph.api;

import org.springframework.datastore.graph.api.NodeBacked;

/**
* @author Michael Hunger
* @since 13.09.2010
*/
public interface NodeTypeStrategy {
    void postEntityCreation(NodeBacked entity);

    <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz);
    long count(final Class<? extends NodeBacked> entityClass);
}
