package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.GraphBacked;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public interface EntityStateAccessors<ENTITY extends GraphBacked<STATE>,STATE> {
    ENTITY getEntity();

    void setUnderlyingState(STATE state);

    Object getValue(Field field);
    boolean isWritable(Field field);

    Object setValue(Field field, Object newVal);

    void createAndAssignState();
}
