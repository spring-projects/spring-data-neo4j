/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.graph.core.GraphBacked;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class DefaultEntityStateAccessors<ENTITY extends GraphBacked<STATE>, STATE> implements EntityStateAccessors<ENTITY,STATE> {
    private final STATE underlyingState;
    protected final ENTITY entity;
    protected final Class<? extends ENTITY> type;
    private final Map<Field, FieldAccessor<ENTITY, ?>> fieldAccessors;
    private final Map<Field,List<FieldAccessListener<ENTITY,?>>> fieldAccessorListeners;
    private STATE state;
    protected final static Log log= LogFactory.getLog(DefaultEntityStateAccessors.class);
    private final FieldAccessorFactoryProviders<ENTITY> fieldAccessorFactoryProviders;


    public DefaultEntityStateAccessors(final STATE underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final DelegatingFieldAccessorFactory delegatingFieldAccessorFactory) {
        this.underlyingState = underlyingState;
        this.entity = entity;
        this.type = type;
        fieldAccessorFactoryProviders = delegatingFieldAccessorFactory.accessorFactoriesFor(type);
        this.fieldAccessors= fieldAccessorFactoryProviders.getFieldAccessors();
        this.fieldAccessorListeners= fieldAccessorFactoryProviders.getFieldAccessListeners();
    }

    @Override
    public abstract void createAndAssignState();

    @Override
    public ENTITY getEntity() {
        return entity;
    }

    @Override
    public void setUnderlyingState(final STATE state) {
        this.state = state;
    }

    @Override
    public boolean isWritable(Field field) {
        final FieldAccessor<ENTITY, ?> accessor = accessorFor(field);
        if (accessor == null) return true;
        return accessor.isWriteable(entity);
    }

    @Override
    public Object getValue(final Field field) {
        final FieldAccessor<ENTITY, ?> accessor = accessorFor(field);
        if (accessor == null) return null;
        else return accessor.getValue(entity);
    }
    @Override
    public Object setValue(final Field field, final Object newVal) {
        final FieldAccessor<ENTITY, ?> accessor = accessorFor(field);
        final Object result=accessor!=null ? accessor.setValue(entity, newVal) : newVal;
        notifyListeners(field, result);
        return result;
    }

    private FieldAccessor<ENTITY, ?> accessorFor(final Field field) {
        return fieldAccessors.get(field);
    }

    private void notifyListeners(final Field field, final Object result) {
        if (!fieldAccessorListeners.containsKey(field) || fieldAccessorListeners.get(field) == null) return;

        for (final FieldAccessListener<ENTITY, ?> listener : fieldAccessorListeners.get(field)) {
            listener.valueChanged(entity, null, result); // todo oldValue
        }
    }

    protected Object getIdFromEntity() {
        final Field idField = fieldAccessorFactoryProviders.getIdField();
        if (idField==null) return null;
        try {
            idField.setAccessible(true);
            return idField.get(entity);
        } catch (IllegalAccessException e) {
            log.warn("Error accessing id field "+idField);
            return null;
        }
    }

}
