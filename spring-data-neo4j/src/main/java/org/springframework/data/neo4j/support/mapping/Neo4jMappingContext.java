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
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.mapping.InvalidEntityTypeException;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Neo4J specific {@link MappingContext} implementation. Simply creates {@link Neo4jPersistentEntityImpl} and
 * {@link org.springframework.data.neo4j.mapping.Neo4jPersistentProperty} instances.
 *
 * @author Oliver Gierke
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntityImpl<?>, Neo4jPersistentProperty> {

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
        typeAliases.update(this);
        return entity;
    }

    @Override
    protected Neo4jPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
                                                               Neo4jPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new Neo4jPersistentPropertyImpl(field, descriptor, owner, simpleTypeHolder,this);
    }

    public boolean isNodeEntity(Class<?> type) {
        try {
            return getPersistentEntity(type).isNodeEntity();
        } catch (InvalidEntityTypeException me) {
            return false;
        }
    }

    public boolean isRelationshipEntity(Class<?> type) {
        try {
            return getPersistentEntity(type).isRelationshipEntity();
        } catch (InvalidEntityTypeException me) {
            return false;
        }
    }

    public void setPersistentState(Object entity, PropertyContainer pc) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entity.getClass());
        persistentEntity.setPersistentState(entity, pc);
    }

    // todo move to superclass
    public boolean isPersistentEntity(Class type) {
        try {
            return getPersistentEntity(type)!=null;
        } catch(MappingException me) {
            return false;
        }
    }


    static class TypeAliases {
        private final Map<Object,Class> aliasToType = new HashMap<Object, Class>();
        private final Map<Class,Object> typeToAlias = new HashMap<Class,Object>();
        private final Map<Class,List<Object>> typeToAliasHierarchy = new HashMap<Class,List<Object>>();

        public void update(Neo4jMappingContext context) {
            for (Neo4jPersistentEntityImpl<?> entity : new ArrayList<Neo4jPersistentEntityImpl<?>>(context.getPersistentEntities())) {
                addType(entity,context);
            }
        }

        public List<Object> addType(Neo4jPersistentEntityImpl<?> entity, Neo4jMappingContext context) {
            if (entity==null) return Collections.emptyList();
            List<Object> hierarchy=new ArrayList<Object>();
            final Class type = entity.getType();
            store(type, entity.getTypeAlias());
            hierarchy.add(resolveType(type));
            hierarchy.addAll(addType(getPersistentEntity(context, type.getSuperclass()), context));
            typeToAliasHierarchy.put(type, hierarchy);
            return hierarchy;
        }

        private Neo4jPersistentEntityImpl<?> getPersistentEntity(Neo4jMappingContext context, Class<?> type) {
            try {
                return context.getPersistentEntity(type);
            } catch(MappingException me) {
                return null;
            }
        }

        private void store(Class type, Object typeAlias) {
            storeAlias(type, type);
            storeAlias(type.getName(), type);
            storeAlias(type.getSimpleName(), type);
            if (typeAlias!=null) {
                storeAlias(typeAlias, type);
                storeType(type, typeAlias);
            } else {
                storeType(type, type.getSimpleName());
            }
        }

        private Object storeType(Class type, Object alias) {
            return typeToAlias.put(type,alias);
        }

        private void storeAlias(Object alias, Class type) {
            if (aliasToType.containsKey(alias)) {
                final Class existingType = aliasToType.get(alias);
                if (!existingType.equals(type)) throw new MappingException("Can not map the same alias "+alias+" to two types: new: "+type+" existing: "+ existingType);
            } else {
                aliasToType.put(alias,type);
            }
        }
        public Class resolveAlias(Object alias) {
            return aliasToType.get(alias);
        }
        public Object resolveType(Class type) {
            return typeToAlias.get(type);
        }
        public List<Object> resolveTypeHierarchy(Class type) {
            return typeToAliasHierarchy.get(type);
        }
    }

    private final TypeAliases typeAliases=new TypeAliases();

    public List<Object> getTypeHierarchy(Class type) {
        return typeAliases.resolveTypeHierarchy(type);
    }

    public Class getType(Object alias) {
        return typeAliases.resolveAlias(alias);
    }
}
