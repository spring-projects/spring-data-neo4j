package org.springframework.datastore.graph.api;

import org.neo4j.graphdb.PropertyContainer;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public interface GraphBacked<STATE> {
    void setUnderlyingState(STATE state);
    STATE getUnderlyingState();
}
