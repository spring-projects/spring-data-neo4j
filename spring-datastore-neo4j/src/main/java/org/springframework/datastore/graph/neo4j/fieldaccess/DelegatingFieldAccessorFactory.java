package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.datastore.graph.api.GraphEntity;
import org.springframework.datastore.graph.api.GraphRelationship;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;


public abstract class DelegatingFieldAccessorFactory<T> implements FieldAccessorFactory<T> {
    private final static Log log = LogFactory.getLog(DelegatingFieldAccessorFactory.class);
    private final GraphDatabaseContext graphDatabaseContext;

    public DelegatingFieldAccessorFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.fieldAccessorFactories.addAll(createAccessorFactories());
        this.fieldAccessorListenerFactories.addAll(createListenerFactories());
    }

    protected abstract Collection<FieldAccessorListenerFactory<?>> createListenerFactories();

    protected abstract Collection<? extends FieldAccessorFactory<?>> createAccessorFactories();

    @Override
    public boolean accept(final Field f) {
        return true;
    }

    final Collection<FieldAccessorFactory<?>> fieldAccessorFactories = new ArrayList<FieldAccessorFactory<?>>();
    final Collection<FieldAccessorListenerFactory<?>> fieldAccessorListenerFactories = new ArrayList<FieldAccessorListenerFactory<?>>();

    public FieldAccessor forField(final Field field) {
        final FieldAccessorFactory<?> factory = factoryForField(field);
        return factory != null ? factory.forField(field) : null;
    }

    private <E> FieldAccessorFactory<E> factoryForField(final Field field) {
        if (isAspectjField(field)) return null;
        for (final FieldAccessorFactory<?> fieldAccessorFactory : fieldAccessorFactories) {
            if (fieldAccessorFactory.accept(field)) {
                if (log.isInfoEnabled()) log.info("Factory " + fieldAccessorFactory + " used for field: " + field);
                return (FieldAccessorFactory<E>) fieldAccessorFactory;
            }
        }
        //throw new RuntimeException("No FieldAccessor configured for field: " + field);
        if (log.isInfoEnabled()) log.info("No FieldAccessor configured for field: " + field);
        return null;
    }

    private boolean isAspectjField(final Field field) {
        return field.getName().startsWith("ajc");
    }

    public static String getNeo4jPropertyName(final Field field) {
        final Class<?> entityClass = field.getDeclaringClass();
        if (useShortNames(entityClass)) return field.getName();
        return String.format("%s.%s", entityClass.getSimpleName(), field.getName());
    }

    private static boolean useShortNames(final Class<?> entityClass) {
        final GraphEntity graphEntity = entityClass.getAnnotation(GraphEntity.class);
        if (graphEntity != null) return graphEntity.useShortNames();
        final GraphRelationship graphRelationship = entityClass.getAnnotation(GraphRelationship.class);
        if (graphRelationship != null) return graphRelationship.useShortNames();
        return false;
    }

    public List<FieldAccessListener<T, ?>> listenersFor(final Field field) {
        final List<FieldAccessListener<T, ?>> result = new ArrayList<FieldAccessListener<T, ?>>();
        final List<FieldAccessorListenerFactory<T>> fieldAccessListenerFactories = getFieldAccessListenerFactories(field);
        for (final FieldAccessorListenerFactory<T> fieldAccessorListenerFactory : fieldAccessListenerFactories) {
            final FieldAccessListener<T, ?> listener = fieldAccessorListenerFactory.forField(field);
            result.add(listener);
        }
        return result;
    }

    private <E> List<FieldAccessorListenerFactory<E>> getFieldAccessListenerFactories(final Field field) {
        final List<FieldAccessorListenerFactory<E>> result = new ArrayList<FieldAccessorListenerFactory<E>>();
        for (final FieldAccessorListenerFactory<?> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
            if (fieldAccessorListenerFactory.accept(field)) {
                result.add((FieldAccessorListenerFactory<E>) fieldAccessorListenerFactory);
            }
        }
        return result;
    }


    static class FieldAccessorFactoryProvider<E> {
        private final Field field;
        private final FieldAccessorFactory<E> fieldAccessorFactory;
        private final List<FieldAccessorListenerFactory<E>> fieldAccessorListenerFactories;

        FieldAccessorFactoryProvider(final Field field, final FieldAccessorFactory<E> fieldAccessorFactory, final List<FieldAccessorListenerFactory<E>> fieldAccessorListenerFactories) {
            this.field = field;
            this.fieldAccessorFactory = fieldAccessorFactory;
            this.fieldAccessorListenerFactories = fieldAccessorListenerFactories;
        }

        public FieldAccessor<E, ?> accessor() {
            if (fieldAccessorFactory == null) return null;
            return fieldAccessorFactory.forField(field);
        }

        public List<FieldAccessListener<E, ?>> listeners() {
            if (fieldAccessorListenerFactories == null) return null;
            final List<FieldAccessListener<E, ?>> listeners = new ArrayList<FieldAccessListener<E, ?>>();
            for (final FieldAccessorListenerFactory<E> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
                listeners.add(fieldAccessorListenerFactory.forField(field));
            }
            return listeners;
        }

        public Field getField() {
            return field;
        }
    }


    private final Map<Class<?>, List<FieldAccessorFactoryProvider<?>>> acessorFactoryProviderCache = new HashMap<Class<?>, List<FieldAccessorFactoryProvider<?>>>();

    public Map<Field, FieldAccessor<?, ?>> accessorsFor(final Class<?> type) {
        final List<FieldAccessorFactoryProvider<?>> fieldAccessorFactories = getCachedFieldAccessorFactories(type);
        final Map<Field, FieldAccessor<?, ?>> result = new HashMap<Field, FieldAccessor<?, ?>>();
        for (final FieldAccessorFactoryProvider<?> fieldAccessorFactoryProvider : fieldAccessorFactories) {
            final FieldAccessor<?, ?> accessor = fieldAccessorFactoryProvider.accessor();
            result.put(fieldAccessorFactoryProvider.getField(), accessor);
        }
        return result;
    }

    public Map<Field, List<FieldAccessListener>> listenersFor(final Class<?> type) {
        final List<FieldAccessorFactoryProvider<?>> fieldAccessorFactories = getCachedFieldAccessorFactories(type);
        final Map<Field, List<FieldAccessListener>> result = new HashMap<Field, List<FieldAccessListener>>();
        for (final FieldAccessorFactoryProvider<?> fieldAccessorFactoryProvider : fieldAccessorFactories) {
            final List<FieldAccessListener> listeners = (List<FieldAccessListener>) fieldAccessorFactoryProvider.listeners();
            result.put(fieldAccessorFactoryProvider.getField(), listeners);
        }
        return result;
    }

    private synchronized List<FieldAccessorFactoryProvider<?>> getCachedFieldAccessorFactories(final Class<?> type) {
        final List<FieldAccessorFactoryProvider<?>> fieldAccessorFactoryProviders = acessorFactoryProviderCache.get(type);
        if (fieldAccessorFactoryProviders != null) return fieldAccessorFactoryProviders;
        final List<FieldAccessorFactoryProvider<?>> fieldAccessorFactories = new ArrayList<FieldAccessorFactoryProvider<?>>();
        ReflectionUtils.doWithFields(type, new ReflectionUtils.FieldCallback() {
            public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                final FieldAccessorFactory<?> factory = factoryForField(field);
                final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(field);
                fieldAccessorFactories.add(new FieldAccessorFactoryProvider(field, factory, listenerFactories));

            }
        });
        acessorFactoryProviderCache.put(type, fieldAccessorFactories);
        return fieldAccessorFactories;
    }
}