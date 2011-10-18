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

package org.springframework.data.neo4j.template;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;

/**
 * A template with convenience operations, exception translation and implicit transaction for modifying methods
 *
 * @author mh
 * @since 19.02.11
 */
public interface Neo4jOperations {

    /**
     * Executes the callback in a NON-transactional context.
     *
     * @param callback for executing graph operations NON-transactionally, not null
     * @param <T>      return type
     * @return whatever the callback chooses to return
     * @throws org.springframework.dao.DataAccessException
     *          subclasses
     */
    <T> T exec(GraphCallback<T> callback);

    <T> GraphRepository<T> repositoryFor(Class<T> clazz);

    /**
     * Returns the reference node.
     */
    Node getReferenceNode();

    /**
     * Delegates to the GraphDatabase
     *
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws NotFoundException
     */
    Node getNode(long id);

    /**
     * Creates a node
     * @param properties the properties that should be initially set on the node
     */
    Node createNode(Map<String, Object> properties);

    Node createNode();

    /**
     * Creates a node mapped by the given entity class
     * @param target mapped entity class or Node.class
     * @param properties the properties that should be initially set on the node
     */
    <T> T createNodeAs(Class<T> target, Map<String, Object> properties);


    /**
     * Delegates to the GraphDatabase
     *
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws NotFoundException
     */
    Relationship getRelationship(long id);

    /**
     * Creates a relationship with the given initial properties.
     */
    Relationship createRelationshipBetween(Node startNode, Node endNode, String type, Map<String, Object> props);

    /**
     * Retrieves a single relationship entity between two node entities with the given relationship type projected to the provided
     * relationship entity class
     */
    <R> R getRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType);

    /**
     * Retrieves a single relationship entity between two node entities.
     */
    Relationship getRelationshipBetween(Object start, Object end, String relationshipType);

    /**
     * Removes the relationship of this type between the two node entities
     */
    void deleteRelationshipBetween(Object start, Object end, String type);

    /**
     * Creates a single relationship entity between two node entities with the given relationship type projected to the provided
     * relationship entity class. If it allowDuplicates existing relationships won't be taken into account. Returns the projected
     * relationship entity.
     */
    <R> R createRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType, boolean allowDuplicates);


    /**
     * Retrieves an existing index for the given class and/or name
     * @param type entity class, might be null
     * @param indexName might be null
     * @return Index&lt;Node%gt; or Index&lt;Relationship&gt;
     */
    <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName);

    /**
     * Indexes the given field and value for the element.
     *
     * @param indexName Name of the index, will be checked against existing indexes according to the given element
     *                  assumes a "node" node index or "relationship" relationship index for a null value
     * @param element   node or relationship to index
     * @param field     field to index
     * @param value     value to index
     * @param <T>       the provided element type
     * @return the provided element for convenience
     */
    <T extends PropertyContainer> T index(String indexName, T element, String field, Object value);

    /**
     * The value is looked up in the Neo4j index returning the IndexHits wrapped in a Result to be converted
     * into Paths or Entities.
     */
    <T extends PropertyContainer> Result<T> lookup(String indexName, String field, Object value);

    /**
     * The query is executed on the index returning the IndexHits wrapped in a Result to be converted
     * into Paths or Entities.
     */
    <T extends PropertyContainer> Result<T> lookup(String indexName, Object query);

    /**
     * The query is executed on the index for this entity type returning the IndexHits wrapped in a Result to be converted
     * into Paths or Entities.
     */
    <T extends PropertyContainer> Result<T> lookup(Class<?> indexedType, Object query);

    /**
     * Provides a cypher or gremlin query engine set up with a default entity converter.
     */
    QueryEngine queryEngineFor(QueryType type);

    /**
     * Runs the given cypher statement and packages the result in a Result, simple conversions via the
     * registered converter-factories are already executed via this method.
     */
    Result<Map<String, Object>> query(String statement, Map<String, Object> params);

    /**
     * Executes the given Gremlin statement and returns the result packaged as Result as Neo4j types, not
     * Gremlin types. The Neo4j-Graph is provided as variable "g". Table rows are converted to Map<String,Object>.
     */
    Result<Object> execute(String statement, Map<String, Object> params);

    /**
     * Traverses the graph starting at the given node with the provided traversal description. The Path's of the
     * traversal will be packaged into a Result which can be easily converted into Nodes, Relationships or
     * Graph-Entities.
     */
    Result<Path> traverse(Node startNode, TraversalDescription traversal);

    /**
     * Traverses the graph starting at the given node entity with the provided traversal description. The Path's of the
     * traversal will be packaged into a Result which can be easily converted into Nodes, Relationships or
     * Graph-Entities.
     */
    Result<Path> traverse(Object start, TraversalDescription traversal);

    /**
     * Converts the Iterable into a Result object for uniform handling. E.g.
     * template.convert(node.getRelationships());
     */
    <T> Result<T> convert(Iterable<T> iterable);

    /**
     * Converts a single object according to the configured ResultConverter of the Neo4j-Template.
     */
    <T> T convert(Object value, Class<T> type);

    /**
     * Retrieves a node or relationship and returns it mapped to the appropriate type
     * @return mapped entity or null
     */
    <T> T findOne(long id, Class<T> type);
    /**
     * Provides all instances of a given entity type using the typerepresentation strategy configured for this template.
     * This method is also provided by the appropriate repository.
     */
    <T> ClosableIterable<T> findAll(Class<T> entityClass);

    /**
     * Provies the instance count a given entity type using the typerepresentation strategy configured for this template.
     * This method is also provided by the appropriate repository.
     */
    <T> long count(Class<T> entityClass);

    /**
     * Projects a node or relationship entity to a different type. This can be used to use the same, schema free data
     * in different contexts.
     */
    <T> T projectTo(Object entity, Class<T> targetType);

    /**
     * Stores the given entity in the graph, if the entity is already attached to the graph, the node is updated, otherwise
     * a new node is created. Attached relationships will be cascaded.
     * This method is also provided by the appropriate repository.
     */
    <T> T save(T entity);

    /**
     * Removes the given node or relationship entity or node or relationship from the graph, the entity is first removed
     * from all indexes and then deleted.
     */
    void delete(Object entity);

    /**
     * Returns the node or relationship that backs the given entity.
     */
    <S extends PropertyContainer> S getPersistentState(Object entity);

    TraversalDescription traversalDescription();

    GraphDatabase getGraphDatabase();
}
