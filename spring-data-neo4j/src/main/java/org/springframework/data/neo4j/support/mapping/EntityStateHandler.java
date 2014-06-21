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
package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipProperties;
import org.springframework.data.neo4j.mapping.RelationshipResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.neo4j.helpers.collection.MapUtil.map;

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
        return getPersistentState(entity,true);
    }
    public <S extends PropertyContainer> S getPersistentState(Object entity, boolean check) {
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
        if (check) throw new IllegalArgumentException("The entity " + persistentEntity.getEntityName() + " has to be either annotated with @NodeEntity or @RelationshipEntity");
        return null;
    }

    public boolean isNodeEntity(Class<?> targetType) {
        return mappingContext.isNodeEntity(targetType);
    }

    public boolean isRelationshipEntity(Class targetType) {
        return mappingContext.isRelationshipEntity(targetType);
    }

    @SuppressWarnings("unchecked")
    public <S extends PropertyContainer> S useOrCreateState( Object entity, S state, RelationshipType
            annotationProvidedRelationshipType ) {
        if (state != null) return state;
        final S containedState = getPersistentState(entity);
        if (containedState != null) return containedState;
        final Class<?> type = entity.getClass();
        final Neo4jPersistentEntityImpl<?> persistentEntity = mappingContext.getPersistentEntity(type);
        final MappingPolicy mappingPolicy = persistentEntity.getMappingPolicy();
        // todo observe load policy
        if (persistentEntity.isNodeEntity()) {
            if (persistentEntity.isUnique()) return (S)createUniqueNode(persistentEntity,entity);
            return createNode(persistentEntity);
        }
        if (persistentEntity.isRelationshipEntity()) {
            return getOrCreateRelationship(entity, persistentEntity, annotationProvidedRelationshipType );
        }
        throw new IllegalArgumentException("The entity " + persistentEntity.getEntityName() + " has to be either annotated with @NodeEntity or @RelationshipEntity");
    }

    private <S extends PropertyContainer> S createNode(Neo4jPersistentEntityImpl<?> persistentEntity) {
        return (S) graphDatabase.createNode(null,persistentEntity.getAllLabels());
    }

    private Node createUniqueNode(Neo4jPersistentEntityImpl<?> persistentEntity, Object entity) {
        Neo4jPersistentProperty uniqueProperty = persistentEntity.getUniqueProperty();
        final IndexInfo indexInfo = uniqueProperty.getIndexInfo();
        final Object value = uniqueProperty.getValueFromEntity(entity, MappingPolicy.MAP_FIELD_DIRECT_POLICY);
        if (value==null) throw new MappingException("Error creating "+uniqueProperty.getOwner().getName()+" with "+entity+" unique property "+uniqueProperty.getName()+" has null value");
        if (indexInfo.isLabelBased()) {
            return (indexInfo.isFailOnDuplicate())
                    ? graphDatabase.createNode(map(uniqueProperty.getName(),value),persistentEntity.getAllLabels())
                    : graphDatabase.merge(indexInfo.getIndexName(), indexInfo.getIndexKey(), value, Collections.<String,Object>emptyMap(), persistentEntity.getAllLabels());
        } else {
            return graphDatabase.getOrCreateNode(indexInfo.getIndexName(), indexInfo.getIndexKey(), value, Collections.<String,Object>emptyMap(),persistentEntity.getAllLabels());
        }
    }

    @SuppressWarnings("unchecked")
    private <S extends PropertyContainer> S getOrCreateRelationship( Object entity, Neo4jPersistentEntity<?> persistentEntity, RelationshipType annotationProvidedRelationshipType ) {
        final RelationshipProperties relationshipProperties = persistentEntity.getRelationshipProperties();
        final Neo4jPersistentProperty startNodeProperty = relationshipProperties.getStartNodeProperty();
        Node startNode = (Node) getPersistentState(startNodeProperty.getValue(entity, startNodeProperty.getMappingPolicy()));
        final Neo4jPersistentProperty endNodeProperty = relationshipProperties.getEndNodeProperty();
        Node endNode = (Node) getPersistentState(endNodeProperty.getValue(entity, endNodeProperty.getMappingPolicy()));
        RelationshipType relationshipType = getRelationshipType(persistentEntity,entity, annotationProvidedRelationshipType );
        if (persistentEntity.isUnique()) {
            final Neo4jPersistentProperty uniqueProperty = persistentEntity.getUniqueProperty();
            final IndexInfo indexInfo = uniqueProperty.getIndexInfo();
            final Object value = uniqueProperty.getValueFromEntity(entity, MappingPolicy.MAP_FIELD_DIRECT_POLICY);
            if (value == null) {
                throw new MappingException("Error creating "+uniqueProperty.getOwner().getName()+" with "+entity+" unique property "+uniqueProperty.getName()+" has null value");
            }
            return (S) graphDatabase.getOrCreateRelationship(indexInfo.getIndexName(),indexInfo.getIndexKey(), value, startNode,endNode,relationshipType.name(), Collections.<String,Object>emptyMap());
        }
        return (S) graphDatabase.createRelationship(startNode, endNode, relationshipType, Collections.<String,Object>emptyMap());
    }

    private RelationshipType getRelationshipType( Neo4jPersistentEntity persistentEntity, Object entity,
                                                  RelationshipType annotationProvidedRelationshipType )
    {
        final RelationshipProperties relationshipProperties = persistentEntity.getRelationshipProperties();
        final Neo4jPersistentProperty typeProperty = relationshipProperties.getTypeProperty();

        if ( typeProperty != null )
        {
            Object value = typeProperty.getValue( entity, typeProperty.getMappingPolicy() );

            if ( value != null )
            {
                return value instanceof RelationshipType ? (RelationshipType) value : DynamicRelationshipType
                        .withName( value.toString() );
            }
        }

        if ( annotationProvidedRelationshipType != null )
        {
            return annotationProvidedRelationshipType;
        }

        String relationshipTypeAsString = relationshipProperties.getRelationshipType();

        if ( relationshipTypeAsString == null )
        {
            throw new MappingException( "Could not determine relationship-type for " + persistentEntity.getName() );
        }

        return DynamicRelationshipType.withName( relationshipTypeAsString );
    }

    public RelationshipResult relateTo(Object source, Object target, String type) {
        return this.createRelationshipBetween(source, target, type, false);
    }

    // todo gdc.postEntityCreation(rel), return createEntityFromState(rel)
    public RelationshipResult createRelationshipBetween(Object source, Object target, String type, boolean allowDuplicates) {
        if (source == null) throw new IllegalArgumentException("Source entity is null");
        if (target == null) throw new IllegalArgumentException("Target entity is null");
        if (type == null) throw new IllegalArgumentException("Relationshiptype is null");

        final Node sourceNode = getPersistentState(source, Node.class);
        final Node targetNode = getPersistentState(target, Node.class);

        if (!allowDuplicates) {
            Relationship relationship = getRelationshipBetween(sourceNode, targetNode, type);
            if (relationship != null) return new RelationshipResult(relationship, RelationshipResult.Type.EXISTING);
        }


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
           graphDatabase.remove(relationship);
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
            if (relationship.getEndNode().equals(targetNode)) return relationship;
        }
        return null;
    }

    public final boolean equals(Object first, Object second) {
        if (second == first) return true;
        if (second == null) return false;
        final PropertyContainer firstState = getPersistentState(first,false);
        if (firstState == null) return false;
        final PropertyContainer secondState = getPersistentState(second,false);
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


    public Iterable<Relationship> getRelationshipsBetween(Object source, Object target, String type) {
        if (source == null) throw new IllegalArgumentException("Source entity is null");
        if (target == null) throw new IllegalArgumentException("Target entity is null");
        if (type == null) throw new IllegalArgumentException("Relationshiptype is null");
        Node node = getPersistentState(source);
        Node targetNode = getPersistentState(target);
        if (node == null || targetNode == null) return null;
        List<Relationship> result=new ArrayList<Relationship>();
        for (Relationship relationship : node.getRelationships(DynamicRelationshipType.withName(type),Direction.OUTGOING)) {
            if (relationship.getEndNode().equals(targetNode)) result.add(relationship);
        }
        return result;
    }
}
