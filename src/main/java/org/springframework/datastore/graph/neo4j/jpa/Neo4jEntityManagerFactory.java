package org.springframework.datastore.graph.neo4j.jpa;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
public class Neo4jEntityManagerFactory implements EntityManagerFactory {
    GraphDatabaseService graphDatabaseService;
    EntityInstantiator<NodeBacked, Node> nodeInstantiator;
    private PersistenceUnitInfo info;
    private Map params;
    private IndexService indexService;

    public Neo4jEntityManagerFactory(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> nodeInstantiator, IndexService indexService, PersistenceUnitInfo info, Map params) {
        this.graphDatabaseService = graphDatabaseService;
        this.nodeInstantiator = nodeInstantiator;
        this.indexService = indexService;
        this.info = info;
        this.params = params;
    }

    @Override
    public EntityManager createEntityManager() {
        return new Neo4jEntityManager(graphDatabaseService,nodeInstantiator,info,params, indexService);
    }

    /* TODO handle different directories for target datastore */
    @Override
    public EntityManager createEntityManager(Map map) {
        return new Neo4jEntityManager(graphDatabaseService,nodeInstantiator,info,params, indexService);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
