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

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

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
    public boolean accept(final Neo4JPersistentProperty f) {
        return f.isNeo4jPropertyType();
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>> forField(final Neo4JPersistentProperty field) {
        return new PropertyFieldAccessor(conversionService, field);
    }

    public static class PropertyFieldAccessor implements FieldAccessor<GraphBacked<PropertyContainer>> {
        private final ConversionService conversionService;
        private final Neo4JPersistentProperty property;
        protected final String propertyName;
        protected final Class<?> fieldType;

        public PropertyFieldAccessor(ConversionService conversionService, Neo4JPersistentProperty property) {
            this.conversionService = conversionService;
            this.property = property;
            this.propertyName = property.getNeo4jPropertyName();
            this.fieldType = property.getType() ;
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

		@Override
		public Object getDefaultImplementation() {
			return null;
		}

    }
}
