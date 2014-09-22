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
import java.util.Collection;
import java.util.Map;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalMetadata;
import org.neo4j.graphdb.traversal.Traverser;

import org.neo4j.rest.graphdb.util.WrappingResourceIterator;
import org.neo4j.rest.graphdb.RestAPI;
import org.neo4j.rest.graphdb.util.ResourceIterableWrapper;

/**
 * @author Michael Hunger
 * @since 03.02.11
 */
public class RestTraverser implements Traverser {
    private final Collection<Path> paths;
    public RestTraverser(Collection col, RestAPI restApi) {
        this.paths = parseToPaths(col, restApi);
    }

    private Collection<Path> parseToPaths(Collection col, RestAPI restApi) {
        Collection<Path> result=new ArrayList<Path>(col.size());
        for (Object path : col) {
            if (!(path instanceof Map)) throw new RuntimeException("Expected Map for Path representation but got: "+(path!=null ? path.getClass() : null));
            result.add(RestPathParser.parse((Map) path, restApi));
        }
        return result;
    }

    public ResourceIterable<Node> nodes() {
        return new ResourceIterableWrapper<Node, Path>(paths) {
            @Override
            protected Node underlyingObjectToObject(Path path) {
                return path.endNode();
            }
        };
    }

    public ResourceIterable<Relationship> relationships() {
        return new ResourceIterableWrapper<Relationship, Path>(paths) {
            @Override
            protected Relationship underlyingObjectToObject(Path path) {
                return path.lastRelationship();
            }
        };
    }

    public ResourceIterator<Path> iterator() {
        return new WrappingResourceIterator<Path>(paths.iterator());
    }

    @Override
    public TraversalMetadata metadata() {
        throw new UnsupportedOperationException();
    }
}
