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

package org.springframework.data.graph.neo4j.support.path;

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
