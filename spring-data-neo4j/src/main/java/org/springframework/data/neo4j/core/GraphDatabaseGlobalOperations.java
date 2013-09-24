package org.springframework.data.neo4j.core;

import org.neo4j.graphdb.*;

/**
 * Defines the list of global graph operations which can be used
 * within SDN. (At present restricted to those providing Label
 * based functionality)
 *
 * @author Nicki Watt
 * @since 24-09-2013
 */
public interface GraphDatabaseGlobalOperations {

    public ResourceIterable<Label> getAllLabels();

    public ResourceIterable<Node> getAllNodesWithLabel(Label label);

}
