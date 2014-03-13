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

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.index.lucene.ValueContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.Reference;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Neo4J specific {@link MappingContext} implementation. Simply creates {@link Neo4jPersistentEntityImpl} and
 * {@link org.springframework.data.neo4j.mapping.Neo4jPersistentProperty} instances.
 *
 * @author Oliver Gierke
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntityImpl<?>, Neo4jPersistentProperty> {

    private final static Logger log = LoggerFactory.getLogger(Neo4jMappingContext.class);

    private final Map<Annotation, Boolean> referenceAnnotations = new IdentityHashMap<>();
    private EntityIndexCreator entityIndexCreator;

    protected <T> Neo4jPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        final Class<T> type = typeInformation.getType();
        if (type.isAnnotationPresent(NodeEntity.class)) {
            return new Neo4jPersistentEntityImpl<T>(typeInformation,entityAlias);
        }
        if (type.isAnnotationPresent(RelationshipEntity.class)) {
            return new Neo4jPersistentEntityImpl<T>(typeInformation,entityAlias);
        }
        throw new InvalidEntityTypeException("Type " + type + " is neither a @NodeEntity nor a @RelationshipEntity");
    }

    @Override
    protected Neo4jPersistentEntityImpl<?> addPersistentEntity(TypeInformation<?> typeInformation) {
        final Neo4jPersistentEntityImpl<?> entity = super.addPersistentEntity(typeInformation);
        Collection<Neo4jPersistentEntity<?>> superTypeEntities = addSuperTypes(entity);
        updateStoredEntityType(entity, superTypeEntities);
        return entity;
    }

    private void updateStoredEntityType(Neo4jPersistentEntityImpl<?> entity, Collection<Neo4jPersistentEntity<?>> superTypeEntities) {
        entity.updateStoredType(superTypeEntities);
        if (entityIndexCreator!=null) {
            entityIndexCreator.ensureEntityIndexes(entity);
//            if (superTypeEntities!=null) {
//                for (Neo4jPersistentEntity<?> superTypeEntity : superTypeEntities) {
//                    entityIndexCreator.ensureEntityIndexes(superTypeEntity);
//                }
//            }
        }
    }

    private List<Neo4jPersistentEntity<?>> addSuperTypes(Neo4jPersistentEntity<?> entity) {
        List<Neo4jPersistentEntity<?>> entities=new ArrayList<>();
        Class<?> type = entity.getType();
        Collection<Class> typesToAdd = new LinkedHashSet<>();
        while (type != null) {
            typesToAdd.add(type.getSuperclass());
            typesToAdd.addAll(Arrays.asList(type.getInterfaces()));
            type = type.getSuperclass();
        }
        for (Class<?> superType : typesToAdd) {
            entities.addAll(addPersistentEntityWithCheck(superType));
        }
        return entities;
    }

    private Collection<Neo4jPersistentEntity<?>> addPersistentEntityWithCheck(Class<?> type) {
        if (type != null && (isNodeEntityType(type) || isRelationshipEntityType(type))) {
            return Collections.<Neo4jPersistentEntity<?>>singletonList(addPersistentEntity(type));
        }
        return Collections.emptyList();
    }

    @Override
    public void initialize() {
        super.initialize();
        setStrict(true);
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
        if (type==null) return null;
        Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(type);
        return persistentEntity==null ? null : persistentEntity.getEntityType();
    }

    @Override
    protected Neo4jPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
                                                               Neo4jPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new Neo4jPersistentPropertyImpl(field, descriptor, owner, simpleTypeHolder,this);
    }

    private final Map<Class<?>,Class<?>> annotationCheckCache = new IdentityHashMap<Class<?>, Class<?>>();

    public boolean isNodeEntity(Class<?> type) {
        if (Node.class.isAssignableFrom(type)) return true;
        if (!annotationCheckCache.containsKey(type)) cacheType(type);
        return checkAnnotationType(type,NodeEntity.class);
    }

    private void cacheType(Class<?> type) {
        try {
            final Neo4jPersistentEntityImpl<?> entity = getPersistentEntity(type);
            if (entity == null) annotationCheckCache.put(type, type);
            else if (!shouldCreatePersistentEntityFor(ClassTypeInformation.from(type))) annotationCheckCache.put(type, type);
            else if (entity.isNodeEntity()) annotationCheckCache.put(type, NodeEntity.class);
            else if (entity.isRelationshipEntity()) annotationCheckCache.put(type, RelationshipEntity.class);
            else annotationCheckCache.put(type, type);
        } catch (InvalidEntityTypeException me) {
            annotationCheckCache.put(type, type);
        }
    }

    @Override
    protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> typeInformation) {
        boolean result = super.shouldCreatePersistentEntityFor(typeInformation);
        if (!result) return result;
        if (typeInformation.isCollectionLike()) return false;
        Class<?> type = typeInformation.getType();
        if (type.isEnum() || type.isArray() || type.equals(Node.class) || type.equals(Relationship.class) || type.equals(ValueContext.class)) {
            return false;
        }
        String packageName = type.getPackage().getName();
        if (packageName.startsWith("java")) {
            return false;
        }
        return true;
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

    public void setEntityIndexCreator(EntityIndexCreator entityIndexCreator) {
        this.entityIndexCreator = entityIndexCreator;
    }
}
