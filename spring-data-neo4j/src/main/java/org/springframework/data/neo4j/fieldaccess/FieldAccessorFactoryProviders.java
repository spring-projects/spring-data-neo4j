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

import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Michael Hunger
 * @since 15.10.2010
 */
public class FieldAccessorFactoryProviders<T> {

    static class FieldAccessorFactoryProvider<E> {
        private final Neo4JPersistentProperty property;
        private final FieldAccessorFactory<E> fieldAccessorFactory;
        private final List<FieldAccessorListenerFactory<E>> fieldAccessorListenerFactories;

        FieldAccessorFactoryProvider(final Neo4JPersistentProperty property, final FieldAccessorFactory fieldAccessorFactory, final List fieldAccessorListenerFactories) {
            this.property = property;
            this.fieldAccessorFactory = fieldAccessorFactory;
            this.fieldAccessorListenerFactories = fieldAccessorListenerFactories;
        }

        public FieldAccessor<E> accessor() {
            if (fieldAccessorFactory == null) return null;
            return fieldAccessorFactory.forField(property);
        }

        public List<FieldAccessListener<E, ?>> listeners() {
            if (fieldAccessorListenerFactories == null) return null;
            final List<FieldAccessListener<E, ?>> listeners = new ArrayList<FieldAccessListener<E, ?>>(fieldAccessorListenerFactories.size());
            for (final FieldAccessorListenerFactory<E> fieldAccessorListenerFactory : fieldAccessorListenerFactories) {
                listeners.add(fieldAccessorListenerFactory.forField(property));
            }
            return listeners;
        }

        public Neo4JPersistentProperty getProperty() {
            return property;
        }
    }

    private final TypeInformation<?> type;
    private final List<FieldAccessorFactoryProvider<T>> fieldAccessorFactoryProviders = new ArrayList<FieldAccessorFactoryProvider<T>>();
    private final IdFieldAccessorFactory idFieldAccessorFactory;
    private Neo4JPersistentProperty idProperty;

    FieldAccessorFactoryProviders(TypeInformation<?> type) {
        this.type = type;
        idFieldAccessorFactory = new IdFieldAccessorFactory();
    }

    public Map<Neo4JPersistentProperty, FieldAccessor<T>> getFieldAccessors() {
        final Map<Neo4JPersistentProperty, FieldAccessor<T>> result = new HashMap<Neo4JPersistentProperty, FieldAccessor<T>>(fieldAccessorFactoryProviders.size(),1);
        for (final FieldAccessorFactoryProvider<T> fieldAccessorFactoryProvider : fieldAccessorFactoryProviders) {
            final FieldAccessor<T> accessor = fieldAccessorFactoryProvider.accessor();
            result.put(fieldAccessorFactoryProvider.getProperty(), accessor);
        }
        return result;
    }

    public Map<Neo4JPersistentProperty, List<FieldAccessListener<T,?>>> getFieldAccessListeners() {
        final Map<Neo4JPersistentProperty, List<FieldAccessListener<T,?>>> result = new HashMap<Neo4JPersistentProperty, List<FieldAccessListener<T,?>>>(fieldAccessorFactoryProviders.size(),1);
        for (final FieldAccessorFactoryProvider<T> fieldAccessorFactoryProvider : fieldAccessorFactoryProviders) {
            final List<FieldAccessListener<T,?>> listeners = (List<FieldAccessListener<T,?>>) fieldAccessorFactoryProvider.listeners();
            result.put(fieldAccessorFactoryProvider.getProperty(), listeners);
        }
        return result;
    }

    public void add(Neo4JPersistentProperty property, FieldAccessorFactory<?> fieldAccessorFactory, List<FieldAccessorListenerFactory> listenerFactories) {
        fieldAccessorFactoryProviders.add(new FieldAccessorFactoryProvider(property, fieldAccessorFactory, listenerFactories));
        if (property.isIdProperty()) this.idProperty = property;
    }

    public Neo4JPersistentProperty getIdProperty() {
        return idProperty;
    }
}
