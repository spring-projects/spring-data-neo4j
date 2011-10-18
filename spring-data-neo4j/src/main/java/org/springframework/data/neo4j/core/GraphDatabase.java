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

package org.springframework.data.neo4j.core;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;


public interface GraphDatabase {
    /**
     * @return the reference node of the underlying graph database
     */
    Node getReferenceNode();

    /**
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Node getNodeById(long id);

    Node createNode(Map<String, Object> props);

    /**
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Relationship getRelationshipById(long id);

    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props);

    /**
     * @param indexName existing index name, not null
     * @return existing index {@link Index}
     * @throws IllegalArgumentException if the index doesn't exist
     */
    <T extends PropertyContainer> Index<T> getIndex(String indexName);

    /**
     * creates a index
     * @param type type of index requested - either Node.class or Relationship.class
     * @param indexName, not null
     * @param fullText true if a fulltext queryable index is needed, false for exact match
     * @return node index {@link Index}
     */
    <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, boolean fullText);


    /**
     * @return a TraversalDescription as starting point for defining a traversal
     */
    TraversalDescription traversalDescription();

    <T> QueryEngine<T> queryEngineFor(QueryType type);

    void setConversionService(ConversionService conversionService);

    <T> QueryEngine<T> queryEngineFor(QueryType type, ResultConverter resultConverter);

    boolean transactionIsRunning();

    void remove(Node node);

    void remove(Relationship relationship);

    void setResultConverter(ResultConverter resultConverter);
}
