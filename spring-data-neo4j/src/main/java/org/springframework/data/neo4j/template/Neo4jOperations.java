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
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.neo4j.support.query.QueryEngine;
import org.springframework.data.util.TypeInformation;

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

    <T> T getReferenceNode(Class<T> target);

    /**
     * Delegates to the GraphDatabase
     *
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws NotFoundException
     */
    Node getNode(long id);

    Node createNode(Map<String, Object> props);

    Node createNode();

    <T> T createNodeAs(Class<T> target, Map<String, Object> properties);

    Result<Node> createNodes(Map<String, Object> firstNode, Map<String, Object>... otherNodes);

    <T> Iterable<T> createNodesAs(Class<T> target, Map<String, Object> firstNode, Map<String, Object>... otherNodes);


    /**
     * Delegates to the GraphDatabase
     *
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws NotFoundException
     */
    Relationship getRelationship(long id);

    Relationship createRelationshipBetween(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props);

    <R> R getRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType);

	Relationship getRelationshipBetween(Object start, Object end, String relationshipType);
    
    void removeRelationshipBetween(Object start, Object end, String type);

    <R> R createRelationshipBetween(Object start, Object end, Class<R> relationshipEntityClass, String relationshipType, boolean allowDuplicates);


    <S extends PropertyContainer, T> Index<S> getIndex(Class<T> type, String indexName);

    /**
     * Indexes the given field and value for the element.
     *
     * @param indexName Name of the index, will be checked against existing indexes according to the given element
     *                  assumes a "node" node index  or "relationship" relationship index for a null value
     * @param element   node or relationship to index
     * @param field     field to index
     * @param value     value to index
     * @param <T>       the provided element type
     * @return the provided element for convenience
     */
    <T extends PropertyContainer> T index(String indexName, T element, String field, Object value);

    /**
     * The value is looked up in the Neo4j index returning the IndexHits wrapped in a QueryResult to be converted
     * into Paths or Entities.
     */
    <T extends PropertyContainer> Result<T> lookup(String indexName, String field, Object value);

    /**
     * The query is executed on the index returning the IndexHits wrapped in a QueryResult to be converted
     * into Paths or Entities.
     */
    <T extends PropertyContainer> Result<T> lookup(String indexName, Object query);

    <T extends PropertyContainer> Result<T> lookup(Class<?> indexedType, Object query);

    Object query(String statement, Map<String, Object> params, TypeInformation<?> typeInformation);

    QueryEngine queryEngineFor(QueryType type);

    /**
     * Runs the given cypher statement and packages the result in a QueryResult, simple conversions via the
     * registered converter-factories are already executed via this method.
     */
    Result<Map<String, Object>> query(String statement, Map<String, Object> params);

    /**
     * Executes the given Gremlin statement and returns the result packaged as QueryResult as Neo4j types, not
     * Gremlin types. Table rows are converted to Map<String,Object>.
     */
    Result<Object> execute(String statement, Map<String, Object> params);

    /**
     * Traverses the graph starting at the given node with the provided traversal description. The Path's of the
     * traversal will be packaged into a QueryResult which can be easily converted into Nodes, Relationships or
     * Graph-Entities.
     */
    Result<Path> traverse(Node startNode, TraversalDescription traversal);

    Result<Path> traverse(Object start, TraversalDescription traversal);

    <T> Iterable<T> traverse(Object entity, Class<?> targetType, TraversalDescription traversalDescription);

    /**
     * Converts the Iterable into a QueryResult object for uniform handling. E.g.
     * template.convert(node.getRelationships());
     */
    <T> Result<T> convert(Iterable<T> iterable);

    <T> T convert(Object value, Class<T> type);

    <T> ClosableIterable<T> findAll(Class<T> entityClass);

    <T> long count(Class<T> entityClass);

    <S extends PropertyContainer, T> T projectTo(Object entity, Class<T> targetType);

    <T> T save(T entity);

    void remove(Object entity);

    <S extends PropertyContainer> S getPersistentState(Object entity);
}
