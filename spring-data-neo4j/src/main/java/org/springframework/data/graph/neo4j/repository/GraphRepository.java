package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;

/**
 * @author mh
 * @since 12.01.11
 */
public interface GraphRepository<S extends PropertyContainer,T extends GraphBacked<S>> extends CRUDGraphRepository<S,T>, IndexQueryExecutor<S,T>, TraversalQueryExecutor<S,T> {

}
