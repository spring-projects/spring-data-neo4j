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
package org.springframework.data.neo4j.conversion;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Path;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.path.EntityPathPathIterableWrapper;

/**
 * @author mh
 * @since 16.10.11
 */ // todo integrate in result conversion handling
public class TraverserConverter<T> {

    private final Neo4jTemplate ctx;

    public TraverserConverter(Neo4jTemplate ctx) {
        this.ctx = ctx;
    }

    @SuppressWarnings("unchecked")
    public Iterable<T> convert(Traverser traverser, Class<T> targetType) {
        if (Node.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.nodes();
        if (Relationship.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser.relationships();
        if (EntityPath.class.isAssignableFrom(targetType)) return new EntityPathPathIterableWrapper(traverser, ctx);
        if (Path.class.isAssignableFrom(targetType)) return (Iterable<T>) traverser;
        return (Iterable<T>) convertToGraphEntity(traverser, targetType);
    }

    private Iterable<?> convertToGraphEntity(Traverser traverser, final Class<?> targetType) {
        if (ctx.isNodeEntity(targetType)) {
            return new IterableWrapper<Object, Node>(traverser.nodes()) {
                @Override
                protected Object underlyingObjectToObject(Node node) {
                    return ctx.createEntityFromState(node, targetType);
                }
            };
        }
        if (ctx.isRelationshipEntity(targetType)) {
            return new IterableWrapper<Object, Relationship>(traverser.relationships()) {
                @Override
                protected Object underlyingObjectToObject(Relationship relationship) {
                    return ctx.createEntityFromState(relationship, targetType);
                }
            };
        }
        throw new IllegalStateException("Can't determine valid type for traversal target " + targetType);

    }
}
