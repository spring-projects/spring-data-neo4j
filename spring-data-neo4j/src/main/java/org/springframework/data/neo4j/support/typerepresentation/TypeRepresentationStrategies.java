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
package org.springframework.data.neo4j.support.typerepresentation;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.ClosableIterable;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

/**
 * @author mh
 * @since 12.10.11
 */
public class TypeRepresentationStrategies implements TypeRepresentationStrategy<PropertyContainer> {
    private final Neo4jMappingContext mappingContext;
    private final TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;
    private final TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;

    public TypeRepresentationStrategies(Neo4jMappingContext mappingContext,
                                        TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy,
                                        TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy) {
        this.mappingContext = mappingContext;
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
    }

    @SuppressWarnings("unchecked")
    private <T> TypeRepresentationStrategy<?> getTypeRepresentationStrategy(Class<T> type) {
        if (mappingContext.isNodeEntity(type)) {
            return (TypeRepresentationStrategy<?>) nodeTypeRepresentationStrategy;
        } else if (mappingContext.isRelationshipEntity(type)) {
            return (TypeRepresentationStrategy<?>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not @NodeEntity nor @RelationshipEntity.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T> TypeRepresentationStrategy<S> getTypeRepresentationStrategy(S state, Class<T> type) {
        if (state instanceof Node && mappingContext.isNodeEntity(type)) {
            return (TypeRepresentationStrategy<S>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship && mappingContext.isRelationshipEntity(type)) {
            return (TypeRepresentationStrategy<S>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not @NodeEntity nor @RelationshipEntity.");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer, T> TypeRepresentationStrategy<S> getTypeRepresentationStrategy(S state) {
        if (state instanceof Node) {
            return (TypeRepresentationStrategy<S>) nodeTypeRepresentationStrategy;
        } else if (state instanceof Relationship) {
            return (TypeRepresentationStrategy<S>) relationshipTypeRepresentationStrategy;
        }
        throw new IllegalArgumentException("Type is not @NodeEntity nor @RelationshipEntity.");
    }

    @Override
    public void postEntityCreation(PropertyContainer state, Class<?> type) {
        getTypeRepresentationStrategy(state, type).postEntityCreation(state, type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <U> ClosableIterable<PropertyContainer> findAll(Class<U> type) {
        return (ClosableIterable<PropertyContainer>) getTypeRepresentationStrategy(type).findAll(type);
    }

    @Override
    public long count(Class<?> type) {
        return getTypeRepresentationStrategy(type).count(type);
    }

    @Override
    public <U> Class<U> getJavaType(PropertyContainer state) {
        return getTypeRepresentationStrategy(state).getJavaType(state);
    }

    @Override
    public void preEntityRemoval(PropertyContainer state) {
        getTypeRepresentationStrategy(state).preEntityRemoval(state);
    }

    public TypeRepresentationStrategy<Node> getNodeTypeRepresentationStrategy() {
        return nodeTypeRepresentationStrategy;
    }

    public TypeRepresentationStrategy<Relationship> getRelationshipTypeRepresentationStrategy() {
        return relationshipTypeRepresentationStrategy;
    }
}
