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

import static java.util.Arrays.asList;

import java.util.Arrays;
import java.util.Iterator;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;

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

    public Iterable<Relationship> reverseRelationships() {
        return relationships();
    }

    public Iterable<Node> reverseNodes() {
        return asList(endNode(),startNode());
    }
}
