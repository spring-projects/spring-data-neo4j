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
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.util.TypeInformation;

import java.util.*;


public abstract class DelegatingFieldAccessorFactory implements FieldAccessorFactory {

	private final static Log log = LogFactory.getLog(DelegatingFieldAccessorFactory.class);

	protected final GraphDatabaseContext graphDatabaseContext;

    protected abstract Collection<FieldAccessorListenerFactory> createListenerFactories();

    protected abstract Collection<? extends FieldAccessorFactory> createAccessorFactories();

    public DelegatingFieldAccessorFactory(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.fieldAccessorFactories.addAll(createAccessorFactories());
        this.fieldAccessorListenerFactories.addAll(createListenerFactories());
    }

    
    public GraphDatabaseContext getGraphDatabaseContext() {
		return graphDatabaseContext;
	}

	@Override
    public boolean accept(final Neo4jPersistentProperty f) {
        return true;
    }

    final Collection<FieldAccessorFactory> fieldAccessorFactories = new ArrayList<FieldAccessorFactory>();
    final Collection<FieldAccessorListenerFactory> fieldAccessorListenerFactories = new ArrayList<FieldAccessorListenerFactory>();

    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        final FieldAccessorFactory factory = factoryForField(property);
        return factory != null ? factory.forField(property) : null;
    }

    private <E> FieldAccessorFactory factoryForField(final Neo4jPersistentProperty property) {
        if (property.isSyntheticField()) return null;
        for (final FieldAccessorFactory fieldAccessorFactory : fieldAccessorFactories) {
            if (fieldAccessorFactory.accept(property)) {
                if (log.isInfoEnabled()) log.info("Factory " + fieldAccessorFactory + " used for field: " + property);
                return (FieldAccessorFactory) fieldAccessorFactory;
            }
        }
        if (log.isWarnEnabled()) log.warn("No FieldAccessor configured for field: " + property);
        return null;
    }

    public List<FieldAccessListener> listenersFor(final Neo4jPersistentProperty property) {
        final List<FieldAccessListener> result = new ArrayList<FieldAccessListener>();
        final List<FieldAccessorListenerFactory> fieldAccessListenerFactories = getFieldAccessListenerFactories(property);
        for (final FieldAccessorListenerFactory fieldAccessorListenerFactory : fieldAccessListenerFactories) {
            final FieldAccessListener listener = fieldAccessorListenerFactory.forField(property);
            result.add(listener);
        }
        return result;
    }

    private <E> List<FieldAccessorListenerFactory> getFieldAccessListenerFactories(final Neo4jPersistentProperty property) {
        final List<FieldAccessorListenerFactory> result = new ArrayList<FieldAccessorListenerFactory>();
        for (final FieldAccessorListenerFactory fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
            if (fieldAccessorListenerFactory.accept(property)) {
                result.add((FieldAccessorListenerFactory) fieldAccessorListenerFactory);
            }
        }
        return result;
    }




    private final Map<TypeInformation<?>, FieldAccessorFactoryProviders> accessorFactoryProviderCache = new HashMap<TypeInformation<?>, FieldAccessorFactoryProviders>();

    public <T> FieldAccessorFactoryProviders<T> accessorFactoriesFor(final Neo4jPersistentEntity<?> type) {
        synchronized (this) {
            final TypeInformation<?> typeInformation = type.getTypeInformation();
            final FieldAccessorFactoryProviders<T> fieldAccessorFactoryProviders = accessorFactoryProviderCache.get(typeInformation);
            if (fieldAccessorFactoryProviders != null) return fieldAccessorFactoryProviders;
            final FieldAccessorFactoryProviders<T> newFieldAccessorFactories = new FieldAccessorFactoryProviders<T>(typeInformation,graphDatabaseContext);
            type.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                    final FieldAccessorFactory factory = factoryForField(property);
                    final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(property);
                    newFieldAccessorFactories.add(property, factory, listenerFactories);
                }
            });
            type.doWithAssociations(new AssociationHandler<Neo4jPersistentProperty>() {
                @Override
                public void doWithAssociation(Association<Neo4jPersistentProperty> association) {
                    final Neo4jPersistentProperty property = association.getInverse();
                    final FieldAccessorFactory factory = factoryForField(property);
                    final List<FieldAccessorListenerFactory> listenerFactories = (List<FieldAccessorListenerFactory>) getFieldAccessListenerFactories(property);
                    newFieldAccessorFactories.add(property, factory, listenerFactories);
                }
            });
            accessorFactoryProviderCache.put(typeInformation, newFieldAccessorFactories);
            return newFieldAccessorFactories;
        }
    }

}