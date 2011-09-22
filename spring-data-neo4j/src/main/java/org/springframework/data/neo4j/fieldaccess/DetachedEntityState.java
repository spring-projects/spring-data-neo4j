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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.unwrap;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DetachedEntityState<ENTITY extends GraphBacked<STATE>, STATE> implements EntityState<ENTITY,STATE> {
    private final Map<Field, ExistingValue> dirty = new HashMap<Field, ExistingValue>();
    protected final EntityState<ENTITY,STATE> delegate;
    private final static Log log = LogFactory.getLog(DetachedEntityState.class);
    private GraphDatabaseContext graphDatabaseContext;
    public DetachedEntityState(final EntityState<ENTITY, STATE> delegate, GraphDatabaseContext graphDatabaseContext) {
        this.delegate = delegate;
        this.graphDatabaseContext = graphDatabaseContext;
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
        if (isDetached()) {
            if (getEntity().getPersistentState()==null || isDirty(field)) {
                if (log.isDebugEnabled()) log.debug("Outside of transaction, GET value from field " + field);
                Object entityValue = getValueFromEntity(field);
                if (entityValue != null) {
                	return entityValue;
                }
                
                Object defaultValue = getDefaultImplementation(field);
                if (defaultValue != null) {
                    final ENTITY entity = getEntity();
                    try {
                        field.setAccessible(true);
                        field.set(entity, defaultValue);
                        addDirty(field, defaultValue, false);
                    } catch(IllegalAccessException e) {
                    	throw new RuntimeException("Error setting default value for field " + field + " in " + entity.getClass(), e);
                    }
                }
                return defaultValue;
            }
        } else {
//            flushDirty();
        }
        return delegate.getValue(field);
    }

    protected boolean isDetached() {
        return !transactionIsRunning() || !hasPersistentState() || isDirty();
    }

    protected boolean transactionIsRunning() {
        return getGraphDatabaseContext().transactionIsRunning();
    }

    static class ExistingValue {
        public final Object value;
        private final boolean fromGraph;

        ExistingValue(Object value, boolean fromGraph) {
            this.value = value;
            this.fromGraph = fromGraph;
        }

        @Override
        public String toString() {
            return String.format("ExistingValue{value=%s, fromGraph=%s}", value, fromGraph);
        }

        private boolean mustCheckConcurrentModification() {
            return fromGraph;
        }
    }
    @Override
    public Object setValue(final Field field, final Object newVal) {
        if (isDetached()) {
            if (!isDirty(field) && isWritable(field)) {
                Object existingValue;
                if (hasPersistentState()) {
                    addDirty(field, unwrap(delegate.getValue(field)), true);
                }
                else {
                    // existingValue = getValueFromEntity(field);
                    // if (existingValue == null) existingValue = getDefaultValue(field.getType());
                    addDirty(field, newVal, false);
                }
            }
            return newVal;
        }
        // flushDirty();
        return delegate.setValue(field, newVal);
    }
	@Override
	public Object getDefaultImplementation(Field field) {
        return delegate.getDefaultImplementation(field);
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
        if (!hasPersistentState()) {
            // createAndAssignState();
            throw new IllegalStateException("Flushing detached entity without a persistent state, this had to be created first.");
        }

        if (isDirty()) {
            final Map<Field, ExistingValue> dirtyCopy = new HashMap<Field, ExistingValue>(dirty);
            clearDirty();
            for (final Map.Entry<Field, ExistingValue> entry : dirtyCopy.entrySet()) {
                final Field field = entry.getKey();
                Object valueFromEntity = getValueFromEntity(field);
                cascadePersist(valueFromEntity);
                if (log.isDebugEnabled()) log.debug("Flushing dirty Entity new node " + entity.getPersistentState() + " field " + field+ " with value "+ valueFromEntity);
                checkConcurrentModification(entity, entry, field);
                delegate.setValue(field, valueFromEntity);
            }
        }
    }

    private void cascadePersist(Object valueFromEntity) {
        if (valueFromEntity instanceof NodeBacked) {
            ((NodeBacked) valueFromEntity).persist();
        }
        if (valueFromEntity instanceof Collection) {
            for (Object o : (Collection<Object>)valueFromEntity) {
                if (o instanceof NodeBacked) {
                    ((NodeBacked) o).persist();
                }
            }
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

    private void checkConcurrentModification(final ENTITY entity, final Map.Entry<Field, ExistingValue> entry, final Field field) {
        final ExistingValue previousValue = entry.getValue();
        if (previousValue.mustCheckConcurrentModification()) {
            final Object nodeValue = unwrap(delegate.getValue(field));
            if (!ObjectUtils.nullSafeEquals(nodeValue, previousValue.value)) {
                throw new ConcurrentModificationException("Node " + entity.getPersistentState() + " field " + field + " changed in between previous " + previousValue + " current " + nodeValue); // todo or just overwrite
            }
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
    
    private void clearDirty(final Field f) {
        this.dirty.remove(f);
    }

    private void addDirty(final Field f, final Object previousValue, boolean fromGraph) {
        this.dirty.put(f, new ExistingValue(previousValue,fromGraph));
    }


    public GraphDatabaseContext getGraphDatabaseContext() {
        return graphDatabaseContext;
    }

    // todo always create an transaction for persist, atomic operation when no outside tx exists
    @Override
    public ENTITY persist() {
        if (!isDetached()) return getEntity();
        Transaction tx = graphDatabaseContext.beginTx();
        try {
            ENTITY result = delegate.persist();

            flushDirty();
            tx.success();
            return result;
        } finally {
            tx.finish();
        }
    }
}
