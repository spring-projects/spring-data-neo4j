package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.persistence.support.EntityInstantiator;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
public class Neo4jEntityManagerFactory implements EntityManagerFactory {
    @Resource
    GraphDatabaseService graphDatabaseService;
    @Resource
    EntityInstantiator<NodeBacked, Node> nodeInstantiator;

    public Neo4jEntityManagerFactory() {
    }

    public Neo4jEntityManagerFactory(GraphDatabaseService graphDatabaseService, EntityInstantiator<NodeBacked, Node> nodeInstantiator) {
        this.graphDatabaseService = graphDatabaseService;
        this.nodeInstantiator = nodeInstantiator;
    }

    @Override
    public EntityManager createEntityManager() {
        return new Neo4jEntityManager(graphDatabaseService,nodeInstantiator);
    }

    /* TODO handle different directories for target datastore */
    @Override
    public EntityManager createEntityManager(Map map) {
        return new Neo4jEntityManager(graphDatabaseService,nodeInstantiator);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
