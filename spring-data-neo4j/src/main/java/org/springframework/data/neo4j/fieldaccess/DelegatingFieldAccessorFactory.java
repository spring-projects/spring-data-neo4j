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
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.neo4j.mapping.Neo4JPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.util.TypeInformation;

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
    public boolean accept(final Neo4JPersistentProperty f) {
        return true;
    }

    final Collection<FieldAccessorFactory<?>> fieldAccessorFactories = new ArrayList<FieldAccessorFactory<?>>();
    final Collection<FieldAccessorListenerFactory<?>> fieldAccessorListenerFactories = new ArrayList<FieldAccessorListenerFactory<?>>();

    public FieldAccessor forField(final Neo4JPersistentProperty property) {
        final FieldAccessorFactory<?> factory = factoryForField(property);
        return factory != null ? factory.forField(property) : null;
    }

    private <E> FieldAccessorFactory<E> factoryForField(final Neo4JPersistentProperty property) {
        if (property.isSyntheticField()) return null;
        for (final FieldAccessorFactory<?> fieldAccessorFactory : fieldAccessorFactories) {
            if (fieldAccessorFactory.accept(property)) {
                if (log.isInfoEnabled()) log.info("Factory " + fieldAccessorFactory + " used for field: " + property);
                return (FieldAccessorFactory<E>) fieldAccessorFactory;
            }
        }
        if (log.isWarnEnabled()) log.warn("No FieldAccessor configured for field: " + property);
        return null;
    }

    public List<FieldAccessListener<T, ?>> listenersFor(final Neo4JPersistentProperty property) {
        final List<FieldAccessListener<T, ?>> result = new ArrayList<FieldAccessListener<T, ?>>();
        final List<FieldAccessorListenerFactory<T>> fieldAccessListenerFactories = getFieldAccessListenerFactories(property);
        for (final FieldAccessorListenerFactory<T> fieldAccessorListenerFactory : fieldAccessListenerFactories) {
            final FieldAccessListener<T, ?> listener = fieldAccessorListenerFactory.forField(property);
            result.add(listener);
        }
        return result;
    }

    private <E> List<FieldAccessorListenerFactory<E>> getFieldAccessListenerFactories(final Neo4JPersistentProperty property) {
        final List<FieldAccessorListenerFactory<E>> result = new ArrayList<FieldAccessorListenerFactory<E>>();
        for (final FieldAccessorListenerFactory<?> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
            if (fieldAccessorListenerFactory.accept(property)) {
                result.add((FieldAccessorListenerFactory<E>) fieldAccessorListenerFactory);
            }
        }
        return result;
    }




    private final Map<TypeInformation<?>, FieldAccessorFactoryProviders> accessorFactoryProviderCache = new HashMap<TypeInformation<?>, FieldAccessorFactoryProviders>();

    public <T> FieldAccessorFactoryProviders<T> accessorFactoriesFor(final Neo4JPersistentEntity<?> type) {
        synchronized (this) {
            final TypeInformation<?> typeInformation = type.getTypeInformation();
            final FieldAccessorFactoryProviders<T> fieldAccessorFactoryProviders = accessorFactoryProviderCache.get(typeInformation);
            if (fieldAccessorFactoryProviders != null) return fieldAccessorFactoryProviders;
            final FieldAccessorFactoryProviders<T> newFieldAccessorFactories = new FieldAccessorFactoryProviders<T>(typeInformation);
            type.doWithProperties(new PropertyHandler<Neo4JPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4JPersistentProperty property) {
                    final FieldAccessorFactory<?> factory = factoryForField(property);
                    final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(property);
                    newFieldAccessorFactories.add(property, factory, listenerFactories);
                }
            });
            type.doWithAssociations(new AssociationHandler<Neo4JPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4JPersistentProperty> association) {
                    final Neo4JPersistentProperty property = association.getInverse();
                    final FieldAccessorFactory<?> factory = factoryForField(property);
                    final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(property);
                    newFieldAccessorFactories.add(property, factory, listenerFactories);
                }
            });
            accessorFactoryProviderCache.put(typeInformation, newFieldAccessorFactories);
            return newFieldAccessorFactories;
        }
    }

}