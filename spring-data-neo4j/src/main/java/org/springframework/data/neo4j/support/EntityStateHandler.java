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
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntityImpl;
import org.springframework.data.neo4j.mapping.RelationshipProperties;

/**
 * @author mh
 * @since 02.10.11
 */
public class EntityStateHandler {

    private Neo4jMappingContext mappingContext;
    private final GraphDatabase graphDatabase;

    public EntityStateHandler(Neo4jMappingContext mappingContext, GraphDatabase graphDatabase) {
        this.mappingContext = mappingContext;
        this.graphDatabase = graphDatabase;
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

    public boolean hasPersistentState(Object entity) {
        if (entity instanceof PropertyContainer) return true;
        if (isManaged(entity)) return ((ManagedEntity) entity).getPersistentState() != null;
        return getId(entity) != null;
    }

    private Number getId(Object entity) {
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        final Object id = persistentEntity.getPersistentId(entity);
        if (id == null) return null; // todo create new node?
        if (id instanceof Number) {
            return ((Number) id);
        }
        throw new IllegalArgumentException("The id of " + persistentEntity.getEntityName() + " " + persistentEntity.getIdProperty() + " is not a number");
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S getPersistentState(Object entity) {
        if (entity instanceof PropertyContainer) {
            return (S) entity;
        }
        if (isManaged(entity)) {
            return ((ManagedEntity<S, Object>) entity).getPersistentState();
        }
        final Number id = getId(entity);
        if (id == null) return null;
        long graphId = id.longValue();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(entity.getClass());
        if (persistentEntity.isNodeEntity()) {
            return (S) graphDatabase.getNodeById(graphId);
        }
        if (persistentEntity.isRelationshipEntity()) {
            return (S) graphDatabase.getRelationshipById(graphId);
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
        if (containedState != null) return containedState;
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        if (persistentEntity.isNodeEntity()) {
            return (S) graphDatabase.createNode(null);
        }
        if (persistentEntity.isRelationshipEntity()) {
            return createRelationship(entity, persistentEntity);
        }
        throw new IllegalArgumentException("The entity " + persistentEntity.getEntityName() + " has to be either annotated with @NodeEntity or @RelationshipEntity");
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer> S createRelationship(Object entity, Neo4jPersistentEntityImpl<?> persistentEntity) {
        final RelationshipProperties relationshipProperties = persistentEntity.getRelationshipProperties();
        Node startNode = (Node) getPersistentState(relationshipProperties.getStartNodeProperty().getValue(entity));
        Node endNode = (Node) getPersistentState(relationshipProperties.getEndeNodeProperty().getValue(entity));
        Object relType = relationshipProperties.getTypeProperty().getValue(entity);
        if (relType instanceof RelationshipType) {
            return (S) startNode.createRelationshipTo(endNode, (RelationshipType) relType);
        }
        return (S) startNode.createRelationshipTo(endNode, DynamicRelationshipType.withName(relType.toString()));
    }

    public RelationshipResult relateTo(Object source, Object target, String type) {
        return this.createRelationshipBetween(source, target, type, false);
    }

    // todo gdc.postEntityCreation(rel), return createEntityFromState(rel)
    public RelationshipResult createRelationshipBetween(Object source, Object target, String type, boolean allowDuplicates) {
        if (source == null) throw new IllegalArgumentException("Source entity is null");
        if (target == null) throw new IllegalArgumentException("Target entity is null");
        if (type == null) throw new IllegalArgumentException("Relationshiptype is null");

        if (!allowDuplicates) {
            Relationship relationship = getRelationshipBetween(source, target, type);
            if (relationship != null) return new RelationshipResult(relationship, RelationshipResult.Type.EXISTING);
        }

        final Node sourceNode = getPersistentState(source, Node.class);
        final Node targetNode = getPersistentState(target, Node.class);

        if (sourceNode == null) throw new IllegalArgumentException("Source Node  is null");
        if (targetNode == null) throw new IllegalArgumentException("Target Node is null");

        final Relationship relationship = sourceNode.createRelationshipTo(targetNode, DynamicRelationshipType.withName(type));
        return new RelationshipResult(relationship, RelationshipResult.Type.NEW);
    }

    @SuppressWarnings("unchecked")
    public <R extends PropertyContainer> R getPersistentState(Object entity, Class<R> type) {
        final PropertyContainer state = getPersistentState(entity);
        if (type==null || type.isInstance(state)) return (R)state;
        throw new IllegalArgumentException("Target state is not the requested "+type+" but "+state);
    }

    public RelationshipResult removeRelationshipTo(Object source, Object target, String relationshipType) {
        final Relationship relationship = getRelationshipBetween(source, target, relationshipType);
        if (relationship!=null) {
           relationship.delete();
           return new RelationshipResult(relationship, RelationshipResult.Type.DELETED);
        }
        return null;
    }

    public Relationship getRelationshipBetween(Object source, Object target, String type) {
        if (source == null) throw new IllegalArgumentException("Source entity is null");
        if (target == null) throw new IllegalArgumentException("Target entity is null");
        if (type == null) throw new IllegalArgumentException("Relationshiptype is null");
        Node node = getPersistentState(source);
        Node targetNode = getPersistentState(target);
        if (node == null || targetNode == null) return null;
        Iterable<Relationship> relationships = node.getRelationships(DynamicRelationshipType.withName(type),Direction.OUTGOING);
        for (Relationship relationship : relationships) {
            if (relationship.getOtherNode(node).equals(targetNode)) return relationship;
        }
        return null;
    }

    public final boolean equals(Object first, Object second) {
        if (second == first) return true;
        if (second == null) return false;
        final PropertyContainer firstState = getPersistentState(first);
        if (firstState == null) return false;
        final PropertyContainer secondState = getPersistentState(second);
        if (secondState == null) return false;
        return firstState.equals(secondState);
    }

    /**
     * @return result of the hashCode of the underlying node (if any, otherwise identityHashCode)
     */
    public final int hashCode(Object entity) {
        if (entity == null) throw new IllegalArgumentException("Entity is null");
        final PropertyContainer state = getPersistentState(entity);
        if (state == null) return System.identityHashCode(entity);
        return state.hashCode();
    }


}
