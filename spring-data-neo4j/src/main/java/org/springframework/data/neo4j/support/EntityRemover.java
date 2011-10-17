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
package org.springframework.data.neo4j.support;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;

/**
* @author mh
* @since 12.10.11
*/
public class EntityRemover {

    private EntityStateHandler entityStateHandler;
    private TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy;
    private TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy;
    private final GraphDatabase graphDatabase;

    public EntityRemover(EntityStateHandler entityStateHandler, TypeRepresentationStrategy<Node> nodeTypeRepresentationStrategy, TypeRepresentationStrategy<Relationship> relationshipTypeRepresentationStrategy, GraphDatabase graphDatabase) {
        this.entityStateHandler = entityStateHandler;
        this.nodeTypeRepresentationStrategy = nodeTypeRepresentationStrategy;
        this.relationshipTypeRepresentationStrategy = relationshipTypeRepresentationStrategy;
        this.graphDatabase = graphDatabase;
    }

    public void removeNodeEntity(Object entity) {
        Node node = entityStateHandler.getPersistentState(entity, Node.class);
        if (node == null) return;
        nodeTypeRepresentationStrategy.preEntityRemoval(node);
        for (Relationship relationship : node.getRelationships()) {
            removeRelationship(relationship);
        }
        graphDatabase.remove(node);
    }

    public void removeRelationshipEntity(Object entity) {
        Relationship relationship = entityStateHandler.getPersistentState(entity, Relationship.class);
        if (relationship == null) return;
        removeRelationship(relationship);
    }

    private void removeRelationship(Relationship relationship) {
        relationshipTypeRepresentationStrategy.preEntityRemoval(relationship);
        graphDatabase.remove(relationship);
    }

    public void removeRelationshipBetween(Object start, Object target, String type) {
        final RelationshipResult result = entityStateHandler.removeRelationshipTo(start, target, type);
        if (result!=null && result.type == RelationshipResult.Type.DELETED) {
            relationshipTypeRepresentationStrategy.preEntityRemoval(result.relationship);
        }
    }

    public void remove(Object entity) {
        if (entity instanceof Node) {
            graphDatabase.remove((Node)entity);
            return;
        }
        if (entity instanceof Relationship) {
            graphDatabase.remove((Relationship)entity);
            return;
        }
        final Class<?> type = entity.getClass();
        if (entityStateHandler.isNodeEntity(type)) {
            removeNodeEntity(entity);
            return;
        }
        if (entityStateHandler.isRelationshipEntity(type)) {
            removeRelationshipEntity(entity);
            return;
        }
        throw new IllegalArgumentException("@NodeEntity or @RelationshipEntity annotation required on domain class"+type);
    }
}
