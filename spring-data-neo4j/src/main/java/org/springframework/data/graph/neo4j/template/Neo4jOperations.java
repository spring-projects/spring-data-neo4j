package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

/**
 * @author mh
 * @since 19.02.11
 */
public interface Neo4jOperations {
    <T> T doInTransaction(GraphTransactionCallback<T> callback);

    <T> T execute(GraphCallback<T> callback);

    Node getReferenceNode();

    Node getNode(long id);

    Node createNode(Property... props);

    Relationship getRelationship(long id);

    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Property... props);

    <T> Iterable<T> queryNodes(String indexName, Object queryOrQueryObject, PathMapper<T> pathMapper);

    <T> Iterable<T> retrieveNodes(String indexName, String field, String value, PathMapper<T> pathMapper);

    <T> Iterable<T> queryRelationships(String indexName, Object queryOrQueryObject, PathMapper<T> pathMapper);

    <T> Iterable<T> retrieveRelationships(String indexName, String field, String value, PathMapper<T> pathMapper);

    <T> Iterable<T> traverse(Node startNode, TraversalDescription traversal, PathMapper<T> pathMapper);

    <T> Iterable<T> traverseDirectRelationships(Node startNode, RelationshipType type, Direction direction, PathMapper<T> pathMapper);

    <T> Iterable<T> traverseDirectRelationships(Node startNode, PathMapper<T> pathMapper, RelationshipType... type);

    <T> Iterable<T> traverseDirectRelationships(Node startNode, PathMapper<T> pathMapper);

    void index(Relationship relationship, String indexName, String field, Object value);

    void index(Node node, String indexName, String field, Object value);
}
