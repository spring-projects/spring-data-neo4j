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
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.graph.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.graph.neo4j.fieldaccess.NodeEntityStateFactory;
import org.springframework.data.graph.neo4j.fieldaccess.RelationshipEntityStateFactory;
import org.springframework.data.graph.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.TypeRepresentationStrategyFactory;
import org.springframework.data.graph.neo4j.support.node.Neo4jConstructorGraphEntityInstantiator;
import org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking;
import org.springframework.data.graph.neo4j.support.node.PartialNeo4jEntityInstantiator;
import org.springframework.data.graph.neo4j.support.relationship.ConstructorBypassingGraphRelationshipInstantiator;
import org.springframework.data.graph.neo4j.support.relationship.Neo4jRelationshipBacking;
import org.springframework.data.graph.neo4j.transaction.ChainedTransactionManager;
import org.springframework.data.persistence.EntityInstantiator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.validation.Validator;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public class Neo4jConfiguration {
    private GraphDatabaseService graphDatabaseService;

    @Autowired(required = false)
    private Validator validator;

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

    @Qualifier("&entityManagerFactory")
    @Autowired(required = false)
    public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public boolean isUsingCrossStorePersistence() {
        return entityManagerFactory != null;
    }

	@Bean
	public GraphDatabaseContext graphDatabaseContext() throws Exception {
        EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator = graphRelationshipInstantiator();
        EntityInstantiator<NodeBacked, Node> graphEntityInstantiator = graphEntityInstantiator();

        TypeRepresentationStrategyFactory typeRepresentationStrategyFactory =
                new TypeRepresentationStrategyFactory(graphDatabaseService, graphEntityInstantiator, relationshipEntityInstantiator);

        GraphDatabaseContext gdc = new GraphDatabaseContext();
        gdc.setGraphDatabaseService(getGraphDatabaseService());
        gdc.setConversionService(conversionService());
        gdc.setNodeTypeRepresentationStrategy(typeRepresentationStrategyFactory.getNodeTypeRepresentationStrategy());
        gdc.setRelationshipTypeRepresentationStrategy(typeRepresentationStrategyFactory.getRelationshipTypeRepresentationStrategy());
        if (validator!=null) {
            gdc.setValidator(validator);
        }
		return gdc;
	}

    @Bean
    protected ConversionService conversionService() throws Exception {
        return new Neo4jConversionServiceFactoryBean().getObject();
    }

    @Bean
    protected ConstructorBypassingGraphRelationshipInstantiator graphRelationshipInstantiator() {
        return new ConstructorBypassingGraphRelationshipInstantiator();
    }

    @Bean
	protected EntityInstantiator<NodeBacked, Node> graphEntityInstantiator() {
		if (isUsingCrossStorePersistence()) {
			return new PartialNeo4jEntityInstantiator(new Neo4jConstructorGraphEntityInstantiator(), entityManagerFactory);
		} else {
			return new Neo4jConstructorGraphEntityInstantiator();
		}
	}

	@Bean
	public DirectGraphRepositoryFactory finderFactory(GraphDatabaseContext graphDatabaseContext) throws Exception {
		return new DirectGraphRepositoryFactory(graphDatabaseContext);
	}
	
	@Bean
	public Neo4jRelationshipBacking neo4jRelationshipBacking(GraphDatabaseContext graphDatabaseContext, DirectGraphRepositoryFactory graphRepositoryFactory) {
		Neo4jRelationshipBacking aspect = Neo4jRelationshipBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext);
		RelationshipEntityStateFactory entityStateFactory = new RelationshipEntityStateFactory();
		entityStateFactory.setGraphDatabaseContext(graphDatabaseContext);
		entityStateFactory.setGraphRepositoryFactory(graphRepositoryFactory);
		aspect.setRelationshipEntityStateFactory(entityStateFactory);
		return aspect;
	}

	@Bean
	public Neo4jNodeBacking neo4jNodeBacking(GraphDatabaseContext graphDatabaseContext, DirectGraphRepositoryFactory graphRepositoryFactory) {
		Neo4jNodeBacking aspect = Neo4jNodeBacking.aspectOf();
		aspect.setGraphDatabaseContext(graphDatabaseContext);
		NodeEntityStateFactory entityStateFactory = new NodeEntityStateFactory();
		entityStateFactory.setGraphDatabaseContext(graphDatabaseContext);
		entityStateFactory.setGraphRepositoryFactory(graphRepositoryFactory);
		entityStateFactory.setEntityManagerFactory(entityManagerFactory);
		entityStateFactory.setNodeDelegatingFieldAccessorFactory(
				new NodeDelegatingFieldAccessorFactory(graphDatabaseContext, graphRepositoryFactory));
		aspect.setNodeEntityStateFactory(entityStateFactory);
		return aspect;
	}
	
	@Bean
	public PlatformTransactionManager transactionManager() {
		if (isUsingCrossStorePersistence()) {
			JpaTransactionManager jpaTm = new JpaTransactionManager(getEntityManagerFactory());
			JtaTransactionManager jtaTm = new JtaTransactionManager();
			jtaTm.setTransactionManager(new SpringTransactionManager(getGraphDatabaseService()));
			jtaTm.setUserTransaction(new UserTransactionImpl(getGraphDatabaseService()));
			return new ChainedTransactionManager(jpaTm, jtaTm);
		}
		else {
			PlatformTransactionManager transactionManager = new JtaTransactionManager();
			((JtaTransactionManager)transactionManager).setTransactionManager(new SpringTransactionManager(getGraphDatabaseService()));
			((JtaTransactionManager)transactionManager).setUserTransaction(new UserTransactionImpl(getGraphDatabaseService()));
			return transactionManager;
		}
	}

    @Bean
    public ConfigurationCheck configurationCheck() throws Exception {
        return new ConfigurationCheck(graphDatabaseContext(),transactionManager());
    }
}
