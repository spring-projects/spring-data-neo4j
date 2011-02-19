package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

import java.util.Arrays;
import java.util.Iterator;

import static java.util.Arrays.asList;

/**
 * @author mh
 * @since 19.02.11
 */
public class RelationshipPath implements Path {
    private final Relationship relationship;

    public RelationshipPath(Relationship relationship) {
        this.relationship = relationship;
    }

    @Override
    public Node startNode() {
        return relationship.getStartNode();
    }

    @Override
    public Node endNode() {
        return relationship.getEndNode();
    }

    @Override
    public Relationship lastRelationship() {
        return relationship;
    }

    @Override
    public Iterable<Relationship> relationships() {
        return asList(relationship);
    }

    @Override
    public Iterable<Node> nodes() {
        return asList(startNode(),endNode());
    }

    @Override
    public int length() {
        return 1;
    }

    @Override
    public Iterator<PropertyContainer> iterator() {
        return Arrays.<PropertyContainer>asList(startNode(), lastRelationship(), endNode()).iterator();
    }
}
