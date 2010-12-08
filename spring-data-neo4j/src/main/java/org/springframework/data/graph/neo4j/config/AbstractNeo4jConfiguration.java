/*
 * Copyright 2002-2010 the original author or authors.
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
import org.neo4j.index.lucene.LuceneIndexService;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
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
import org.springframework.data.graph.neo4j.support.relationship.ConstructorBypassingGraphRelationshipInstantiator;
import org.springframework.data.graph.neo4j.support.relationship.Neo4jRelationshipBacking;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public abstract class AbstractNeo4jConfiguration {

	@Bean(destroyMethod="shutdown")
	public abstract GraphDatabaseService graphDatabaseService();
	
	@Bean(destroyMethod="shutdown")
	public LuceneIndexService indexService() {
		return new LuceneIndexService(graphDatabaseService());
	}

	@Bean
	public GraphDatabaseContext graphDatabaseContext() throws Exception {
		GraphDatabaseContext gdc = new GraphDatabaseContext();
		gdc.setGraphDatabaseService(graphDatabaseService());
		gdc.setIndexService(indexService());
		gdc.setRelationshipEntityInstantiator(new ConstructorBypassingGraphRelationshipInstantiator());
		gdc.setGraphEntityInstantiator(new Neo4jConstructorGraphEntityInstantiator());
		gdc.setConversionService(new Neo4jConversionServiceFactoryBean().getObject());
		gdc.setNodeTypeStrategy(new SubReferenceNodeTypeStrategy(gdc));
		return gdc;
	}

	@Bean
	public FinderFactory finderFactory() throws Exception {
		return new FinderFactory(graphDatabaseContext());
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
	public Neo4jNodeBacking neo4jNeo4jNodeBacking(GraphDatabaseContext graphDatabaseContext, FinderFactory finderFactory) {
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
	public PlatformTransactionManager transactionManager(GraphDatabaseService graphDatabaseService) {
		PlatformTransactionManager transactionManager = new JtaTransactionManager();
		((JtaTransactionManager)transactionManager).setTransactionManager(new SpringTransactionManager(graphDatabaseService));
		((JtaTransactionManager)transactionManager).setUserTransaction(new UserTransactionImpl(graphDatabaseService));
		return transactionManager;
	}
}
