package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Node;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 15.09.2010
 */
public class DetachableEntityStateAccessors<ENTITY extends NodeBacked, STATE> implements EntityStateAccessors<ENTITY> {
    private final Map<Field, Object> dirty = new HashMap<Field, Object>();
    private final EntityStateAccessors<ENTITY> delegate;
    private final static Log log = LogFactory.getLog(DetachableEntityStateAccessors.class);

    public DetachableEntityStateAccessors(final STATE underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        this(new DefaultEntityStateAccessors<ENTITY, STATE>(underlyingState, entity, type, graphDatabaseContext));
    }

    public DetachableEntityStateAccessors(final EntityStateAccessors<ENTITY> delegate) {
        this.delegate = delegate;
    }

    @Override
    public ENTITY getEntity() {
        return delegate.getEntity();
    }

    @Override
    public GraphDatabaseContext getGraphDatabaseContext() {
        return delegate.getGraphDatabaseContext();
    }

    @Override
    public Object getValue(final Field field) {
        if (!transactionIsRunning()) {
            if (!getEntity().hasUnderlyingNode() || isDirty(field)) {
                log.warn("Outside of transaction, GET value from field " + field);
                return null;
            }
        }
        flushDirty();
        return delegate.getValue(field);
    }

    private boolean transactionIsRunning() {
        return delegate.getGraphDatabaseContext().transactionIsRunning();
    }

    @Override
    public Object setValue(final Field field, final Object newVal) {
        if (!transactionIsRunning()) {
            final ENTITY entity = getEntity();
            if (!isDirty(field)) {
                Object existingValue;
                if (entity.hasUnderlyingNode()) existingValue = unwrap(delegate.getValue(field));
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
    public void createAndAssignNode() {
        delegate.createAndAssignNode();
    }

    /**
     * always runs inside of a transaction
     */
    private void flushDirty() {
        final NodeBacked entity = getEntity();
        final boolean newNode = !entity.hasUnderlyingNode();
        if (newNode) {
            createAndAssignNode();
        }
        if (isDirty()) {
            for (final Map.Entry<Field, Object> entry : dirty.entrySet()) {
                final Field field = entry.getKey();
                log.warn("Flushing dirty Entity new node " + newNode + " field " + field);
                if (!newNode) {
                    checkConcurrentModification(entity, entry, field);
                }
                delegate.setValue(field, getValueFromEntity(field));
            }
            clearDirty();
        }
    }

    @Override
    public void setNode(final Node node) {
        delegate.setNode(node);
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

    private void checkConcurrentModification(final NodeBacked entity, final Map.Entry<Field, Object> entry, final Field field) {
        final Object nodeValue = unwrap(delegate.getValue(field));
        final Object previousValue = entry.getValue();
        if (!ObjectUtils.nullSafeEquals(nodeValue, previousValue)) {
            throw new ConcurrentModificationException("Node " + entity.getUnderlyingNode() + " field " + field + " changed in between previous " + previousValue + " current " + nodeValue); // todo or just overwrite
        }
    }

    private Object unwrap(final Object value) {
        return (value instanceof DoReturn) ? ((DoReturn) value).value : value;
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

}



