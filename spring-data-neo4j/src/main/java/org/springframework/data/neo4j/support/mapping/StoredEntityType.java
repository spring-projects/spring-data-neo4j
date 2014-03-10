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

import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.util.TypeInformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * @author mh
 * @since 14.02.12
 */
public class StoredEntityType {
    /*
    class -> tree of alias (superclass + interfaces), trs writes tree to graph
    write
    class -> fqn -> simple-class-name -> declared alias
    read
    class <- fqn <- simple-class-name <- declared alias

    should the persistent-entity resolve the whole alias information?
    should the persistent-entity contain a list of its super-entities (classes and interfaces?)
    should the TRS/TM not get a class/TI but a PE as parameter?
    and return a PE as well

    should a context be able to resolve not only Class/TI but also className, short-class-name and aliases? -> see below
     */
    private final Object alias;
    private final Neo4jPersistentEntity<?> entity;
    private final Collection<StoredEntityType> superTypes;
    private final EntityAlias entityAlias;
    private final Class<?> type;
    private final boolean isNodeEntity;
    private final boolean isRelationshipEntity;

    StoredEntityType(Neo4jPersistentEntity<?> entity, Collection<Neo4jPersistentEntity<?>> superTypeEntities, final EntityAlias entityAlias) {
        this.entity = entity;
        this.entityAlias = entityAlias;
        this.superTypes = collectSuperTypes(superTypeEntities);
        this.alias = createAlias();
        type = this.entity.getType();
        isNodeEntity = this.entity.isNodeEntity();
        isRelationshipEntity = this.entity.isRelationshipEntity();
    }

    private Collection<StoredEntityType> collectSuperTypes(Collection<Neo4jPersistentEntity<?>> superTypeEntities) {
        if (superTypeEntities==null) return Collections.emptyList();
        Collection<StoredEntityType> result=new ArrayList<>(superTypeEntities.size());
        for (Neo4jPersistentEntity<?> superTypeEntity : superTypeEntities) {
            result.add(superTypeEntity.getEntityType());
        }
        return result;
    }

    protected Object createAlias() {
        return entityAlias.createAlias(entity);
    }

    public Object getAlias() {
        return alias;
    }

    public Neo4jPersistentEntity<?> getEntity() {
        return entity;
    }

    public Collection<StoredEntityType> getSuperTypes() {
        return superTypes;
    }

    boolean matchesAlias(Object alias) {
        if (alias == null) return false;
        final Class<?> type = getType();
        if (alias instanceof String) {
            return getAlias().equals(alias) || type.getName().equals(alias);
        }
        if (alias instanceof Class) {
            return type.equals(alias);
        }
        if (alias instanceof TypeInformation) {
            return entity.getTypeInformation().equals(alias);
        }
        return alias.equals(getAlias());
    }

    public Class<?> getType() {
        return type;
    }

    public boolean isNodeEntity() {
        return isNodeEntity;
    }

    public boolean isRelationshipEntity() {
        return isRelationshipEntity;
    }

    @Override
    public String toString() {
        return String.format("StoredEntityType for %s with alias %s",getType(),getAlias());
    }

    public StoredEntityType findByTypeClass(Class type) {
        if (getType().equals(type)) return this;
        for (StoredEntityType superType : superTypes) {
            StoredEntityType foundType = superType.findByTypeClass(type);
            if (foundType!=null) return foundType;
        }
        return null;
    }
}
