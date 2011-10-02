package org.springframework.data.neo4j.config;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.support.EntityInstantiator;
import org.springframework.data.neo4j.support.node.CrossStoreNodeEntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.node.PartialNodeEntityInstantiator;
import org.springframework.data.neo4j.transaction.ChainedTransactionManager;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.persistence.EntityManagerFactory;

/**
 * @author mh
 * @since 30.09.11
 */
@Configuration
public class CrossStoreNeo4jConfiguration extends Neo4jAspectConfiguration {

    private EntityManagerFactory entityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Qualifier("&entityManagerFactory")
    @Autowired(required = false)
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public boolean isUsingCrossStorePersistence() {
        return entityManagerFactory != null;
    }

    @Bean
	protected EntityInstantiator<Node> graphEntityInstantiator() {
		if (isUsingCrossStorePersistence()) {
			return new PartialNodeEntityInstantiator(new NodeEntityInstantiator(mappingContext), entityManagerFactory);
		} else {
			return new NodeEntityInstantiator(mappingContext);
		}
	}

    @Bean
	public PlatformTransactionManager transactionManager() {
		if (isUsingCrossStorePersistence()) {
			JpaTransactionManager jpaTm = new JpaTransactionManager(getEntityManagerFactory());
            JtaTransactionManager jtaTm = createJtaTransactionManager();
			return new ChainedTransactionManager(jpaTm, jtaTm);
		}
		else {
            return createJtaTransactionManager();
		}
	}

    @Bean
    public NodeEntityStateFactory nodeEntityStateFactory() throws Exception {

        CrossStoreNodeEntityStateFactory entityStateFactory = new CrossStoreNodeEntityStateFactory();
        entityStateFactory.setGraphDatabaseContext(graphDatabaseContext());
        entityStateFactory.setEntityManagerFactory(entityManagerFactory);
        entityStateFactory.setMappingContext(mappingContext());
        entityStateFactory.setNodeDelegatingFieldAccessorFactory(nodeDelegatingFieldAccessorFactory());
        return entityStateFactory;
    }
}
