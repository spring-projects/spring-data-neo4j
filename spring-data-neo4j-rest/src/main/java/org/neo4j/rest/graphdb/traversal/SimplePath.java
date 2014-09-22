/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IteratorUtil;

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

    public Iterable<Relationship> reverseRelationships() {
        List<Relationship> reverseRels = IteratorUtil.addToCollection(relationships, new ArrayList<Relationship>());
        Collections.reverse(reverseRels);
        return reverseRels;
    }

    public Iterable<Node> reverseNodes() {
        List<Node> reverseNodes = IteratorUtil.addToCollection(nodes, new ArrayList<Node>());
        Collections.reverse(reverseNodes);
        return reverseNodes;
    }
}
