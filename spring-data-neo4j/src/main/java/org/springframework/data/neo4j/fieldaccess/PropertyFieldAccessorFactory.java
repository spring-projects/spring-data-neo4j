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

import org.neo4j.graphdb.ConstraintViolationException;
import org.neo4j.graphdb.PropertyContainer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import static org.springframework.data.neo4j.support.DoReturn.doReturn;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class PropertyFieldAccessorFactory implements FieldAccessorFactory {

    private final Neo4jTemplate template;

    public PropertyFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty f) {
        return f.isNeo4jPropertyType();
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty field) {
        return new PropertyFieldAccessor(template, field);
    }

    public static class PropertyFieldAccessor implements FieldAccessor {
        protected final Neo4jTemplate template;
        protected final Neo4jPersistentProperty property;
        protected final String propertyName;
        protected final Class<?> fieldType;

        public PropertyFieldAccessor(Neo4jTemplate template, Neo4jPersistentProperty property) {
            this.template = template;
            this.property = property;
            this.propertyName = property.getNeo4jPropertyName();
            this.fieldType = property.getType() ;
        }

        @Override
        public boolean isWriteable(final Object entity) {
            return true;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            final PropertyContainer propertyContainer = template.getPersistentState(entity);
            try {
                if (newVal==null) {
                    propertyContainer.removeProperty(propertyName);
                } else {
                    propertyContainer.setProperty(propertyName, newVal);
                }
            } catch(ConstraintViolationException cve) {
                throw new DataIntegrityViolationException("Unique constraint violated "+property.getOwner().getName()+"."+property.getName()+" new value "+newVal,cve);
            }
            return newVal;
        }

        @Override
        public final Object getValue(final Object entity, MappingPolicy mappingPolicy) {
            return doReturn(doGetValue(entity));
        }

        protected Object doGetValue(final Object entity) {
            PropertyContainer element = template.getPersistentState(entity);
            if (element.hasProperty(propertyName)) {
                Object value = element.getProperty(propertyName);
                if (value == null || fieldType.isInstance(value)) return value;
                return convertSimplePropertyValue(value);
            }
            return getDefaultValue(fieldType);
        }

        protected Object convertSimplePropertyValue(Object value) {
            if (template.getConversionService() !=null) {
                return template.getConversionService().convert(value, fieldType);
            }
            return value;
        }

        private Object getDefaultValue(final Class<?> type) {
            return property.getDefaultValue(template.getConversionService(), type);
        }

		@Override
		public Object getDefaultValue() {
			return getDefaultValue(fieldType);
		}

    }
}
