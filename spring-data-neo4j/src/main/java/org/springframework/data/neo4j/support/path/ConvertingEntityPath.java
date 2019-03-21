/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.support.path;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.mapping.EntityPersister;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Iterator;

/**
* @author mh
* @since 26.02.11
*/
@SuppressWarnings("unchecked") // TODO DefaultNode/RelationshipBacked
public class ConvertingEntityPath<S,E> implements EntityPath<S,E> {

    private final Neo4jTemplate template;

    @Override
    public <T> T startEntity(Class<T>... types) {
        return projectEntityToFirstParameterOrCreateFromStoredType(startNode(), types);
    }

    private <T> T projectEntityToFirstParameterOrCreateFromStoredType(Node node, Class<T>... types) {
        if (node==null) return null;
        if (types==null || types.length==0) return template.createEntityFromStoredType(node, MappingPolicy.LOAD_POLICY);
        return template.projectTo(node, types[0]);
    }

    @Override
    public <T> T endEntity(Class<T>... types) {
        return projectEntityToFirstParameterOrCreateFromStoredType(endNode(), types);
    }
    @Override
    public <T> T lastRelationshipEntity(Class<T>... types) {
        Relationship relationship = lastRelationship();
        if (relationship==null) return null;
        return template.projectTo(relationship, getFirstOrDefault((Class<T>) DefaultRelationshipBacked.class, types));
    }

    private static <T> T getFirstOrDefault(final T defaultValue, T... values) {
        if (values == null || values.length == 0) return defaultValue;
        else return values[0];
    }

    @Override
    public <T> Iterable<T> nodeEntities() {
        return new IterableWrapper<T,Node>(nodes()) {
            @Override
            protected T underlyingObjectToObject(Node node) {
                return template.createEntityFromStoredType(node, null);
            }
        };
    }

    @Override
    public <T> Iterable<T> relationshipEntities(final Class<T>... relationships) {
        return new IterableWrapper<T,Relationship>(relationships()) {
            @Override
            protected T underlyingObjectToObject(Relationship relationship) {
                return template.projectTo(relationship, getFirstOrDefault((Class<T>) DefaultRelationshipBacked.class, relationships));
            }
        };
    }

    @Override
    public <T> Iterable<T> allPathEntities(final Class<T>... relationships) {
        return new IterableWrapper<T,PropertyContainer>(delegate) {
            @Override
            protected T underlyingObjectToObject(PropertyContainer element) {
                return template.projectTo(element, getFirstOrDefault((Class<T>) DefaultRelationshipBacked.class, relationships));
            }
        };
    }


    public ConvertingEntityPath(Path delegate, Neo4jTemplate template) {
        this.delegate = delegate;
        this.template = template;
    }

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
    public Iterable<Relationship> reverseRelationships() {
        return delegate.reverseRelationships();
    }

    @Override
    public Iterable<Node> reverseNodes() {
        return delegate.reverseNodes();
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
