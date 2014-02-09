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
 * @since 24.04.12
 */
public interface Infrastructure {
    EntityStateHandler getEntityStateHandler();

    ConversionService getConversionService();

    Validator getValidator();

    GraphDatabaseService getGraphDatabaseService();

    GraphDatabase getGraphDatabase();

    ResultConverter getResultConverter();

    EntityRemover getEntityRemover();

    IndexProvider getIndexProvider();

    Neo4jEntityPersister getEntityPersister();

    PlatformTransactionManager getTransactionManager();

    TypeRepresentationStrategies getTypeRepresentationStrategies();

    Neo4jMappingContext getMappingContext();

    TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy();

    TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy();

    TypeSafetyPolicy getTypeSafetyPolicy();

    SchemaIndexProvider getSchemaIndexProvider();

    CypherQueryExecutor getCypherQueryExecutor();
}
