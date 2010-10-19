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

package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.datastore.graph.api.GraphBacked;

import java.lang.reflect.Field;

import static org.springframework.datastore.graph.neo4j.fieldaccess.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class PropertyFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {
    @Override
    public boolean accept(final Field f) {
        return isNeo4jPropertyType(f.getType());
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>, ?> forField(final Field field) {
        return new PropertyFieldAccessor(field);
    }

    private boolean isNeo4jPropertyType(final Class<?> fieldType) {
        // todo: add array support
        return fieldType.isPrimitive()
                || fieldType.equals(String.class)
                || fieldType.equals(Character.class)
                || fieldType.equals(Boolean.class)
                || (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType))
                || (fieldType.isArray() && !fieldType.getComponentType().isArray() && isNeo4jPropertyType(fieldType.getComponentType()));
    }

    public static class PropertyFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>, Object> {
        protected final Field field;

        public PropertyFieldAccessor(final Field field) {
            this.field = field;
        }

        @Override
        public boolean isWriteable(final GraphBacked<PropertyContainer> graphBacked) {
            return true;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            final PropertyContainer propertyContainer = graphBacked.getUnderlyingState();
            if (newVal==null) {
                propertyContainer.removeProperty(getPropertyName());
            } else {
                propertyContainer.setProperty(getPropertyName(), newVal);
            }
            return newVal;
        }

        @Override
        public final Object getValue(final GraphBacked<PropertyContainer> graphBacked) {
            return doReturn(doGetValue(graphBacked));
        }

        protected Object doGetValue(final GraphBacked<PropertyContainer> graphBacked) {
            return graphBacked.getUnderlyingState().getProperty(getPropertyName(), getDefaultValue(field.getType()));
        }

        private String getPropertyName() {
            return DelegatingFieldAccessorFactory.getNeo4jPropertyName(field);
        }

        private Object getDefaultValue(final Class<?> type) {
            if (type.isPrimitive()) {
                if (type.equals(boolean.class)) return false;
                return 0;
            }
            return null;
        }

    }
}
