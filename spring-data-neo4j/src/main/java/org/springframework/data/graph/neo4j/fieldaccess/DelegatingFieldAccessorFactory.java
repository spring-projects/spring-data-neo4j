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
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;


public abstract class DelegatingFieldAccessorFactory<T> implements FieldAccessorFactory<T> {

	private final static Log log = LogFactory.getLog(DelegatingFieldAccessorFactory.class);

	protected final GraphDatabaseContext graphDatabaseContext;

    protected abstract Collection<FieldAccessorListenerFactory<?>> createListenerFactories();

    protected abstract Collection<? extends FieldAccessorFactory<?>> createAccessorFactories();

    public DelegatingFieldAccessorFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.fieldAccessorFactories.addAll(createAccessorFactories());
        this.fieldAccessorListenerFactories.addAll(createListenerFactories());
    }

    
    public GraphDatabaseContext getGraphDatabaseContext() {
		return graphDatabaseContext;
	}

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
        final NodeEntity graphEntity = entityClass.getAnnotation(NodeEntity.class);
        if (graphEntity != null) return graphEntity.useShortNames();
        final RelationshipEntity graphRelationship = entityClass.getAnnotation(RelationshipEntity.class);
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




    private final Map<Class<?>, FieldAccessorFactoryProviders> acessorFactoryProviderCache = new HashMap<Class<?>, FieldAccessorFactoryProviders>();

    public <T> FieldAccessorFactoryProviders<T> accessorFactoriesFor(final Class<T> type) {
        synchronized (this) {
            final FieldAccessorFactoryProviders<T> fieldAccessorFactoryProviders = acessorFactoryProviderCache.get(type);
            if (fieldAccessorFactoryProviders != null) return fieldAccessorFactoryProviders;
            final FieldAccessorFactoryProviders<T> newFieldAccessorFactories = new FieldAccessorFactoryProviders<T>(type);
            ReflectionUtils.doWithFields(type, new ReflectionUtils.FieldCallback() {
                public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                    final FieldAccessorFactory<?> factory = factoryForField(field);
                    final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(field);
                    newFieldAccessorFactories.add(field, factory, listenerFactories);
                }
            });
            acessorFactoryProviderCache.put(type, newFieldAccessorFactories);
            return newFieldAccessorFactories;
        }
    }

}