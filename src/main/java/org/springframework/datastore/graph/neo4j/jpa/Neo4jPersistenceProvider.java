package org.springframework.datastore.graph.neo4j.jpa;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.persistence.support.EntityInstantiator;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
// todo handle additinal info + database path
public class Neo4jPersistenceProvider implements PersistenceProvider {
    @Resource
    GraphDatabaseService graphDatabaseService;

    @Resource
    EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return new Neo4jEntityManagerFactory(graphDatabaseService,graphEntityInstantiator);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        return new Neo4jEntityManagerFactory(graphDatabaseService,graphEntityInstantiator);
    }
}
