/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.config;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.graph.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.graph.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.graph.neo4j.fieldaccess.NodeEntityStateAccessorsFactory;
import org.springframework.data.graph.neo4j.fieldaccess.RelationshipEntityStateAccessorsFactory;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.SubReferenceNodeTypeStrategy;
import org.springframework.data.graph.neo4j.support.node.Neo4jConstructorGraphEntityInstantiator;
import org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking;
import org.springframework.data.graph.neo4j.support.node.PartialNeo4jEntityInstantiator;
import org.springframework.data.graph.neo4j.support.relationship.ConstructorBypassingGraphRelationshipInstantiator;
import org.springframework.data.graph.neo4j.support.relationship.Neo4jRelationshipBacking;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.persistence.transaction.NaiveDoubleTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.persistence.EntityManagerFactory;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public class Neo4jConfiguration {
    private GraphDatabaseService graphDatabaseService;

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    @Autowired
    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    private EntityManagerFactory entityManagerFactory;

    public EntityManagerFactory getEntityManagerFactory() {
        return entityManagerFactory;
    }

    @Autowired(required = false)
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public boolean isUsingCrossStorePersistence() {
        return entityManagerFactory!=null;
    }

	@Bean
	public GraphDatabaseContext graphDatabaseContext() throws Exception {
		GraphDatabaseContext gdc = new GraphDatabaseContext();
		gdc.setGraphDatabaseService(getGraphDatabaseService());
		gdc.setRelationshipEntityInstantiator(new ConstructorBypassingGraphRelationshipInstantiator());
		if (isUsingCrossStorePersistence()) {
			gdc.setGraphEntityInstantiator(new PartialNeo4jEntityInstantiator(new Neo4jConstructorGraphEntityInstantiator(), getEntityManagerFactory().createEntityManager()));
		}
		else {
			gdc.setGraphEntityInstantiator(new Neo4jConstructorGraphEntityInstantiator());
		}
		gdc.setConversionService(new Neo4jConversionServiceFactoryBean().getObject());
		gdc.setNodeTypeStrategy(new SubReferenceNodeTypeStrategy(gdc));
		return gdc;
	}

	@Bean
	public FinderFactory finderFactory(GraphDatabaseContext graphDatabaseContext) throws Exception {
		return new FinderFactory(graphDatabaseContext);
	}
	
	@Bean
	public Neo4jRelationshipBacking neo4jRelationshipBacking(GraphDatabaseContext graphDatabaseContext, FinderFactory finderFactory) {
		Neo4jRelationshipBacking aspect = Neo4jRelationshipBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext);
		RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory = new RelationshipEntityStateAccessorsFactory();
		entityStateAccessorsFactory.setGraphDatabaseContext(graphDatabaseContext);
		entityStateAccessorsFactory.setFinderFactory(finderFactory);
		aspect.setRelationshipEntityStateAccessorsFactory(entityStateAccessorsFactory);
		return aspect;
	}

	@Bean
	public Neo4jNodeBacking neo4jNodeBacking(GraphDatabaseContext graphDatabaseContext, FinderFactory finderFactory) {
		Neo4jNodeBacking aspect = Neo4jNodeBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext);
		NodeEntityStateAccessorsFactory entityStateAccessorsFactory = new NodeEntityStateAccessorsFactory();
		entityStateAccessorsFactory.setGraphDatabaseContext(graphDatabaseContext);
		entityStateAccessorsFactory.setFinderFactory(finderFactory);
		entityStateAccessorsFactory.setNodeDelegatingFieldAccessorFactory(
				new NodeDelegatingFieldAccessorFactory(graphDatabaseContext, finderFactory));
		aspect.setNodeEntityStateAccessorsFactory(entityStateAccessorsFactory);
		return aspect;
	}
	
	@Bean
	public PlatformTransactionManager transactionManager() {
		if (isUsingCrossStorePersistence()) {
			JpaTransactionManager jpaTm = new JpaTransactionManager(getEntityManagerFactory());
			JtaTransactionManager jtaTm = new JtaTransactionManager();
			jtaTm.setTransactionManager(new SpringTransactionManager(getGraphDatabaseService()));
			jtaTm.setUserTransaction(new UserTransactionImpl(getGraphDatabaseService()));
			return new NaiveDoubleTransactionManager(jpaTm, jtaTm);
		}
		else {
			PlatformTransactionManager transactionManager = new JtaTransactionManager();
			((JtaTransactionManager)transactionManager).setTransactionManager(new SpringTransactionManager(getGraphDatabaseService()));
			((JtaTransactionManager)transactionManager).setUserTransaction(new UserTransactionImpl(getGraphDatabaseService()));
			return transactionManager;
		}
	}
}
