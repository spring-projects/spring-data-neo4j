package org.springframework.persistence.graph.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
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
    EntityInstantiator<NodeBacked, Node> nodeInstantiator;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map map) {
        return new Neo4jEntityManagerFactory(graphDatabaseService,nodeInstantiator);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        return new Neo4jEntityManagerFactory(graphDatabaseService,nodeInstantiator);
    }
}
