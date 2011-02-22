package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;

import java.util.Map;

/**
 * @author mh
 * @since 19.02.11
 */
public interface Neo4jOperations {
    <T> T update(GraphCallback<T> callback);

    <T> T exec(GraphCallback<T> callback);

    Node getReferenceNode();

    Node getNode(long id);

    Node createNode(Map<String, Object> props, String... indexFields);

    Relationship getRelationship(long id);

    Relationship createRelationship(Node startNode, Node endNode, RelationshipType type, Map<String, Object> props, String... indexFields);

    <T> Iterable<T> query(String indexName, PathMapper<T> pathMapper, Object queryOrQueryObject);

    <T> Iterable<T> query(String indexName, PathMapper<T> pathMapper, String field, String value);

    <T> Iterable<T> traverseGraph(Node startNode, PathMapper<T> pathMapper, TraversalDescription traversal);

    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper, RelationshipType type, Direction direction);

    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper, RelationshipType... type);

    <T> Iterable<T> traverseNext(Node startNode, PathMapper<T> pathMapper);

    <T extends PropertyContainer> T index(String indexName, T element, String field, Object value);
    <T extends PropertyContainer> T autoIndex(String indexName, T element, String... indexFields);
}
