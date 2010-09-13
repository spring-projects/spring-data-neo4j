package org.springframework.datastore.graph.neo4j.jpa;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.EntityInstantiator;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Collections;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
public class Neo4jEntityManagerFactory implements EntityManagerFactory {
    private PersistenceUnitInfo info;
    private Map params;
    private final GraphDatabaseContext graphDatabaseContext;

    public Neo4jEntityManagerFactory(GraphDatabaseContext graphDatabaseContext, PersistenceUnitInfo info, Map params) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.info = info;
        this.params = params;
    }

    @Override
    public EntityManager createEntityManager() {
        return new Neo4jEntityManager(graphDatabaseContext,info,params);
    }

    /* TODO handle different directories for target datastore */
    @Override
    public EntityManager createEntityManager(Map map) {
        return new Neo4jEntityManager(graphDatabaseContext,info,params);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return null;
    }

    @Override
    public Metamodel getMetamodel() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Cache getCache() {
        return null;
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return true;
    }
}
