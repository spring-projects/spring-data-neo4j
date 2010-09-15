package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public interface EntityStateAccessors<ENTITY extends NodeBacked> {
    ENTITY getEntity();

    GraphDatabaseContext getGraphDatabaseContext();

    void setNode(Node node);

    Object getValue(Field field);

    Object setValue(Field field, Object newVal);

    void createAndAssignNode();
}
