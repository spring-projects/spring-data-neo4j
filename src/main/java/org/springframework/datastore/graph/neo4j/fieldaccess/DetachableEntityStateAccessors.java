package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.datastore.graph.api.GraphBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.unwrap;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DetachableEntityStateAccessors<ENTITY extends GraphBacked<STATE>, STATE> implements EntityStateAccessors<ENTITY,STATE> {
    private final Map<Field, Object> dirty = new HashMap<Field, Object>();
    private final EntityStateAccessors<ENTITY,STATE> delegate;
    private final static Log log = LogFactory.getLog(DetachableEntityStateAccessors.class);
    private GraphDatabaseContext graphDatabaseContext;

    public DetachableEntityStateAccessors(final EntityStateAccessors<ENTITY,STATE> delegate, GraphDatabaseContext graphDatabaseContext) {
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
    public Object getValue(final Field field) {
        if (!transactionIsRunning()) {
            if (getEntity().getUnderlyingState()==null || isDirty(field)) {
                if (log.isWarnEnabled()) log.warn("Outside of transaction, GET value from field " + field);
                return null;
            }
        } else {
            flushDirty();
        }
        return delegate.getValue(field);
    }

    private boolean transactionIsRunning() {
        return getGraphDatabaseContext().transactionIsRunning();
    }

    @Override
    public Object setValue(final Field field, final Object newVal) {
        if (!transactionIsRunning()) {
            final ENTITY entity = getEntity();
            if (!isDirty(field) && isWritable(field)) {
                Object existingValue;
                if (entity.getUnderlyingState()!=null) existingValue = unwrap(delegate.getValue(field));
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
        delegate.createAndAssignState();
    }

    /**
     * always runs inside of a transaction
     */
    private void flushDirty() {
        final ENTITY entity = getEntity();
        final boolean newNode = entity.getUnderlyingState()==null;
        if (newNode) {
            createAndAssignState();
        }
        if (isDirty()) {
            for (final Map.Entry<Field, Object> entry : dirty.entrySet()) {
                final Field field = entry.getKey();
                if (log.isWarnEnabled()) log.warn("Flushing dirty Entity new node " + newNode + " field " + field);
                if (!newNode) {
                    checkConcurrentModification(entity, entry, field);
                }
                delegate.setValue(field, getValueFromEntity(field));
            }
            clearDirty();
        }
    }

    @Override
    public void setUnderlyingState(final STATE state) {
        delegate.setUnderlyingState(state);
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
            throw new ConcurrentModificationException("Node " + entity.getUnderlyingState() + " field " + field + " changed in between previous " + previousValue + " current " + nodeValue); // todo or just overwrite
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
}



