package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.repository.support.EntityInformation;

import java.io.Serializable;

/**
 * @author mh
 * @since 28.03.11
 */
public interface GraphEntityInformation<S extends PropertyContainer, T extends GraphBacked<S>> extends EntityInformation<T, Long> {

    boolean isNodeEntity();
    boolean isPartialEntity();
}
