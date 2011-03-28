package org.neo4j.rest.graphdb;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.util.Iterator;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public class SimplePath implements Path {
    Iterable<Relationship> relationships;
    Iterable<Node> nodes;
    private final Relationship lastRelationship;
    private int length;
    private Node startNode;
    private Node endNode;

    public SimplePath(Node startNode, Node endNode, Relationship lastRelationship, int length, Iterable<Node> nodes, Iterable<Relationship> relationships) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.lastRelationship = lastRelationship;
        this.length = length;
        this.nodes = nodes;
        this.relationships = relationships;
    }

    public Node startNode() {
        return startNode;
    }

    public Node endNode() {
        return endNode;
    }

    public Relationship lastRelationship() {
        return lastRelationship;
    }

    public Iterable<Relationship> relationships() {
        return relationships;
    }

    public Iterable<Node> nodes() {
        return nodes;
    }

    public int length() {
        return length;
    }

    public Iterator<PropertyContainer> iterator() {
        return new Iterator<PropertyContainer>()
        {
            Iterator<? extends PropertyContainer> current = nodes().iterator();
            Iterator<? extends PropertyContainer> next = relationships().iterator();

            public boolean hasNext()
            {
                return current.hasNext();
            }

            public PropertyContainer next()
            {
                try
                {
                    return current.next();
                }
                finally
                {
                    Iterator<? extends PropertyContainer> temp = current;
                    current = next;
                    next = temp;
                }
            }

            public void remove()
            {
                next.remove();
            }
        };
    }
}
