/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.cross_store.config;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.aspects.config.Neo4jAspectConfiguration;
import org.springframework.data.neo4j.config.JtaTransactionManagerFactoryBean;
import org.springframework.data.neo4j.cross_store.support.node.CrossStoreNodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.cross_store.support.node.CrossStoreNodeEntityInstantiator;
import org.springframework.data.neo4j.cross_store.support.node.CrossStoreNodeEntityStateFactory;
import org.springframework.data.neo4j.fieldaccess.FieldAccessorFactoryFactory;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.transaction.ChainedTransactionManager;
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
	protected EntityInstantiator<Node> graphEntityInstantiator() throws Exception {
		if (isUsingCrossStorePersistence()) {
    			return new CrossStoreNodeEntityInstantiator(new NodeEntityInstantiator(entityStateHandler()), entityManagerFactory);
		} else {
			return new NodeEntityInstantiator(entityStateHandler());
		}
	}

    @Bean
	public PlatformTransactionManager neo4jTransactionManager() throws Exception {
        JtaTransactionManager jtaTm = new JtaTransactionManagerFactoryBean( getGraphDatabaseService() ).getObject();

		if (isUsingCrossStorePersistence()) {
			JpaTransactionManager jpaTm = new JpaTransactionManager(getEntityManagerFactory());
			return new ChainedTransactionManager(jpaTm, jtaTm);
		}
		else {
            return jtaTm;
		}
	}

    public FieldAccessorFactoryFactory crossStoreNodeDelegatingFieldAccessorFactory() throws Exception {
        return new CrossStoreNodeDelegatingFieldAccessorFactory.Factory();
    }

    @Bean
    public NodeEntityStateFactory nodeEntityStateFactory() throws Exception {
        return new CrossStoreNodeEntityStateFactory(neo4jMappingContext(), nodeDelegatingFieldAccessorFactory(), crossStoreNodeDelegatingFieldAccessorFactory(),entityManagerFactory);
    }

}
