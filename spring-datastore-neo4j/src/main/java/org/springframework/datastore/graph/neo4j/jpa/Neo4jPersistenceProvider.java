package org.springframework.datastore.graph.neo4j.jpa;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.index.IndexService;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.EntityInstantiator;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.LoadState;
import javax.persistence.spi.PersistenceProvider;
import javax.persistence.spi.PersistenceUnitInfo;
import javax.persistence.spi.ProviderUtil;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 23.08.2010
 */
// todo handle additinal info + database path
@Configurable
public class Neo4jPersistenceProvider implements PersistenceProvider {
    @Resource
    private GraphDatabaseContext graphDatabaseContext;

    @Override
    public EntityManagerFactory createEntityManagerFactory(String emName, Map params) {
        System.out.println("emName = " + emName);
        System.out.println("params = " + params);
        return new Neo4jEntityManagerFactory(graphDatabaseContext, null,params);
    }

    @Override
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map params) {
        System.out.println("info.getPersistenceProviderClassName() = " + info.getPersistenceProviderClassName());
        System.out.println("info.getManagedClassNames() = " + info.getManagedClassNames());
        System.out.println("info.getMappingFileNames() = " + info.getMappingFileNames());
        System.out.println("info.getTransactionType() = " + info.getTransactionType());
        System.out.println("info.getProperties() = " + info.getProperties());
        System.out.println("info.getPersistenceUnitName() = " + info.getPersistenceUnitName());
        System.out.println("params = " + params);
        return new Neo4jEntityManagerFactory(graphDatabaseContext,info,params);
    }

    @Override
    public ProviderUtil getProviderUtil() {
        return new ProviderUtil(){
            @Override
            public LoadState isLoadedWithoutReference(Object o, String s) {
                return LoadState.UNKNOWN;
            }

            @Override
            public LoadState isLoadedWithReference(Object o, String s) {
                return LoadState.UNKNOWN;
            }

            @Override
            public LoadState isLoaded(Object o) {
                return LoadState.UNKNOWN;
            }
        };
    }
}
