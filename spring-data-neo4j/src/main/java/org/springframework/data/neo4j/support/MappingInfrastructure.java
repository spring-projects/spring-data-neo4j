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
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.index.IndexProvider;
import org.springframework.data.neo4j.support.mapping.EntityRemover;
import org.springframework.data.neo4j.support.mapping.EntityStateHandler;
import org.springframework.data.neo4j.support.mapping.Neo4jEntityPersister;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.support.query.CypherQueryExecutor;
import org.springframework.data.neo4j.support.schema.SchemaIndexProvider;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategies;
import org.springframework.data.neo4j.support.typesafety.TypeSafetyPolicy;
import org.springframework.transaction.PlatformTransactionManager;

import javax.validation.Validator;

/**
 * @author mh
 * @since 17.10.11
 */
public class MappingInfrastructure implements Infrastructure {

    private final ConversionService conversionService;
    private final Validator validator;
    private final TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;

    private final TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;

    private final Neo4jMappingContext mappingContext;
    private final CypherQueryExecutor cypherQueryExecutor;
    private final EntityStateHandler entityStateHandler;
    private final Neo4jEntityPersister entityPersister;
    private final EntityRemover entityRemover;
    private final TypeRepresentationStrategies typeRepresentationStrategies;
    private final PlatformTransactionManager transactionManager;
    private final ResultConverter resultConverter;
    private final IndexProvider indexProvider;
    private final SchemaIndexProvider schemaIndexProvider;
    private final GraphDatabaseService graphDatabaseService;
    private final GraphDatabase graphDatabase;
    private final TypeSafetyPolicy typeSafetyPolicy;

    public MappingInfrastructure(GraphDatabase graphDatabase, GraphDatabaseService graphDatabaseService, IndexProvider indexProvider, ResultConverter resultConverter, PlatformTransactionManager transactionManager, TypeRepresentationStrategies typeRepresentationStrategies, EntityRemover entityRemover, Neo4jEntityPersister entityPersister, EntityStateHandler entityStateHandler, CypherQueryExecutor cypherQueryExecutor, Neo4jMappingContext mappingContext, TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy, Validator validator, ConversionService conversionService, SchemaIndexProvider schemaIndexProvider, TypeSafetyPolicy typeSafetyPolicy) {
        this.graphDatabase = graphDatabase;
        this.graphDatabaseService = graphDatabaseService;
        this.indexProvider = indexProvider;
        this.resultConverter = resultConverter;
        this.transactionManager = transactionManager;
        this.typeRepresentationStrategies = typeRepresentationStrategies;
        this.entityRemover = entityRemover;
        this.entityPersister = entityPersister;
        this.entityStateHandler = entityStateHandler;
        this.cypherQueryExecutor = cypherQueryExecutor;
        this.mappingContext = mappingContext;
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
        this.validator = validator;
        this.conversionService = conversionService;
        this.schemaIndexProvider = schemaIndexProvider;
        this.typeSafetyPolicy = typeSafetyPolicy;
    }

    @Override
    public EntityStateHandler getEntityStateHandler() {
        return entityStateHandler;
    }

    @Override
    public ConversionService getConversionService() {
        return conversionService;
    }

    @Override
    public Validator getValidator() {
        return validator;
    }

    @Override
    public GraphDatabaseService getGraphDatabaseService() {
        return graphDatabaseService;
    }

    @Override
    public GraphDatabase getGraphDatabase() {
        return graphDatabase;
    }

    @Override
    public ResultConverter getResultConverter() {
        return resultConverter;
    }

    @Override
    public EntityRemover getEntityRemover() {
        return entityRemover;
    }

    @Override
    public IndexProvider getIndexProvider() {
        return indexProvider;
    }

    @Override
    public Neo4jEntityPersister getEntityPersister() {
        return entityPersister;
    }

    @Override
    public PlatformTransactionManager getTransactionManager() {
        return transactionManager;
    }

    @Override
    public TypeRepresentationStrategies getTypeRepresentationStrategies() {
        return typeRepresentationStrategies;
    }

    @Override
    public Neo4jMappingContext getMappingContext() {
        return mappingContext;
    }

    @Override
    public TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    @Override
    public TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }

    @Override
    public TypeSafetyPolicy getTypeSafetyPolicy() {
        return typeSafetyPolicy;
    }

    @Override
    public SchemaIndexProvider getSchemaIndexProvider() {
        return schemaIndexProvider;
    }

    @Override
    public CypherQueryExecutor getCypherQueryExecutor() {
        return cypherQueryExecutor;
    }
}
