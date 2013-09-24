package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.*;
import org.neo4j.tooling.GlobalGraphOperations;
import org.springframework.data.neo4j.core.GraphDatabaseGlobalOperations;

/**
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class DelegatingGraphDatabaseGlobalOperations implements GraphDatabaseGlobalOperations {

    private GraphDatabaseService graphDatabaseService;
    private GlobalGraphOperations globalGraphOperations;

    public DelegatingGraphDatabaseGlobalOperations(GraphDatabaseService delegate) {
        this.graphDatabaseService = delegate;
    }

    private GlobalGraphOperations getGlobalGraphOperations() {
        if (globalGraphOperations == null) {
            globalGraphOperations = GlobalGraphOperations.at(graphDatabaseService);
        }
        return globalGraphOperations;
    }

    @Override
    public ResourceIterable<Label> getAllLabels() {
        return getGlobalGraphOperations().getAllLabels();
    }

    @Override
    public ResourceIterable<Node> getAllNodesWithLabel(Label label) {
        return getGlobalGraphOperations().getAllNodesWithLabel(label);
    }
}
