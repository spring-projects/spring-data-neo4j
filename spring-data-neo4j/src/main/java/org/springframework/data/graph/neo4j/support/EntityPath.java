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

package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.relationship.DefaultRelationshipBacked;

import java.util.Iterator;

/**
* @author mh
* @since 26.02.11
*/
public class EntityPath<S extends NodeBacked,E extends NodeBacked> implements Path {

    public <T extends NodeBacked> T startEntity(Class<T>...types) {
        return createNodeEntityFromFirstParameterOrStoredType(startNode(), types);
    }

    private <T extends NodeBacked> T createNodeEntityFromFirstParameterOrStoredType(Node node, Class<T>...types) {
        if (node==null) return null;
        if (types==null || types.length==0) return graphDatabaseContext.createEntityFromStoredType(node);
        return graphDatabaseContext.createEntityFromState(node,types[0]);
    }

    public <T extends NodeBacked> T endEntity(Class<T>...types) {
        return createNodeEntityFromFirstParameterOrStoredType(endNode(),types);
    }
    public <T extends RelationshipBacked> T lastRelationshipEntity(Class<T>...types) {
        Relationship relationship = lastRelationship();
        if (relationship==null) return null;
        return graphDatabaseContext.createEntityFromState(relationship, getFirstOrDefault((Class<T>) DefaultRelationshipBacked.class, types));
    }

    private static <T> T getFirstOrDefault(final T defaultValue, T... values) {
        if (values == null || values.length == 0) return defaultValue;
        else return values[0];
    }

    public <T extends NodeBacked> Iterable<T> nodeEntities() {
        return new IterableWrapper<T,Node>(nodes()) {
            @Override
            protected T underlyingObjectToObject(Node node) {
                return graphDatabaseContext.createEntityFromStoredType(node);
            }
        };
    }

    public <T extends RelationshipBacked> Iterable<T> relationshipEntities(final Class<T>...relationships) {
        return new IterableWrapper<T,Relationship>(relationships()) {
            @Override
            protected T underlyingObjectToObject(Relationship relationship) {
                return graphDatabaseContext.createEntityFromState(relationship, getFirstOrDefault((Class<T>)DefaultRelationshipBacked.class, relationships));
            }
        };
    }

    public <T extends GraphBacked> Iterable<T> allPathEntities(final Class<T>...relationships) {
        return new IterableWrapper<T,PropertyContainer>(delegate) {
            @Override
            protected T underlyingObjectToObject(PropertyContainer element) {
                return graphDatabaseContext.createEntityFromState(element, getFirstOrDefault((Class<T>)DefaultRelationshipBacked.class, relationships));
            }
        };
    }


    public EntityPath(GraphDatabaseContext graphDatabaseContext, Path delegate) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.delegate = delegate;
    }

    private final GraphDatabaseContext graphDatabaseContext;
    private final Path delegate;

    @Override
    public Node startNode() {
        return delegate.startNode();
    }

    @Override
    public Node endNode() {
        return delegate.endNode();
    }

    @Override
    public Relationship lastRelationship() {
        return delegate.lastRelationship();
    }

    @Override
    public Iterable<Relationship> relationships() {
        return delegate.relationships();
    }

    @Override
    public Iterable<Node> nodes() {
        return delegate.nodes();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public Iterator<PropertyContainer> iterator() {
        return delegate.iterator();
    }
}
