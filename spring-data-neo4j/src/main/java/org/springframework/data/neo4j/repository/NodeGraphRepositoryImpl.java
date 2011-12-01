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

package org.springframework.data.neo4j.repository;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NodeGraphRepositoryImpl<T> extends AbstractGraphRepository<Node, T> implements GraphRepository<T>, RelationshipOperationsRepository<T> {

    public NodeGraphRepositoryImpl(final Class<T> clazz, final Neo4jTemplate template) {
        super(template, clazz);
    }

    @Override
    protected Node getById(long id) {
        return template.getNode(id);
    }

    @Override
    public <N> Iterable<T> findAllByTraversal(final N start, final TraversalDescription traversalDescription) {
        return template.traverse(start, clazz, traversalDescription);
    }

    @Override
    public <R> R createRelationshipBetween(T start, Object end, Class<R> relationshipEntityClass, String relationshipType) {
        return template.createRelationshipBetween(start,end,relationshipEntityClass,relationshipType,false);
    }

    @Override
    public <R> R getRelationshipBetween(T start, Object end, Class<R> relationshipEntityClass, String relationshipType) {
        return template.getRelationshipBetween(start,end,relationshipEntityClass,relationshipType);
    }

    @Override
    public void deleteRelationshipBetween(T start, Object end, String type) {
        template.deleteRelationshipBetween(start,end,type);
    }
}

