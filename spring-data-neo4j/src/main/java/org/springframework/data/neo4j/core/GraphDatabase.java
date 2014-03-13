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

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.query.CypherQueryEngine;
import org.springframework.data.neo4j.support.query.CypherQueryEngineImpl;

import javax.transaction.TransactionManager;
import java.util.Collection;
import java.util.Map;


public interface GraphDatabase {
    /**
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Node getNodeById(long id);

    /**
     * creates the node and initializes its properties
     */
    Node createNode(Map<String, Object> props, Collection<String> labels);

    /**
     * creates the node uniquely or returns an existing node with the same label-key-value combination.
     * properties are used to initialize the node. It needs a unique constraint to work correctly.
     */
    Node merge(String labelName, String key, Object value, final Map<String, Object> properties, Collection<String> labels);
    /**
     * creates the node uniquely or returns an existing node with the same index-key-value combination.
     * properties are used to initialize the node.
     */
    Node getOrCreateNode(String indexName, String key, Object value, final Map<String, Object> properties, Collection<String> labels);

    /**
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws org.neo4j.graphdb.NotFoundException
     */
    Relationship getRelationshipById(long id);

    /**
     * creates the relationship between the startNode, endNode with the given type which will be populated with the provided properties
     */
    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> properties);


    /**
     * Creates the relationship uniquely, uses the given index,key,value to achieve that.
     * If the relationship for this combination already existed it is returned otherwise created and populated with the provided properties.
     */
    Relationship getOrCreateRelationship(String indexName, String key, Object value, Node startNode, Node endNode, String type, Map<String, Object> properties);

    /**
     * deletes the Node and its index entries
     */
    void remove(Node node);

    /**
     * deletes the relationship and its index entries
     */
    void remove(Relationship relationship);

    /**
     * @param indexName existing index name, not null
     * @return existing index {@link Index}
     * @throws IllegalArgumentException if the index doesn't exist
     */
    <T extends PropertyContainer> Index<T> getIndex(String indexName);

    /**
     * creates a index
     * @param type type of index requested - either Node.class or Relationship.class
     * @param indexType SIMPLE, FULLTEXT or POINT declaring the requested index-type
     * @return node index {@link Index}
     */
    <T extends PropertyContainer> Index<T> createIndex(Class<T> type, String indexName, IndexType indexType);


    /**
     * @return a TraversalDescription as starting point for defining a traversal
     */
    TraversalDescription traversalDescription();

    /**
     * returns a query engine for the provided type (Cypher) which is initialized with the default result converter
     */
    CypherQueryEngine queryEngine();

    /**
     * returns a query engine for the provided type (Cypher) which is initialized with the provided result converter
     */
    CypherQueryEngine queryEngine(ResultConverter resultConverter);

    /**
     * @param conversionService the conversion service to be used for the default result converter of this database
     */
    void setConversionService(ConversionService conversionService);

    /**
     * @param resultConverter the default result converter to be used with this database
     */
    void setResultConverter(ResultConverter resultConverter);

    /**
     * @return true if a transaction is currently running
     */
    boolean transactionIsRunning();

    TransactionManager getTransactionManager();

    Transaction beginTx();

    void shutdown();

    Collection<String> getAllLabelNames();
}
