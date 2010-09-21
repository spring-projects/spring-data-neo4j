package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.api.GraphRelationship;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class DelegatingFieldAccessorFactory<T> implements FieldAccessorFactory<T> {
    private final static Log log = LogFactory.getLog(DelegatingFieldAccessorFactory.class);
    private final GraphDatabaseContext graphDatabaseContext;

    public DelegatingFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.fieldAccessorFactories.addAll(createAccessorFactories());
        this.fieldAccessorListenerFactories.addAll(createListenerFactories());
    }

    protected abstract Collection<FieldAccessorListenerFactory<?>> createListenerFactories();
    protected abstract Collection<? extends FieldAccessorFactory<?>> createAccessorFactories();

    @Override
    public boolean accept(Field f) {
        return true;
    }

    final Collection<FieldAccessorFactory<?>> fieldAccessorFactories=new ArrayList<FieldAccessorFactory<?>>();
	final Collection<FieldAccessorListenerFactory<?>> fieldAccessorListenerFactories = new ArrayList<FieldAccessorListenerFactory<?>>();
    public FieldAccessor forField(Field field) {
        if (isAspectjField(field)) return null;
        for (FieldAccessorFactory<?> fieldAccessorFactory : fieldAccessorFactories) {
            if (fieldAccessorFactory.accept(field)) {
                if (log.isInfoEnabled()) log.info("Factory " + fieldAccessorFactory + " used for field: " + field);
                return fieldAccessorFactory.forField(field);
            }
        }
        //throw new RuntimeException("No FieldAccessor configured for field: " + field);
        log.warn("No FieldAccessor configured for field: " + field);
        return null;
    }

    private boolean isAspectjField(Field field) {
        return field.getName().startsWith("ajc");
    }

    public static String getNeo4jPropertyName(Field field) {
        final Class<?> entityClass = field.getDeclaringClass();
        if (useShortNames(entityClass)) return field.getName();
        return String.format("%s.%s", entityClass.getSimpleName(), field.getName());
    }

    private static boolean useShortNames(Class<?> entityClass) {
        final GraphEntity graphEntity = entityClass.getAnnotation(GraphEntity.class);
        if (graphEntity != null) return graphEntity.useShortNames();
        final GraphRelationship graphRelationship = entityClass.getAnnotation(GraphRelationship.class);
        if (graphRelationship != null) return graphRelationship.useShortNames();
        return false;
    }

    public List<FieldAccessListener<T, ?>> listenersFor(Field field) {
        List<FieldAccessListener<T, ?>> result = new ArrayList<FieldAccessListener<T, ?>>();
        for (FieldAccessorListenerFactory<?> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
            if (fieldAccessorListenerFactory.accept(field)) {
                final FieldAccessListener<T, ?> listener = (FieldAccessListener<T, ?>) fieldAccessorListenerFactory.forField(field);
                result.add(listener);
            }
        }
        return result;
    }
}