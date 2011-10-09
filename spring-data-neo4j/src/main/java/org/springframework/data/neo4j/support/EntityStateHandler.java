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

import org.neo4j.graphdb.*;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.mapping.RelationshipProperties;

/**
 * @author mh
 * @since 02.10.11
 */
public class EntityStateHandler {

    private Neo4jMappingContext mappingContext;
    private final GraphDatabaseService service;

    public EntityStateHandler(Neo4jMappingContext mappingContext, GraphDatabaseService service) {
        this.mappingContext = mappingContext;
        this.service = service;
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> void setPersistentState(Object entity, S state) {
        if (entity instanceof PropertyContainer) {
            return;
        }
        if (isManaged(entity)) {
            ((ManagedEntity<S, Object>) entity).setPersistentState(state);
            return;
        }
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        persistentEntity.setPersistentState(entity, state);
    }

    public boolean isManaged(Object entity) {
        return entity instanceof ManagedEntity;
    }

    public boolean isManaged(Class type) {
        return ManagedEntity.class.isAssignableFrom(type);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        if (entity instanceof PropertyContainer) {
            return (S) entity;
        }
        if (isManaged(entity)) {
            return ((ManagedEntity<S, Object>) entity).getPersistentState();
        }
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        final Object id = persistentEntity.getPersistentId(entity);
        if (id == null) return null; // todo create new node?
        if (!(id instanceof Number))
            throw new IllegalArgumentException("The id of " + persistentEntity.getEntityName() + " " + persistentEntity.getIdProperty() + " is not a number");
        long graphId = ((Number) id).longValue();
        if (isNodeEntity(type)) {
            return (S) service.getNodeById(graphId);
        }
        if (isRelationshipEntity(type)) {
            return (S) service.getRelationshipById(graphId);
        }
        throw new IllegalArgumentException("The entity " + persistentEntity.getEntityName() + " has to be either annotated with @NodeEntity or @RelationshipEntity");
    }

    public boolean isNodeEntity(Class<?> targetType) {
        return mappingContext.isNodeEntity(targetType);
    }

    public boolean isRelationshipEntity(Class targetType) {
        return mappingContext.isRelationshipEntity(targetType);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S useOrCreateState(Object entity, S state) {
        if (state != null) return state;
        final S containedState = getPersistentState(entity);
        if (containedState == null) return containedState;
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        if (persistentEntity.isNodeEntity()) {
            return (S) service.createNode();
        }
        if (persistentEntity.isRelationshipEntity()) {
            return createRelationship(entity, persistentEntity);
        }
        throw new IllegalArgumentException("The entity " + persistentEntity.getEntityName() + " has to be either annotated with @NodeEntity or @RelationshipEntity");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer> S createRelationship(Object entity, Neo4jPersistentEntityImpl<?> persistentEntity) {
        final RelationshipProperties relationshipProperties = persistentEntity.getRelationshipProperties();
        Node startNode = (Node) relationshipProperties.getStartNodeProperty().getValue(entity);
        Node endNode = (Node) relationshipProperties.getStartNodeProperty().getValue(entity);
        Object relType = relationshipProperties.getTypeProperty().getValue(entity);
        if (relType instanceof RelationshipType) {
            return (S) startNode.createRelationshipTo(endNode, (RelationshipType) relType);
        }
        return (S) startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(relType.toString()));
    }
}
