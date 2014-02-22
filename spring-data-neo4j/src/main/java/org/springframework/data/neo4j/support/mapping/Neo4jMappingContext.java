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

import org.neo4j.graphdb.PropertyContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import static java.lang.String.format;

/**
 * Neo4J specific {@link MappingContext} implementation. Simply creates {@link Neo4jPersistentEntityImpl} and
 * {@link org.springframework.data.neo4j.mapping.Neo4jPersistentProperty} instances.
 *
 * @author Oliver Gierke
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntityImpl<?>, Neo4jPersistentProperty> {

    private final static Logger log = LoggerFactory.getLogger(Neo4jMappingContext.class);

    private final Map<Annotation, Boolean> referenceAnnotations = new IdentityHashMap<Annotation, java.lang.Boolean>();
    
    protected <T> Neo4jPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        final Class<T> type = typeInformation.getType();
        if (type.isAnnotationPresent(NodeEntity.class)) {
            return new Neo4jPersistentEntityImpl<T>(typeInformation);
        }
        if (type.isAnnotationPresent(RelationshipEntity.class)) {
            return new Neo4jPersistentEntityImpl<T>(typeInformation);
        }
        throw new InvalidEntityTypeException("Type " + type + " is neither a @NodeEntity nor a @RelationshipEntity");
    }

    @Override
    protected Neo4jPersistentEntityImpl<?> addPersistentEntity(TypeInformation<?> typeInformation) {
        final Neo4jPersistentEntityImpl<?> entity = super.addPersistentEntity(typeInformation);
        Collection<Neo4jPersistentEntity<?>> superTypeEntities = addSuperTypes(entity);
        entity.updateStoredType(new StoredEntityType(entity,superTypeEntities,entityAlias));
        return entity;
    }

    private List<Neo4jPersistentEntity<?>> addSuperTypes(Neo4jPersistentEntity<?> entity) {
        List<Neo4jPersistentEntity<?>> entities=new ArrayList<Neo4jPersistentEntity<?>>();
        final Class<?> type = entity.getType();
        entities.addAll(addPersistentEntityWithCheck(type.getSuperclass()));
        for (Class<?> anInterface : type.getInterfaces()) {
            entities.addAll(addPersistentEntityWithCheck(anInterface));
        }
        return entities;
    }

    private Collection<Neo4jPersistentEntity<?>> addPersistentEntityWithCheck(Class<?> type) {
        if (type != null && (isNodeEntityType(type) || isRelationshipEntityType(type))) {
            return Collections.<Neo4jPersistentEntity<?>>singletonList(addPersistentEntity(type));
        }
        return Collections.emptyList();
    }

    private boolean isRelationshipEntityType(Class<?> type) {
        return type.isAnnotationPresent(RelationshipEntity.class);
    }

    private boolean isNodeEntityType(Class<?> type) {
        return type.isAnnotationPresent(NodeEntity.class);
    }

    // todo performance, change to direct lookups if too slow
    public Neo4jPersistentEntity<?> getPersistentEntity(Object alias) {
        for (Neo4jPersistentEntityImpl<?> entity : getPersistentEntities()) {
            if (entity.matchesAlias(alias)) return entity;
        }
        return tryToResolveAliasAsEntityClassName(alias);
    }

    private Neo4jPersistentEntity<?> tryToResolveAliasAsEntityClassName(Object alias) {
        if (alias instanceof Class) {
            try {
                return getPersistentEntity((Class)alias);
            } catch(MappingException me) {
                // ignores
            }
        }
        if (alias instanceof String && alias.toString().contains(".")) {
            try {
                return tryToResolveAliasAsEntityClassName(Class.forName(alias.toString()));
            } catch (ClassNotFoundException cnfe) {
                // ignore
            }
        }
        if (alias instanceof StoredEntityType) {
            return ((StoredEntityType)alias).getEntity();
        }
        return null;
    }

    public StoredEntityType getStoredEntityType(Class type) {
       return getPersistentEntity(type).getEntityType();
    }

    @Override
    protected Neo4jPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
                                                               Neo4jPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new Neo4jPersistentPropertyImpl(field, descriptor, owner, simpleTypeHolder,this);
    }

    private final Map<Class<?>,Class<?>> annotationCheckCache = new IdentityHashMap<Class<?>, Class<?>>();

    public boolean isNodeEntity(Class<?> type) {
        if (!annotationCheckCache.containsKey(type)) cacheType(type);
        return checkAnnotationType(type,NodeEntity.class);
    }

    private void cacheType(Class<?> type) {
        try {
            final Neo4jPersistentEntityImpl<?> entity = getPersistentEntity(type);
            if (entity == null) annotationCheckCache.put(type, type);
            else if (entity.isNodeEntity()) annotationCheckCache.put(type, NodeEntity.class);
            else if (entity.isRelationshipEntity()) annotationCheckCache.put(type, RelationshipEntity.class);
            else annotationCheckCache.put(type, type);
        } catch (InvalidEntityTypeException me) {
            annotationCheckCache.put(type, type);
        }
    }

    private boolean checkAnnotationType(Class<?> type, Class<?> annotation) {
        final Class<?> marker = annotationCheckCache.get(type);
        return marker==annotation;
    }

    public boolean isRelationshipEntity(Class<?> type) {
        if (!annotationCheckCache.containsKey(type)) cacheType(type);
        return checkAnnotationType(type,RelationshipEntity.class);
    }

    public void setPersistentState(Object entity, PropertyContainer pc) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entity.getClass());
        persistentEntity.setPersistentState(entity, pc);
    }

    private EntityAlias entityAlias=new EntityAlias();

    public EntityAlias getEntityAlias() {
        return entityAlias;
    }

    public void setEntityAlias(EntityAlias entityAlias) {
        this.entityAlias = entityAlias;
    }
    
    public boolean isReference(Neo4jPersistentProperty property) {
        for (Annotation annotation : property.getAnnotations()) {
            Boolean isReference = referenceAnnotations.get(annotation);
            if (isReference == null) {
                isReference = Reference.class.isInstance(annotation) || annotation.annotationType().isAnnotationPresent(Reference.class);
                referenceAnnotations.put(annotation, isReference);
            } 
            if (isReference) return true;
        }
        return false;
    }
}
