package org.springframework.datastore.graph.neo4j.finder;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.EntityInstantiator;

public class FinderFactory {

    private final GraphDatabaseContext graphDatabaseContext;

    public FinderFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public <T extends NodeBacked> Finder<T> getFinderForClass(Class<T> clazz) {
        return new Finder<T>(clazz, graphDatabaseContext);
    }
}
