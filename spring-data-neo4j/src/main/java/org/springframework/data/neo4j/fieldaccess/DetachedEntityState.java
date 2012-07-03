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

import org.neo4j.graphdb.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.neo4j.support.DoReturn.unwrap;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DetachedEntityState<STATE> implements EntityState<STATE> {
    private final static Logger log = LoggerFactory.getLogger(DetachedEntityState.class);

    private final Map<Neo4jPersistentProperty, ExistingValue> dirty = new HashMap<Neo4jPersistentProperty, ExistingValue>();
    protected final EntityState<STATE> delegate;
    private Neo4jTemplate template;
    private Neo4jPersistentEntity<?> persistentEntity;

    public DetachedEntityState(final EntityState<STATE> delegate, Neo4jTemplate template) {
        this.delegate = delegate;
        this.persistentEntity = delegate.getPersistentEntity();
        this.template = template;
    }

    @Override
    public boolean isWritable(Neo4jPersistentProperty property) {
        return delegate.isWritable(property);
    }

    @Override
    public Object getEntity() {
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
    public Neo4jPersistentEntity<?> getPersistentEntity() {
        return persistentEntity;
    }

    @Override
    public Object getValue(Neo4jPersistentProperty property, MappingPolicy mappingPolicy) {
        mappingPolicy = mappingPolicy == null ? property.getMappingPolicy() : mappingPolicy;
        if (isDetached()) {
            if (template.getPersistentState(getEntity())==null || isDirty(property)) {
                if (log.isDebugEnabled()) log.debug("Outside of transaction, GET value from field " + property);
                Object entityValue = getValueFromEntity(property, MappingPolicy.MAP_FIELD_DIRECT_POLICY);
                if (entityValue != null) {
                	return entityValue;
                }

                Object defaultValue = getDefaultValue(property);
                if (defaultValue != null) {
                    final Object entity = getEntity();
                    property.setValue(entity, defaultValue);
                    addDirty(property, defaultValue, false);
                }
                return defaultValue;
            }
        } else {
//            flushDirty();
        }
        return delegate.getValue(property, mappingPolicy);
    }

    @Override
    public Object getValue(final Field field, MappingPolicy mappingPolicy) {
        return getValue(property(field), mappingPolicy);
    }

    protected boolean isDetached() {
        return !transactionIsRunning() || !hasPersistentState() || isDirty();
    }

    protected boolean transactionIsRunning() {
        return getTemplate().transactionIsRunning();
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
    public Object setValue(final Field field, final Object newVal, MappingPolicy mappingPolicy) {
        return setValue(property(field),newVal, mappingPolicy);
    }

    private Neo4jPersistentProperty property(Field field) {
        return persistentEntity.getPersistentProperty(field.getName());
    }

    @Override
    public Object setValue(final Neo4jPersistentProperty property, final Object newVal, MappingPolicy mappingPolicy) {
        if (isDetached()) {
            if (!isDirty(property) && isWritable(property)) {
                if (hasPersistentState()) {
                    addDirty(property, unwrap(delegate.getValue(property, MappingPolicy.MAP_FIELD_DIRECT_POLICY)), true);
                }
                else {
                    addDirty(property, newVal, false);
                }
            }
            return newVal;
        }
        // flushDirty();
        return delegate.setValue(property, newVal, mappingPolicy);
    }
	@Override
	public Object getDefaultValue(Neo4jPersistentProperty property) {
        return delegate.getDefaultValue(property);
	}
    private Object getDefaultValue(final Class<?> type) {
        if (type.isPrimitive()) {
            if (type.equals(boolean.class)) return false;
            return 0;
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void createAndAssignState() {
        if (template.transactionIsRunning()) {
            delegate.createAndAssignState();
        } else {
            log.warn("New Nodebacked created outside of transaction " + delegate.getEntity().getClass());
        }
    }

    /**
     * always runs inside of a transaction
     */
    private void flushDirty() {
        final Object entity = getEntity();
        if (!hasPersistentState()) {
            // createAndAssignState();
            throw new IllegalStateException("Flushing detached entity without a persistent state, this had to be created first.");
        }

        if (isDirty()) {
            final Map<Neo4jPersistentProperty, ExistingValue> dirtyCopy = new HashMap<Neo4jPersistentProperty, ExistingValue>(dirty);
            try {
                for (final Map.Entry<Neo4jPersistentProperty, ExistingValue> entry : dirtyCopy.entrySet()) {
                    final Neo4jPersistentProperty property = entry.getKey();
                    Object valueFromEntity = getValueFromEntity(property, MappingPolicy.MAP_FIELD_DIRECT_POLICY);
                    cascadePersist(valueFromEntity);
                    if (log.isDebugEnabled()) log.debug("Flushing dirty Entity new node " + entity + " field " + property+ " with value "+ valueFromEntity);
                    final MappingPolicy mappingPolicy = property.getMappingPolicy();
                    checkConcurrentModification(entity, entry, property, mappingPolicy);
                    delegate.setValue(property, valueFromEntity, mappingPolicy);
                    dirty.remove(property);
                }
            } finally {
                if (!dirty.isEmpty()) { // restore all dirty data
                    dirty.putAll(dirtyCopy);
                }
            }
        }
    }


    private void cascadePersist(Object valueFromEntity) {
    /* TODO   if (valueFromEntity instanceof NodeBacked) {
            ((NodeBacked) valueFromEntity).persist();
        }
        if (valueFromEntity instanceof Collection) {
            for (Object o : (Collection<Object>)valueFromEntity) {
                if (o instanceof NodeBacked) {
                    ((NodeBacked) o).persist();
                }
            }
        }
     */
    }

    @Override
    public void setPersistentState(final STATE state) {
        delegate.setPersistentState(state);
    }


    private Object getValueFromEntity(final Neo4jPersistentProperty property, MappingPolicy mappingPolicy) {
        final Object entity = getEntity();
        return property.getValue(entity, mappingPolicy);
    }

    private void checkConcurrentModification(final Object entity, final Map.Entry<Neo4jPersistentProperty, ExistingValue> entry, final Neo4jPersistentProperty property, final MappingPolicy mappingPolicy) {
        final ExistingValue previousValue = entry.getValue();
        if (previousValue.mustCheckConcurrentModification()) {
            final Object nodeValue = unwrap(delegate.getValue(property, mappingPolicy));
            if (!ObjectUtils.nullSafeEquals(nodeValue, previousValue.value)) {
                throw new ConcurrentModificationException("Node " + entity + " field " + property + " changed in between previous " + previousValue + " current " + nodeValue); // todo or just overwrite
            }
        }
    }

    private boolean isDirty() {
        return !this.dirty.isEmpty();
    }

    private boolean isDirty(final Neo4jPersistentProperty property) {
        return this.dirty.containsKey(property);
    }

    private void clearDirty() {
        this.dirty.clear();
    }
    
    private void clearDirty(final Field f) {
        this.dirty.remove(f);
    }

    private void addDirty(final Neo4jPersistentProperty property, final Object previousValue, boolean fromGraph) {
        this.dirty.put(property, new ExistingValue(previousValue,fromGraph));
    }


    public Neo4jTemplate getTemplate() {
        return template;
    }

    // todo always create an transaction for persist, atomic operation when no outside tx exists
    @Override
    public Object persist() {
        if (!isDetached()) return getEntity();
        Transaction tx = template.beginTx();
        try {
            Object result = delegate.persist();

            flushDirty();
            tx.success();
            return result;
        } catch(Throwable t) {
			tx.failure();
			if (t instanceof Error) throw (Error)t;
			if (t instanceof RuntimeException) throw (RuntimeException)t;
			throw new org.springframework.data.neo4j.core.UncategorizedGraphStoreException("Error persisting entity "+getEntity(),t);
        } finally {
            tx.finish();
        }
    }
}
