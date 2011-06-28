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

package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.graph.core.Property;
import org.springframework.data.graph.neo4j.support.path.PathMapper;
import org.springframework.data.graph.neo4j.support.query.QueryEngine;

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
     * @return the reference node of the underlying graph database
     */
    Node getReferenceNode();

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
    Node createNode(Property... props);

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
    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Property... props);

    /**
     * Queries the supplied index with a lucene query string or query object (if the neo4j-index provider is lucene)
     *
     *
     * @param indexName Name of the index, will be checked against existing indexes, first relationship-indexes, then node indexes
     * assumes a "node" node index for a null value
     * @param queryOrQueryObject a lucene query string or query object (if the neo4j-index provider is lucene)
     * @param pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the query result mapping
     * @see org.springframework.data.graph.neo4j.support.path.IterationController for controlling eagerness of iteration
     */
    <T> ClosableIterable<T> lookup(String indexName, Object queryOrQueryObject, PathMapper<T> pathMapper);

    /**
     * Queries the supplied index with a field - value combination
     *
     *
     * @param indexName Name of the index, will be checked against existing indexes, first relationship-indexes, then node indexes
     * assumes a "node" node index for a null value
     * @param field field to query
     * @param value value to supply to index query
     * @param pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the query result mapping
     * @see org.springframework.data.graph.neo4j.support.path.IterationController for controlling eagerness of iteration
     */
    <T> ClosableIterable<T> lookup(String indexName, String field, String value, PathMapper<T> pathMapper);

    /**
     * Traverses the whole path with the given traversal descripting starting at the start node.
     *
     * @param traversal a traversal description, possibly generated by the Traversal.description()... DSL
     * @param startNode start node for the traversal
     * @param pathMapper pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the traversal result mapping
     */
    <T> Iterable<T> traverse(TraversalDescription traversal, Node startNode, PathMapper<T> pathMapper);

    /**
     * Traverses only to the direct neighbours of the start node
     * @param startNode start node for the traversal
     * @param pathMapper pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param type type of relationships to consider
     * @param direction direction of relationship to consider (can be OUTGOING, INCOMING, BOTH)
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the traversal result mapping
     */
    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper, RelationshipType type, Direction direction);

    /**
     * Traverses only to the direct neighbours of the start node for the specified relationship types
     * @param startNode start node for the traversal
     * @param pathMapper pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param types types of relationships to consider
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the traversal result mapping
     */
    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper, RelationshipType... types);

    /**
     * Traverses only to all direct neighbours of the start node for all relationships
     * @param startNode start node for the traversal
     * @param pathMapper pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the traversal result mapping
     */
    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper);

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

    Iterable<Map<String, Object>> query(QueryEngine.Type engineType, String statement);

    <T> Iterable<T> query(QueryEngine.Type engineType, String statement, Class<T> type);

    <T> T queryForObject(QueryEngine.Type engineType, String statement, Class<T> type);
}
