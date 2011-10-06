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

package org.springframework.data.neo4j.config;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.kernel.AbstractGraphDatabase;
import org.neo4j.kernel.impl.transaction.SpringTransactionManager;
import org.neo4j.kernel.impl.transaction.UserTransactionImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jNodeConverterImpl;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.EntityInstantiator;
import org.springframework.data.neo4j.support.EntityStateHandler;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.data.neo4j.template.Neo4jExceptionTranslator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

import javax.annotation.PostConstruct;
import javax.validation.Validator;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public abstract class Neo4jConfiguration {
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

    @Bean
	public GraphDatabaseContext graphDatabaseContext() throws Exception {
        EntityInstantiator<Relationship> relationshipEntityInstantiator = graphRelationshipInstantiator();
        EntityInstantiator<Node> graphEntityInstantiator = graphEntityInstantiator();

        TypeRepresentationStrategyFactory typeRepresentationStrategyFactory =
                new TypeRepresentationStrategyFactory(graphDatabaseService, graphEntityInstantiator, relationshipEntityInstantiator);

        GraphDatabaseContext gdc = new GraphDatabaseContext();
        gdc.setGraphDatabaseService(getGraphDatabaseService());
        gdc.setConversionService(conversionService());
        gdc.setMappingContext(mappingContext());
        gdc.setConverter(neo4jConverter());
        gdc.setEntityStateHandler(entityStateHandler());
        gdc.setNodeTypeRepresentationStrategy(typeRepresentationStrategyFactory.getNodeTypeRepresentationStrategy());
        gdc.setRelationshipTypeRepresentationStrategy(typeRepresentationStrategyFactory.getRelationshipTypeRepresentationStrategy());
        if (validator!=null) {
            gdc.setValidator(validator);
        }
		return gdc;
	}

    @Bean
    public EntityStateHandler entityStateHandler() {
        return new EntityStateHandler(mappingContext(),graphDatabaseService);
    }

    @Bean
    public Neo4jNodeConverterImpl neo4jConverter() throws Exception {
        return new Neo4jNodeConverterImpl();
    }

    @Bean
    protected ConversionService conversionService() throws Exception {
        return new Neo4jConversionServiceFactoryBean().getObject();
    }

    @Bean
    protected RelationshipEntityInstantiator graphRelationshipInstantiator() throws Exception {
        return new RelationshipEntityInstantiator(entityStateHandler());
    }

    @Bean
	protected EntityInstantiator<Node> graphEntityInstantiator() throws Exception {
	   return new NodeEntityInstantiator(entityStateHandler());
	}

	@Bean
	public DirectGraphRepositoryFactory directGraphRepositoryFactory() throws Exception {
		return new DirectGraphRepositoryFactory(graphDatabaseContext());
	}

    @Bean
    public RelationshipEntityStateFactory relationshipEntityStateFactory() throws Exception {
        RelationshipEntityStateFactory entityStateFactory = new RelationshipEntityStateFactory();
        entityStateFactory.setGraphDatabaseContext(graphDatabaseContext());
        entityStateFactory.setMappingContext(mappingContext());
        entityStateFactory.setRelationshipDelegatingFieldAccessorFactory(relationshipDelegatingFieldAccessorFactory());
        return entityStateFactory;
    }

    @Bean
    public Neo4jMappingContext mappingContext() {
        return new Neo4jMappingContext();
    }

    @PostConstruct
    public void setupContext() throws Exception {
        neo4jConverter().setNodeEntityStateFactory(nodeEntityStateFactory());
    }
    @Bean
    public NodeEntityStateFactory nodeEntityStateFactory() throws Exception {
        NodeEntityStateFactory entityStateFactory = new NodeEntityStateFactory();
        entityStateFactory.setGraphDatabaseContext(graphDatabaseContext());
        entityStateFactory.setMappingContext(mappingContext());
        entityStateFactory.setNodeDelegatingFieldAccessorFactory(nodeDelegatingFieldAccessorFactory());
        return entityStateFactory;
    }

    @Bean
    public DelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory() throws Exception {
        return new NodeDelegatingFieldAccessorFactory(graphDatabaseContext());
    }
    
    @Bean
    public DelegatingFieldAccessorFactory relationshipDelegatingFieldAccessorFactory() throws Exception {
        return new RelationshipDelegatingFieldAccessorFactory(graphDatabaseContext());
    }

    @Bean(name = {"neo4jTransactionManager","transactionManager"})
    @Qualifier("neo4jTransactionManager")
	public PlatformTransactionManager neo4jTransactionManager() {
        return createJtaTransactionManager();
	}

    protected JtaTransactionManager createJtaTransactionManager() {
        JtaTransactionManager jtaTm = new JtaTransactionManager();
        final GraphDatabaseService gds = getGraphDatabaseService();
        if (gds instanceof AbstractGraphDatabase) {
            jtaTm.setTransactionManager(new SpringTransactionManager(gds));
            jtaTm.setUserTransaction(new UserTransactionImpl(gds));
        } else {
            final NullTransactionManager tm = new NullTransactionManager();
            jtaTm.setTransactionManager(tm);
            jtaTm.setUserTransaction(new UserTransactionAdapter(tm));
        }
        return jtaTm;
    }

    @Bean
    public GraphDatabase graphDatabase() {
        return new DelegatingGraphDatabase(graphDatabaseService);
    }

    @Bean
    public ConfigurationCheck configurationCheck() throws Exception {
        return new ConfigurationCheck(graphDatabaseContext(),neo4jTransactionManager());
    }

    @Bean
    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
        return new Neo4jExceptionTranslator();
    }
}
