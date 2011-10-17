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
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.neo4j.annotation.QueryType;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.query.QueryEngine;

import java.util.Map;

/**
 * A template with convenience operations, exception translation and implicit transaction for modifying methods
 * @author mh
 * @since 19.02.11
 */
public interface Neo4jOperations {

    /**
     * Executes the callback in a NON-transactional context.
     * @param callback for executing graph operations NON-transactionally, not null
     * @param <T> return type
     * @return whatever the callback chooses to return
     * @throws org.springframework.dao.DataAccessException subclasses
     */
    <T> T exec(GraphCallback<T> callback);

    /**
     * Delegates to the GraphDatabase
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws NotFoundException
     */
    Node getNode(long id);

    /**
     * Transactionally creates the node, sets the properties (if any) and indexes the given fields (if any).
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>template.createNode(Property._("name","value"));</code>
     * <code>template.createNode(Property._("name","value","prop","anotherValue"));</code>
     *
     *
     * @param props properties to be set at node creation might be null
     * @return the newly created node
     */
    Node createNode(Map<String,Object> props);

    /**
     * Delegates to the GraphDatabase
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws NotFoundException
     */
    Relationship getRelationship(long id);

    /**
     * Transactionally creates the relationship, sets the properties (if any) and indexes the given fielss (if any)
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>template.createRelationship(from,to,TYPE, Property._("name","value"));</code>
     * <code>template.createRelationship(from,to,TYPE, Property._("name","value","prop","anotherValue"));</code>
     *
     * @param startNode start-node of relationship
     * @param endNode end-node of relationship
     * @param type relationship type, might by an enum implementing RelationshipType or a DynamicRelationshipType.withName("name")
     * @param props optional initial properties
     * @return  the newly created relationship
     */
    Relationship createRelationshipBetween(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props);

    /**
     * Indexes the given field and value for the element.
     * @param indexName Name of the index, will be checked against existing indexes according to the given element
     * assumes a "node" node index  or "relationship" relationship index for a null value
     * @param element node or relationship to index
     * @param field field to index
     * @param value value to index
     * @param <T> the provided element type
     * @return the provided element for convenience
     */
    <T extends PropertyContainer> T index(String indexName, T element, String field, Object value);

    /**
     * Converts the Iterable into a QueryResult object for uniform handling. E.g.
     * template.convert(node.getRelationships());
     */
    <T> Result<T> convert(Iterable<T> iterable);

    /**
     * Runs the given cypher statement and packages the result in a QueryResult, simple conversions via the
     * registered converter-factories are already executed via this method.
     */
    Result<Map<String, Object>> query(String statement,Map<String,Object> params);

    /**
     * Executes the given Gremlin statement and returns the result packaged as QueryResult as Neo4j types, not
     * Gremlin types. Table rows are converted to Map<String,Object>.
     */
    Result<Object> execute(String statement, Map<String,Object> params);

    /**
     * Traverses the graph starting at the given node with the provided traversal description. The Path's of the
     * traversal will be packaged into a QueryResult which can be easily converted into Nodes, Relationships or
     * Graph-Entities.
     */
    Result<Path> traverse(Node startNode, TraversalDescription traversal);

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

    Result<Path> traverse(Object start, TraversalDescription traversal);

    <T extends PropertyContainer> Result<T> lookup(Class<?> indexedType, Object query);

    Node createNode();

    @SuppressWarnings("unchecked")
    <T> T convert(Object value, Class<T> type);

    /**
     * Delegates to the GraphDatabase
     * @return the reference node of the underlying graph database
     */
    <T> T getReferenceNode(Class<T> target);

    QueryEngine queryEngineFor(QueryType type);
}
