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

import static java.util.Arrays.*;

import javax.validation.Validator;

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
import org.springframework.data.convert.DefaultTypeMapper;
import org.springframework.data.convert.TypeMapper;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.fieldaccess.DelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.support.DelegatingGraphDatabase;
import org.springframework.data.neo4j.support.MappingInfrastructure;
import org.springframework.data.neo4j.support.Neo4jExceptionTranslator;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexProviderImpl;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.mapping.Neo4jEntityFetchHandler;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.mapping.SourceStateTransmitter;
import org.springframework.data.neo4j.support.mapping.TRSTypeAliasAccessor;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.typerepresentation.ClassValueTypeInformationMapper;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.jta.UserTransactionAdapter;

/**
 * Abstract base class for code based configuration of Spring managed Neo4j infrastructure.
 * <p>Subclasses are required to provide an implementation of graphDbService ....
 * 
 * @author Thomas Risberg
 */
@Configuration
public abstract class Neo4jConfiguration {
    private GraphDatabaseService graphDatabaseService;

    private ConversionService conversionService;

    @Autowired(required = false)
    private Validator validator;

    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    @Qualifier("conversionService")
    @Autowired(required = false)
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Autowired
    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    @Bean
    public MappingInfrastructure mappingInfrastructure() throws Exception {
        MappingInfrastructure infrastructure = new MappingInfrastructure();
        infrastructure.setGraphDatabaseService(getGraphDatabaseService());
        infrastructure.setTypeRepresentationStrategyFactory(typeRepresentationStrategyFactory());
        infrastructure.setConversionService(neo4jConversionService());
        infrastructure.setMappingContext(mappingContext());
        infrastructure.setEntityStateHandler(entityStateHandler());

        infrastructure.setNodeEntityStateFactory(nodeEntityStateFactory());
        infrastructure.setNodeTypeRepresentationStrategy(nodeTypeRepresentationStrategy());
        infrastructure.setNodeEntityInstantiator(graphEntityInstantiator());

        infrastructure.setRelationshipEntityStateFactory(relationshipEntityStateFactory());
        infrastructure.setRelationshipTypeRepresentationStrategy(relationshipTypeRepresentationStrategy());
        infrastructure.setRelationshipEntityInstantiator(graphRelationshipInstantiator());

        infrastructure.setTransactionManager(neo4jTransactionManager());
        infrastructure.setGraphDatabase(graphDatabase());
        
        infrastructure.setIndexProvider(indexProvider());

        if (validator!=null) {
            infrastructure.setValidator(validator);
        }
        return infrastructure;
    }

    @Bean(initMethod="postConstruct")
    public Neo4jTemplate neo4jTemplate() throws Exception {
        final Neo4jTemplate neo4jTemplate = new Neo4jTemplate();
        neo4jTemplate.setInfrastructure(mappingInfrastructure());
        nodeEntityStateFactory().setTemplate(neo4jTemplate);
        relationshipEntityStateFactory().setTemplate(neo4jTemplate);
        return neo4jTemplate;
	}

    @Bean
    public TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy() throws Exception {
        return typeRepresentationStrategyFactory().getRelationshipTypeRepresentationStrategy();
    }

    @Bean
    public TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy() throws Exception {
        return typeRepresentationStrategyFactory().getNodeTypeRepresentationStrategy();
    }

    @Bean
    public TypeRepresentationStrategyFactory typeRepresentationStrategyFactory() throws Exception {
        return new TypeRepresentationStrategyFactory(graphDatabase(), indexProvider());
    }

    @Bean
    public EntityStateHandler entityStateHandler() throws Exception {
        return new EntityStateHandler(mappingContext(),graphDatabase());
    }


    @Bean
    public TypeMapper<Node> nodeTypeMapper() throws Exception {
        return new DefaultTypeMapper<Node>(new TRSTypeAliasAccessor<Node>(nodeTypeRepresentationStrategy()),asList(new ClassValueTypeInformationMapper()));
    }

    @Bean
    public TypeMapper<Relationship> relationshipTypeMapper() throws Exception {
        return new DefaultTypeMapper<Relationship>(new TRSTypeAliasAccessor<Relationship>(relationshipTypeRepresentationStrategy()),asList(new ClassValueTypeInformationMapper()));
    }

    @Bean
    public Neo4jEntityFetchHandler entityFetchHandler() throws Exception {
        final SourceStateTransmitter<Node> nodeSourceStateTransmitter = nodeStateTransmitter();
        final SourceStateTransmitter<Relationship> relationshipSourceStateTransmitter = new SourceStateTransmitter<Relationship>(relationshipEntityStateFactory());
        return new Neo4jEntityFetchHandler(entityStateHandler(), neo4jConversionService(), nodeSourceStateTransmitter, relationshipSourceStateTransmitter);
    }

    @Bean
    public SourceStateTransmitter<Node> nodeStateTransmitter() throws Exception {
        return new SourceStateTransmitter<Node>(nodeEntityStateFactory());
    }

    //@Scope(BeanDefinition.SCOPE_PROTOTYPE)
    @Bean
    protected ConversionService neo4jConversionService() throws Exception {
        final Neo4jConversionServiceFactoryBean neo4jConversionServiceFactoryBean = new Neo4jConversionServiceFactoryBean();
        if (conversionService!=null) {
            neo4jConversionServiceFactoryBean.addConverters(conversionService);
            return conversionService;
        }
        return neo4jConversionServiceFactoryBean.getObject();
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
    public Neo4jMappingContext mappingContext() throws Exception {
        final Neo4jMappingContext mappingContext = new Neo4jMappingContext();
        nodeEntityStateFactory().setMappingContext(mappingContext);
        relationshipEntityStateFactory().setMappingContext(mappingContext);

        return mappingContext;
    }

    @Bean
    public RelationshipEntityStateFactory relationshipEntityStateFactory() throws Exception {
        return new RelationshipEntityStateFactory();
    }

    @Bean
    public NodeEntityStateFactory nodeEntityStateFactory() throws Exception {
        return new NodeEntityStateFactory();
    }

    @Bean
    public DelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory() throws Exception {
        final NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory = new NodeDelegatingFieldAccessorFactory(neo4jTemplate());
        nodeEntityStateFactory().setNodeDelegatingFieldAccessorFactory(nodeDelegatingFieldAccessorFactory);
        return nodeDelegatingFieldAccessorFactory;
    }
    
    @Bean
    public DelegatingFieldAccessorFactory relationshipDelegatingFieldAccessorFactory() throws Exception {
        final RelationshipDelegatingFieldAccessorFactory relationshipDelegatingFieldAccessorFactory = new RelationshipDelegatingFieldAccessorFactory(neo4jTemplate());
        relationshipEntityStateFactory().setRelationshipDelegatingFieldAccessorFactory(relationshipDelegatingFieldAccessorFactory);
        return relationshipDelegatingFieldAccessorFactory;
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
        return new ConfigurationCheck(neo4jTemplate(),neo4jTransactionManager());
    }

    @Bean
    public PersistenceExceptionTranslator persistenceExceptionTranslator() {
        return new Neo4jExceptionTranslator();
    }

    @Bean
    public IndexProvider indexProvider() throws Exception {
        return new IndexProviderImpl(mappingContext(), graphDatabase());
    }
}
