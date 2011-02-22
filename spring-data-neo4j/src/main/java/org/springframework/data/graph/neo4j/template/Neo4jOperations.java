package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.Map;

/**
 * A template with convenience operations, exception translation and implicit transaction for modifying methods
 * @author mh
 * @since 19.02.11
 */
public interface Neo4jOperations {
    /**
     * Executes the callback in a transactional context, throwing an exception in the callback will cause the transaction to be rolled back
     * The callback is passed a GraphDatabaseService.
     * @param callback for executing graph operations transactionally, not null
     * @param <T> return type
     * @return whatever the callback chooses to return
     * @throws org.springframework.dao.DataAccessException subclasses
     */
    <T> T update(GraphCallback<T> callback);

    /**
     * Executes the callback in a NON-transactional context.
     * @param callback for executing graph operations NON-transactionally, not null
     * @param <T> return type
     * @return whatever the callback chooses to return
     * @throws org.springframework.dao.DataAccessException subclasses
     */
    <T> T exec(GraphCallback<T> callback);

    /**
     * Delegates to the GraphDatabaseService
     * @return the reference node of the underlying graph database
     */
    Node getReferenceNode();

    /**
     * Delegates to the GraphDatabaseService
     * @param id node id
     * @return the requested node of the underlying graph database
     * @throws NotFoundException
     */
    Node getNode(long id);

    /**
     * Transactionally creates the node, sets the properties (if any) and indexes the given fields (if any).
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>template.createNode(PropertyMap._("name","value"));</code>
     * <code>template.createNode(PropertyMap.props().set("name","value").set("prop","anotherValue").toMap(), "name", "prop");</code>
     * @param props properties to be set at node creation might be null
     * @param indexFields fields that are automatically indexed from the given properties for the newly created ndoe
     * @return the newly created node
     */
    Node createNode(Map<String, Object> props, String... indexFields);

    /**
     * Delegates to the GraphDatabaseService
     * @param id relationship id
     * @return the requested relationship of the underlying graph database
     * @throws NotFoundException
     */
    Relationship getRelationship(long id);

    /**
     * Transactionally creates the relationship, sets the properties (if any) and indexes the given fielss (if any)
     * Two shortcut means of providing the properties (very short with static imports)
     * <code>template.createRelationship(from,to,TYPE, PropertyMap._("name","value"));</code>
     * <code>template.createRelationship(from,to,TYPE, PropertyMap.props().set("name","value").set("prop","anotherValue").toMap(), "name", "prop");</code>
     * @param startNode start-node of relationship
     * @param endNode end-node of relationship
     * @param type relationship type, might by an enum implementing RelationshipType or a DynamicRelationshipType.withName("name")
     * @param props optional initial properties
     * @param indexFields optional indexed fields
     * @return  the newly created relationship
     */
    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props, String... indexFields);

    /**
     * Queries the supplied index with a lucene query string or query object (if the neo4j-index provider is lucene)
     * @param indexName Name of the index, will be checked against existing indexes, first relationship-indexes, then node indexes
     * assumes a "node" node index for a null value
     * @param pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param queryOrQueryObject a lucene query string or query object (if the neo4j-index provider is lucene)
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the query result mapping
     * @see IterationController for controlling eagerness of iteration
     */
    <T> Iterable<T> query(String indexName, PathMapper<T> pathMapper, Object queryOrQueryObject);

    /**
     * Queries the supplied index with a field - value combination
     * @param indexName Name of the index, will be checked against existing indexes, first relationship-indexes, then node indexes
     * assumes a "node" node index for a null value
     * @param pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param field field to query
     * @param value value to supply to index query
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the query result mapping
     * @see IterationController for controlling eagerness of iteration
     */
    <T> Iterable<T> query(String indexName, PathMapper<T> pathMapper, String field, String value);

    /**
     * Traverses the whole path with the given traversal descripting starting at the start node.
     * @param startNode start node for the traversal
     * @param pathMapper pathMapper a mapper that translates from the resulting paths into some domain object, might use PathMapper.WithoutResult for a callback behaviour
     * @param traversal a traversal description, possibly generated by the Traversal.description()... DSL
     * @param <T> expected type of result
     * @return a lazy (when mapped) or eagerly (when called back) iterable containing the results of the traversal result mapping
     */
    <T> Iterable<T> traverseGraph(Node startNode, PathMapper<T> pathMapper, TraversalDescription traversal);

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
    /**
     * Auto-indexes all indexFields for the given element's properties if they exist
     * @param indexName Name of the index, will be checked against existing indexes according to the given element
     * assumes a "node" node index  or "relationship" relationship index for a null value
     * @param element node or relationship to auto-index
     * @param indexProperties property names to index
     * @param <T> the provided element type
     * @return the provided element for convenience
     */
    <T extends PropertyContainer> T autoIndex(String indexName, T element, String... indexProperties);
}
