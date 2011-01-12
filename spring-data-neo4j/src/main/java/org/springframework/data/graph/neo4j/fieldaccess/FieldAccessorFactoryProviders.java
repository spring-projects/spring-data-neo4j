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
        private final Field field;
        private final FieldAccessorFactory<E> fieldAccessorFactory;
        private final List<FieldAccessorListenerFactory<E>> fieldAccessorListenerFactories;

        FieldAccessorFactoryProvider(final Field field, final FieldAccessorFactory<E> fieldAccessorFactory, final List<FieldAccessorListenerFactory<E>> fieldAccessorListenerFactories) {
            this.field = field;
            this.fieldAccessorFactory = fieldAccessorFactory;
            this.fieldAccessorListenerFactories = fieldAccessorListenerFactories;
        }

        public FieldAccessor<E> accessor() {
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

    private final Class<T> type;
    private final List<FieldAccessorFactoryProvider<T>> fieldAccessorFactoryProviders = new ArrayList<FieldAccessorFactoryProvider<T>>();
    private final IdFieldAccessorFactory idFieldAccessorFactory;
    private Field idField;

    FieldAccessorFactoryProviders(Class<T> type) {
        this.type = type;
        idFieldAccessorFactory = new IdFieldAccessorFactory();
    }

    public Map<Field, FieldAccessor<T>> getFieldAccessors() {
        final Map<Field, FieldAccessor<T>> result = new HashMap<Field, FieldAccessor<T>>();
        for (final FieldAccessorFactoryProvider<T> fieldAccessorFactoryProvider : fieldAccessorFactoryProviders) {
            final FieldAccessor<T> accessor = fieldAccessorFactoryProvider.accessor();
            result.put(fieldAccessorFactoryProvider.getField(), accessor);
        }
        return result;
    }

    public Map<Field, List<FieldAccessListener<T,?>>> getFieldAccessListeners() {
        final Map<Field, List<FieldAccessListener<T,?>>> result = new HashMap<Field, List<FieldAccessListener<T,?>>>();
        for (final FieldAccessorFactoryProvider<T> fieldAccessorFactoryProvider : fieldAccessorFactoryProviders) {
            final List<FieldAccessListener<T,?>> listeners = (List<FieldAccessListener<T,?>>) fieldAccessorFactoryProvider.listeners();
            result.put(fieldAccessorFactoryProvider.getField(), listeners);
        }
        return result;
    }

    public void add(Field field, FieldAccessorFactory<?> fieldAccessorFactory, List<FieldAccessorListenerFactory> listenerFactories) {
        fieldAccessorFactoryProviders.add(new FieldAccessorFactoryProvider(field, fieldAccessorFactory, listenerFactories));
        if (idFieldAccessorFactory.accept(field)) this.idField = field;
    }

    public Field getIdField() {
        return idField;
    }
}
