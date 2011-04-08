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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.GraphBacked;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class PropertyFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {

    private final ConversionService conversionService;

    public PropertyFieldAccessorFactory(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @Override
    public boolean accept(final Field f) {
        return isNeo4jPropertyType(f.getType());
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>> forField(final Field field) {
        return new PropertyFieldAccessor(conversionService,DelegatingFieldAccessorFactory.getNeo4jPropertyName(field),field.getType());
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

    public static class PropertyFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>> {
        private final ConversionService conversionService;
        protected final String propertyName;
        protected final Class<?> fieldType;

        public PropertyFieldAccessor(ConversionService conversionService, String propertyName, Class fieldType) {
            this.conversionService = conversionService;
            this.propertyName = propertyName;
            this.fieldType = fieldType;
        }

        @Override
        public boolean isWriteable(final GraphBacked<PropertyContainer> graphBacked) {
            return true;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            final PropertyContainer propertyContainer = graphBacked.getPersistentState();
            if (newVal==null) {
                propertyContainer.removeProperty(propertyName);
            } else {
                propertyContainer.setProperty(propertyName, newVal);
            }
            return newVal;
        }

        @Override
        public final Object getValue(final GraphBacked<PropertyContainer> graphBacked) {
            return doReturn(doGetValue(graphBacked));
        }

        protected Object doGetValue(final GraphBacked<PropertyContainer> graphBacked) {
            PropertyContainer element = graphBacked.getPersistentState();
            if (element.hasProperty(propertyName)) {
                Object value = element.getProperty(propertyName);
                if (value == null || fieldType.isInstance(value)) return value;
                if (conversionService!=null) {
                    return conversionService.convert(value, fieldType);
                }
                return value;
            }
            return getDefaultValue(fieldType);
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
