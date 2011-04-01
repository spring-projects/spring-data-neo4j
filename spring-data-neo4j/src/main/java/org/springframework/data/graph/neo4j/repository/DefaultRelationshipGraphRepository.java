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

package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

public class DefaultRelationshipGraphRepository<T extends RelationshipBacked> extends AbstractGraphRepository<Relationship, T> implements RelationshipGraphRepository<T> {

    public DefaultRelationshipGraphRepository(final Class<T> clazz, final GraphDatabaseContext graphDatabaseContext) {
        super(graphDatabaseContext, clazz);
    }

    @Override
    protected Relationship getById(long id) {
        return graphDatabaseContext.getRelationshipById(id);
    }

    @Override
    public <N extends NodeBacked> Iterable<T> findAllByTraversal(final N startNode, final TraversalDescription traversalDescription) {
        throw new UnsupportedOperationException("Traversal not able to start at relationship");
    }

    @Override
    public T save(T entity) {
        return entity;
    }

    @Override
    public Iterable<T> save(Iterable<? extends T> entities) {
        return (Iterable<T>) entities;
    }
}

