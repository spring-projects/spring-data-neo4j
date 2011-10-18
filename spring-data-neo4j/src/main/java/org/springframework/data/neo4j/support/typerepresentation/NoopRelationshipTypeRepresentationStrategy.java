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

import org.neo4j.graphdb.Relationship;
import org.neo4j.helpers.collection.ClosableIterable;

import org.springframework.data.neo4j.core.RelationshipTypeRepresentationStrategy;
import org.springframework.data.neo4j.mapping.EntityInstantiator;

public class NoopRelationshipTypeRepresentationStrategy implements RelationshipTypeRepresentationStrategy {

    private final EntityInstantiator<Relationship> relationshipEntityInstantiator;

    public NoopRelationshipTypeRepresentationStrategy(EntityInstantiator<Relationship> relationshipEntityInstantiator) {
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

    @Override
    public void postEntityCreation(Relationship state, Class<?> type) {
    }

    @Override
    public <U> ClosableIterable<U> findAll(Class<U> clazz) {
        throw new UnsupportedOperationException("findAll not supported.");
    }

    @Override
    public long count(Class<?> entityClass) {
        throw new UnsupportedOperationException("count not supported.");
    }

    @Override
    public void preEntityRemoval(Relationship state) {
    }

    @Override
    public Class<?> getJavaType(Relationship state) {
        throw new UnsupportedOperationException("getJavaType not supported.");
    }

    @Override
    public <U> U createEntity(Relationship state) {
        throw new UnsupportedOperationException("Creation with stored type not supported.");
    }

    @Override
    public <U> U createEntity(Relationship state, Class<U> type) {
        return projectEntity(state, type);
    }

    @Override
    public <U> U projectEntity(Relationship state, Class<U> type) {
        return relationshipEntityInstantiator.createEntityFromState(state, type);
    }
}
