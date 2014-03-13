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
package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.context.MappingContextIsNewStrategyFactory;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.fieldaccess.Neo4jConversionServiceFactoryBean;
import org.springframework.data.neo4j.fieldaccess.NodeDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.fieldaccess.RelationshipDelegatingFieldAccessorFactory;
import org.springframework.data.neo4j.mapping.EntityInstantiator;
import org.springframework.data.neo4j.support.conversion.EntityResultConverter;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.index.IndexProviderImpl;
import org.springframework.data.neo4j.support.mapping.EntityRemover;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.mapping.EntityTools;
import org.springframework.data.neo4j.support.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.node.EntityStateFactory;
import org.springframework.data.neo4j.support.node.NodeEntityInstantiator;
import org.springframework.data.neo4j.support.node.NodeEntityStateFactory;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityInstantiator;
import org.springframework.data.neo4j.support.relationship.RelationshipEntityStateFactory;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategies;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;
import org.springframework.data.neo4j.support.typesafety.TypeSafetyPolicy;
import org.springframework.data.support.IsNewStrategyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

import javax.validation.Validator;

/**
 * @author mh
 * @since 17.10.11
 */
public class MappingInfrastructureFactoryBean implements FactoryBean<Infrastructure>, InitializingBean {
    private ConversionService conversionService;
    private Validator validator;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;
    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;
    private TypeRepresentationStrategyFactory typeRepresentationStrategyFactory;

    private Neo4jMappingContext mappingContext;
    private CypherQueryExecutor cypherQueryExecutor;
    private EntityStateHandler entityStateHandler;
    private Neo4jEntityPersister entityPersister;
    private EntityStateFactory<Node> nodeEntityStateFactory;
    private EntityStateFactory<Relationship> relationshipEntityStateFactory;
    private EntityRemover entityRemover;
    private TypeRepresentationStrategies typeRepresentationStrategies;
    private EntityInstantiator<Relationship> relationshipEntityInstantiator;
    private EntityInstantiator<Node> nodeEntityInstantiator;
    private PlatformTransactionManager transactionManager;
    private ResultConverter resultConverter;
    private IndexProvider indexProvider;
    private SchemaIndexProvider schemaIndexProvider;
    private GraphDatabaseService graphDatabaseService;
    private GraphDatabase graphDatabase;
    private IsNewStrategyFactory isNewStrategyFactory;
    private TypeSafetyPolicy typeSafetyPolicy;

    private MappingInfrastructure mappingInfrastructure;
    private TypeRepresentationStrategyFactory.Strategy typeRepresentationStrategy;

    public MappingInfrastructureFactoryBean(GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        this.graphDatabase = graphDatabase;
        this.transactionManager = transactionManager;
    }
    public MappingInfrastructureFactoryBean(GraphDatabaseService graphDatabaseService, PlatformTransactionManager transactionManager) {
        this.graphDatabaseService = graphDatabaseService;
        this.transactionManager = transactionManager;
    }

    public MappingInfrastructureFactoryBean() {
    }

    @Override
    public void afterPropertiesSet() {
        try {
        if (this.mappingContext == null) {
            this.mappingContext = new Neo4jMappingContext();
        }
        if (this.isNewStrategyFactory == null) {
            this.isNewStrategyFactory = new MappingContextIsNewStrategyFactory(mappingContext); 
        }
        if (this.graphDatabaseService == null && graphDatabase instanceof DelegatingGraphDatabase) {
            this.graphDatabaseService = ((DelegatingGraphDatabase) graphDatabase).getGraphDatabaseService();
        }
        if (this.graphDatabase == null && graphDatabaseService instanceof GraphDatabase) {
            this.graphDatabase = (GraphDatabase)graphDatabaseService;
        }
        if (this.graphDatabase == null) {
            this.graphDatabase = new DelegatingGraphDatabase(graphDatabaseService);
        }
        if (this.transactionManager == null) {
            this.transactionManager = new JtaTransactionManager(graphDatabase.getTransactionManager());
        }
        if (this.conversionService==null) {
            this.conversionService=new Neo4jConversionServiceFactoryBean().getObject();
        }
        if (entityStateHandler == null) {
            entityStateHandler = new EntityStateHandler(mappingContext,graphDatabase);
        }
        if (nodeEntityInstantiator == null) {
            nodeEntityInstantiator = new NodeEntityInstantiator(entityStateHandler);
        }
        if (relationshipEntityInstantiator == null) {
            relationshipEntityInstantiator = new RelationshipEntityInstantiator(entityStateHandler);
        }
        if (this.typeRepresentationStrategyFactory == null) {
            this.typeRepresentationStrategyFactory = typeRepresentationStrategy!=null ? new TypeRepresentationStrategyFactory(graphDatabase,typeRepresentationStrategy) : new TypeRepresentationStrategyFactory(graphDatabase);
        }
        if (this.nodeTypeRepresentationStrategy == null) {
            this.nodeTypeRepresentationStrategy = typeRepresentationStrategyFactory.getNodeTypeRepresentationStrategy();
        }
        if (this.relationshipTypeRepresentationStrategy == null) {
            this.relationshipTypeRepresentationStrategy = typeRepresentationStrategyFactory.getRelationshipTypeRepresentationStrategy();
        }
        if (this.nodeEntityStateFactory==null) {
            this.nodeEntityStateFactory = new NodeEntityStateFactory(mappingContext, new NodeDelegatingFieldAccessorFactory.Factory());
        }
        if (this.relationshipEntityStateFactory==null) {
            this.relationshipEntityStateFactory = new RelationshipEntityStateFactory(mappingContext, new RelationshipDelegatingFieldAccessorFactory.Factory());
        }
        this.typeRepresentationStrategies = new TypeRepresentationStrategies(mappingContext, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy);

        final EntityStateHandler entityStateHandler = new EntityStateHandler(mappingContext, graphDatabase);
        EntityTools<Node> nodeEntityTools = new EntityTools<Node>(nodeTypeRepresentationStrategy, nodeEntityStateFactory, nodeEntityInstantiator, mappingContext);
        EntityTools<Relationship> relationshipEntityTools = new EntityTools<Relationship>(relationshipTypeRepresentationStrategy, relationshipEntityStateFactory, relationshipEntityInstantiator, mappingContext);
        this.entityPersister = new Neo4jEntityPersister(conversionService, nodeEntityTools, relationshipEntityTools, mappingContext, entityStateHandler);
        this.entityRemover = new EntityRemover(this.entityStateHandler, nodeTypeRepresentationStrategy, relationshipTypeRepresentationStrategy, graphDatabase);
        if (this.resultConverter == null) {
            this.resultConverter = new EntityResultConverter<Object, Object>(conversionService);
        }
        this.graphDatabase.setResultConverter(resultConverter);
        this.cypherQueryExecutor = new CypherQueryExecutor(graphDatabase.queryEngine(resultConverter));
        if (schemaIndexProvider == null) {
            schemaIndexProvider = new SchemaIndexProvider(graphDatabase);
        }
        if (this.indexProvider == null) {
            this.indexProvider = new IndexProviderImpl(graphDatabase);
        }
        if (this.typeSafetyPolicy == null) {
            this.typeSafetyPolicy = new TypeSafetyPolicy();
        }
        this.mappingInfrastructure = new MappingInfrastructure(graphDatabase, graphDatabaseService, indexProvider, resultConverter, transactionManager, typeRepresentationStrategies, entityRemover, entityPersister, entityStateHandler, cypherQueryExecutor, mappingContext, relationshipTypeRepresentationStrategy, nodeTypeRepresentationStrategy, validator, conversionService, schemaIndexProvider, typeSafetyPolicy);
        } catch (Exception e) {
            throw new RuntimeException("error initializing "+getClass().getName(),e);
        }
    }

    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public void setRelationshipEntityInstantiator(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    public void setNodeEntityInstantiator(EntityInstantiator<Node> nodeEntityInstantiator) {
        this.nodeEntityInstantiator = nodeEntityInstantiator;
    }


    public void setEntityStateHandler(EntityStateHandler entityStateHandler) {
        this.entityStateHandler = entityStateHandler;
    }

    public void setNodeEntityStateFactory(EntityStateFactory<Node> nodeEntityStateFactory) {
        this.nodeEntityStateFactory = nodeEntityStateFactory;
    }

    public void setRelationshipEntityStateFactory(EntityStateFactory<Relationship> relationshipEntityStateFactory) {
        this.relationshipEntityStateFactory = relationshipEntityStateFactory;
    }

    public EntityStateHandler getEntityStateHandler() {
        return entityStateHandler;
    }

    public TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public void setNodeTypeRepresentationStrategy(TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy) {
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
    }

    public TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    public void setRelationshipTypeRepresentationStrategy(TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy) {
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
    }

    public ConversionService getConversionService() {
        return conversionService;
    }

    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public Validator getValidator() {
        return validator;
    }

    public void setValidator(Validator validatorFactory) {
        this.validator = validatorFactory;
    }

    public void setMappingContext(Neo4jMappingContext mappingContext) {
        this.mappingContext = mappingContext;
    }


    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    public void setGraphDatabaseService(GraphDatabaseService graphDatabaseService) {
        this.graphDatabaseService = graphDatabaseService;
    }

    public void setGraphDatabase(GraphDatabase graphDatabase) {
        this.graphDatabase = graphDatabase;
    }

    public GraphDatabase getGraphDatabase() {
        return graphDatabase;
    }

    public ResultConverter getResultConverter() {
        return resultConverter;
    }

    public EntityRemover getEntityRemover() {
        return entityRemover;
    }

    public IndexProvider getIndexProvider() {
        return indexProvider;
    }

    public Neo4jEntityPersister getEntityPersister() {
        return entityPersister;
    }

    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    public TypeRepresentationStrategies getTypeRepresentationStrategies() {
        return typeRepresentationStrategies;
    }


    public CypherQueryExecutor getCypherQueryExecutor() {
        return cypherQueryExecutor;
    }

    public Neo4jMappingContext getMappingContext() {
        return mappingContext;
    }

    public void setTypeRepresentationStrategyFactory(TypeRepresentationStrategyFactory typeRepresentationStrategyFactory) {
        this.typeRepresentationStrategyFactory = typeRepresentationStrategyFactory;
    }
    public void setTypeRepresentationStrategy(TypeRepresentationStrategyFactory.Strategy strategy) {
        this.typeRepresentationStrategy = strategy;
    }

    public void setIndexProvider(IndexProvider indexProvider) {
        this.indexProvider = indexProvider;
    }

    public IsNewStrategyFactory getIsNewStrategyFactory() {
        return isNewStrategyFactory;
    }

    public void setIsNewStrategyFactory(IsNewStrategyFactory newStrategyFactory) {
        isNewStrategyFactory = newStrategyFactory;
    }

    public void setTypeSafetyPolicy(TypeSafetyPolicy typeSafetyPolicy) {
        this.typeSafetyPolicy = typeSafetyPolicy;
    }

    public TypeSafetyPolicy getTypeSafetyPolicy() {
        return typeSafetyPolicy;
    }

    @Override
    public Infrastructure getObject() {
        return mappingInfrastructure;
    }

    @Override
    public Class<?> getObjectType() {
        return Infrastructure.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public static Infrastructure createDirect(GraphDatabase graphDatabase, PlatformTransactionManager transactionManager) {
        final MappingInfrastructureFactoryBean factoryBean = new MappingInfrastructureFactoryBean(graphDatabase, transactionManager);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
    public static Infrastructure createDirect(GraphDatabaseService graphDatabase, PlatformTransactionManager transactionManager) {
        final MappingInfrastructureFactoryBean factoryBean = new MappingInfrastructureFactoryBean(graphDatabase, transactionManager);
        factoryBean.afterPropertiesSet();
        return factoryBean.getObject();
    }
}
