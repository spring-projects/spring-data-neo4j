package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotInTransactionException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.datastore.graph.api.GraphBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public abstract class DefaultEntityStateAccessors<ENTITY extends GraphBacked<STATE>, STATE> implements EntityStateAccessors<ENTITY,STATE> {
    private final STATE underlyingState;
    protected final ENTITY entity;
    private final Class<? extends ENTITY> type;
    protected final GraphDatabaseContext graphDatabaseContext;
    private final Map<Field,FieldAccessor<ENTITY,?>> fieldAccessors=new HashMap<Field, FieldAccessor<ENTITY,?>>();
    private final Map<Field,List<FieldAccessListener<ENTITY,?>>> fieldAccessorListeners=new HashMap<Field, List<FieldAccessListener<ENTITY,?>>>();
    private STATE state;
    protected final static Log log= LogFactory.getLog(DefaultEntityStateAccessors.class);


    public DefaultEntityStateAccessors(final STATE underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        this.underlyingState = underlyingState;
        this.entity = entity;
        this.type = type;
        this.graphDatabaseContext = graphDatabaseContext;
        createAccessorsAndListeners(type, graphDatabaseContext);
    }

    @Override
    public abstract void createAndAssignState();

    @Override
    public ENTITY getEntity() {
        return entity;
    }

    @Override
    public GraphDatabaseContext getGraphDatabaseContext() {
        return graphDatabaseContext;
    }

    @Override
    public void setUnderlyingState(final STATE state) {
        this.state = state;
    }

    private void createAccessorsAndListeners(final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext) {
        final DelegatingFieldAccessorFactory fieldAccessorFactory = new DelegatingFieldAccessorFactory(graphDatabaseContext);
        ReflectionUtils.doWithFields(type, new ReflectionUtils.FieldCallback() {
            public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                fieldAccessors.put(field, fieldAccessorFactory.forField(field));
                fieldAccessorListeners.put(field, fieldAccessorFactory.listenersFor(field)); // TODO Bad code
            }
        });
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

}
