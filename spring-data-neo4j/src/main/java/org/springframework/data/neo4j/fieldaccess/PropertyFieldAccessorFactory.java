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
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class PropertyFieldAccessorFactory implements FieldAccessorFactory {

    private final GraphDatabaseContext graphDatabaseContext;

    public PropertyFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty f) {
        return f.isNeo4jPropertyType();
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty field) {
        return new PropertyFieldAccessor(graphDatabaseContext, field);
    }

    public static class PropertyFieldAccessor implements FieldAccessor {
        protected final GraphDatabaseContext graphDatabaseContext;
        protected final Neo4jPersistentProperty property;
        protected final String propertyName;
        protected final Class<?> fieldType;
        private ConversionService conversionService;

        public PropertyFieldAccessor(GraphDatabaseContext graphDatabaseContext, Neo4jPersistentProperty property) {
            this.graphDatabaseContext = graphDatabaseContext;
            conversionService = graphDatabaseContext.getConversionService();
            this.property = property;
            this.propertyName = property.getNeo4jPropertyName();
            this.fieldType = property.getType() ;
        }

        @Override
        public boolean isWriteable(final Object entity) {
            return true;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal) {
            final PropertyContainer propertyContainer = graphDatabaseContext.getPersistentState(entity);
            if (newVal==null) {
                propertyContainer.removeProperty(propertyName);
            } else {
                propertyContainer.setProperty(propertyName, newVal);
            }
            return newVal;
        }

        @Override
        public final Object getValue(final Object entity) {
            return doReturn(doGetValue(entity));
        }

        protected Object doGetValue(final Object entity) {
            PropertyContainer element = graphDatabaseContext.getPersistentState(entity);
            if (element.hasProperty(propertyName)) {
                Object value = element.getProperty(propertyName);
                if (value == null || fieldType.isInstance(value)) return value;
                if (graphDatabaseContext.getConversionService() !=null) {
                    return graphDatabaseContext.getConversionService().convert(value, fieldType);
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
