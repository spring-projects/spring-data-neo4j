package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 19.02.11
 */
public class NodePath implements Path {
    private final Node node;

    public NodePath(Node node) {
        this.node = node;
    }

    @Override
    public Node startNode() {
        return node;
    }

    @Override
    public Node endNode() {
        return node;
    }

    @Override
    public Relationship lastRelationship() {
        return null;
    }

    @Override
    public Iterable<Relationship> relationships() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<Node> nodes() {
        return asList(node);
    }

    @Override
    public int length() {
        return 0;
    }

    @Override
    public Iterator<PropertyContainer> iterator() {
        return Arrays.<PropertyContainer>asList(node).iterator();
    }
}

