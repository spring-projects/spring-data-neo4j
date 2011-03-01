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
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.unwrap;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DetachableEntityState<ENTITY extends GraphBacked<STATE>, STATE> implements EntityState<ENTITY,STATE> {
    private final Map<Field, Object> dirty = new HashMap<Field, Object>();
    protected final EntityState<ENTITY,STATE> delegate;
    private final static Log log = LogFactory.getLog(DetachableEntityState.class);
    private GraphDatabaseContext graphDatabaseContext;
    private final boolean autoAttach;

    public DetachableEntityState(final EntityState<ENTITY, STATE> delegate, GraphDatabaseContext graphDatabaseContext, boolean autoAttach) {
        this.delegate = delegate;
        this.graphDatabaseContext = graphDatabaseContext;
        this.autoAttach = autoAttach;
    }

    @Override
    public boolean isWritable(final Field field) {
        return delegate.isWritable(field);
    }

    @Override
    public ENTITY getEntity() {
        return delegate.getEntity();
    }

    @Override
    public boolean hasPersistentState() {
        return delegate.hasPersistentState();
    }

    @Override
    public STATE getPersistentState() {
        return delegate.getPersistentState();
    }

    @Override
    public Object getValue(final Field field) {
        if (!transactionIsRunning() || !hasPersistentState()) {
            if (getEntity().getPersistentState()==null || isDirty(field)) {
                if (log.isDebugEnabled()) log.debug("Outside of transaction, GET value from field " + field);
                return null;
            }
        } else {
            flushDirty();
        }
        return delegate.getValue(field);
    }

    protected boolean transactionIsRunning() {
        return getGraphDatabaseContext().transactionIsRunning();
    }

    @Override
    public Object setValue(final Field field, final Object newVal) {
        if (!transactionIsRunning() || !hasPersistentState()) {
            final ENTITY entity = getEntity();
            if (!isDirty(field) && isWritable(field)) {
                Object existingValue;
                if (entity.getPersistentState()!=null) existingValue = unwrap(delegate.getValue(field));
                else {
                    existingValue = getValueFromEntity(field);
                    if (existingValue == null) existingValue = getDefaultValue(field.getType());
                }
                addDirty(field, existingValue);
            }
            return newVal;
        }
        flushDirty();
        return delegate.setValue(field, newVal);
    }

    private Object getDefaultValue(final Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) return false;
            return 0;
        }
        return null;
    }

    @Override
    public void createAndAssignState() {
        if (graphDatabaseContext.transactionIsRunning()) {
            delegate.createAndAssignState();
        } else {
            log.warn("New Nodebacked created outside of transaction " + delegate.getEntity().getClass());
        }
    }

    /**
     * always runs inside of a transaction
     */
    private void flushDirty() {
        final ENTITY entity = getEntity();
        final boolean newState = entity.getPersistentState()==null;
        if (newState) {
            return;
            // createAndAssignState();
        }
        if (isDirty()) {
            for (final Map.Entry<Field, Object> entry : dirty.entrySet()) {
                final Field field = entry.getKey();
                if (log.isDebugEnabled()) log.debug("Flushing dirty Entity new node " + newState + " field " + field);
                if (!newState) {
                    checkConcurrentModification(entity, entry, field);
                }
                delegate.setValue(field, getValueFromEntity(field));
            }
            clearDirty();
        }
    }

    @Override
    public void setPersistentState(final STATE state) {
        delegate.setPersistentState(state);
    }


    private Object getValueFromEntity(final Field field) {
        final ENTITY entity = getEntity();
        try {
            field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error accessing field " + field + " in " + entity.getClass(), e);
        }
    }

    private void checkConcurrentModification(final ENTITY entity, final Map.Entry<Field, Object> entry, final Field field) {
        final Object nodeValue = unwrap(delegate.getValue(field));
        final Object previousValue = entry.getValue();
        if (!ObjectUtils.nullSafeEquals(nodeValue, previousValue)) {
            throw new ConcurrentModificationException("Node " + entity.getPersistentState() + " field " + field + " changed in between previous " + previousValue + " current " + nodeValue); // todo or just overwrite
        }
    }

    private boolean isDirty() {
        return !this.dirty.isEmpty();
    }

    private boolean isDirty(final Field f) {
        return this.dirty.containsKey(f);
    }

    private void clearDirty() {
        this.dirty.clear();
    }

    private void addDirty(final Field f, final Object previousValue) {
        this.dirty.put(f, previousValue);
    }

    public GraphDatabaseContext getGraphDatabaseContext() {
        return graphDatabaseContext;
    }

    @Override
    public ENTITY persist(boolean isOnCreate) {
        if (!autoAttach && isOnCreate) {
            log.warn("Not automatically attaching entity " + getEntity().getClass());
            return getEntity();
        }
        if (graphDatabaseContext.transactionIsRunning()) {
            ENTITY result = delegate.persist(isOnCreate);
            flushDirty();
            return result;
        }
        throw new IllegalStateException("Tried to attach entity outside of transaction "+getEntity().getClass());
    }

}



