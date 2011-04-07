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

package org.springframework.data.graph.neo4j.rest.support;

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
