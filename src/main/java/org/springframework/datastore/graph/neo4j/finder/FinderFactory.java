package org.springframework.datastore.graph.neo4j.finder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.persistence.support.EntityInstantiator;

public class FinderFactory {

    private final GraphDatabaseService graphDatabaseService;
    private final EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
    private final IndexService indexService;

    public FinderFactory(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator, IndexService indexService) {
        this.graphDatabaseService = graphDatabaseService;
        this.graphEntityInstantiator = graphEntityInstantiator;
        this.indexService = indexService;
    }

    public <T extends NodeBacked> Finder<T> getFinderForClass(Class<T> clazz) {
        return new Finder<T>(clazz, graphDatabaseService, graphEntityInstantiator, indexService);
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
