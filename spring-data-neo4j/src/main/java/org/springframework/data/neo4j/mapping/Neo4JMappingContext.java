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

package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

/**
 * Neo4J specific {@link MappingContext} implementation. Simply creates {@link Neo4jPersistentEntityImpl} and
 * {@link Neo4jPersistentProperty} instances.
 * 
 * @author Oliver Gierke
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntityImpl<?>, Neo4jPersistentProperty> {

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
     */
    @Override
    protected <T> Neo4jPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
        return new Neo4jPersistentEntityImpl<T>(typeInformation);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
     */
    @Override
    protected Neo4jPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
            Neo4jPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
        return new Neo4jPersistentPropertyImpl(field, descriptor, owner, simpleTypeHolder);
    }

    public boolean isNodeEntity(Class<?> type) {
        return getPersistentEntity(type).isNodeEntity();
    }

    public boolean isRelationshipEntity(Class<?> type) {
        return getPersistentEntity(type).isRelationshipEntity();
    }

    public void setPersistentState(Object entity, PropertyContainer pc) {
        final Neo4jPersistentEntityImpl<?> persistentEntity = getPersistentEntity(entity.getClass());
        persistentEntity.setPersistentState(entity, pc);
    }

}
