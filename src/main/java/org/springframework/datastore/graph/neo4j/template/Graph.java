package org.springframework.datastore.graph.neo4j.template;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Traverser;
import org.springframework.core.convert.converter.Converter;
import org.springframework.datastore.graph.neo4j.template.graph.GraphDescription;
import org.springframework.datastore.graph.neo4j.template.graph.Property;
import org.springframework.datastore.graph.neo4j.template.traversal.Traversal;

import java.util.List;

public interface Graph {
    Node createNode(Property... params);

    Node getReferenceNode();

    Node getNodeById(final long id);

    Traverser traverse(final Traversal traversal);

    Traverser traverse(Node startNode, Traversal traversal);

    <T> List<T> traverse(Node startNode, Traversal traversal, Converter<Node, T> converter);

    void load(final GraphDescription graph);
}
