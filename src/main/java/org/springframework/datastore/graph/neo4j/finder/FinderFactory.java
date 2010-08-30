package org.springframework.datastore.graph.neo4j.finder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.persistence.support.EntityInstantiator;

public class FinderFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    public FinderFactory(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphEntityInstantiator = graphEntityInstantiator;
    }

    public <T extends NodeBacked> Finder<T> getFinderForClass(Class<T> clazz) {
        return new Finder<T>(clazz, graphDatabaseService, graphEntityInstantiator);
    }

    public Class<NodeBacked> getEntityClass(String shortName) {
        final String className = Neo4jHelper.getClassNameForShortName(graphDatabaseService, shortName);
        try {
            return (Class<NodeBacked>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Unable to find class for " + shortName);
        }
    }
}
