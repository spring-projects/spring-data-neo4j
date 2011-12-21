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

package org.springframework.data.neo4j.fieldaccess;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class DefaultEntityState<STATE> implements EntityState<STATE> {
    protected final Object entity;
    protected final Class<?> type;
    private final Map<Neo4jPersistentProperty, FieldAccessor> fieldAccessors = new HashMap<Neo4jPersistentProperty, FieldAccessor>();
    private final Map<Neo4jPersistentProperty,List<FieldAccessListener>> fieldAccessorListeners = new HashMap<Neo4jPersistentProperty, List<FieldAccessListener>>();
    private STATE state;
    protected final static Logger log= LoggerFactory.getLogger(DefaultEntityState.class);
    private final FieldAccessorFactoryProviders<Object> fieldAccessorFactoryProviders;
    protected final Neo4jPersistentEntity<?> persistentEntity;

    public DefaultEntityState(final STATE underlyingState, final Object entity, final Class<?> type, final DelegatingFieldAccessorFactory delegatingFieldAccessorFactory, Neo4jPersistentEntity<?> persistentEntity) {
        this.state = underlyingState;
        this.entity = entity;
        this.type = type;
        this.persistentEntity = persistentEntity;
        if (delegatingFieldAccessorFactory!=null) {
            fieldAccessorFactoryProviders = delegatingFieldAccessorFactory.accessorFactoriesFor(persistentEntity);
            this.fieldAccessors.putAll(fieldAccessorFactoryProviders.getFieldAccessors());
            this.fieldAccessorListeners.putAll(fieldAccessorFactoryProviders.getFieldAccessListeners());
        } else {
            fieldAccessorFactoryProviders = null; // todo
        }
    }

    @Override
    public abstract void createAndAssignState();

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public void setPersistentState(final STATE state) {
        this.state = state;
    }

    @Override
    public boolean hasPersistentState() {
        return this.state!=null;
    }

    @Override
    public STATE getPersistentState() {
        return state;
    }

    public Neo4jPersistentEntity<?> getPersistentEntity() {
        return persistentEntity;
    }

    @Override
    public boolean isWritable(Neo4jPersistentProperty property) {
        final FieldAccessor accessor = accessorFor(property);
        if (accessor == null) return true;
        return accessor.isWriteable(entity);
    }

    @Override
    public Object getValue(final Neo4jPersistentProperty property, MappingPolicy mappingPolicy) {
        final FieldAccessor accessor = accessorFor(property);
        if (accessor == null) return null;
        else return accessor.getValue(entity, mappingPolicy);
    }

    @Override
    public Object getValue(final Field field, MappingPolicy mappingPolicy) {
        return getValue(property(field), mappingPolicy);
    }

    @Override
    public Object setValue(final Field field, final Object newVal, MappingPolicy mappingPolicy) {
        return setValue(property(field),newVal, mappingPolicy);
    }

    @Override
    public Object setValue(final Neo4jPersistentProperty property, final Object newVal, MappingPolicy mappingPolicy) {
        final FieldAccessor accessor = accessorFor(property);
        final Object result=accessor!=null ? accessor.setValue(entity, newVal, mappingPolicy) : newVal;
        notifyListeners(property, result);
        return result;
    }


	@Override
	public Object getDefaultValue(Neo4jPersistentProperty property) {
        final FieldAccessor accessor = accessorFor(property);
        if (accessor == null) return null;
        else return accessor.getDefaultValue();
	}

    protected Neo4jPersistentProperty property(Field field) {
        return persistentEntity.getPersistentProperty(field.getName());
    }

    protected FieldAccessor accessorFor(final Neo4jPersistentProperty property) {
        return fieldAccessors.get(property);
    }

    private void notifyListeners(final Neo4jPersistentProperty field, final Object result) {
        if (!fieldAccessorListeners.containsKey(field) || fieldAccessorListeners.get(field) == null) return;
        for (final FieldAccessListener listener : fieldAccessorListeners.get(field)) {
            listener.valueChanged(entity, null, result); // todo oldValue
        }
    }

    protected Object getIdFromEntity() {
        final Neo4jPersistentProperty idProperty = fieldAccessorFactoryProviders.getIdProperty();
        if (idProperty==null) return null;
        return idProperty.getValue(entity, idProperty.getMappingPolicy());
    }
}
