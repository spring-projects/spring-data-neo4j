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

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
public class ConvertingNodePropertyFieldAccessorFactory implements FieldAccessorFactory {


    private final Neo4jTemplate template;

    public ConvertingNodePropertyFieldAccessorFactory(Neo4jTemplate template) {
        this.template = template;
    }

    private ConversionService getConversionService() {
        return template.getConversionService();
    }


	@Override
    public boolean accept(final Neo4jPersistentProperty property) {
        final ConversionService conversionService = getConversionService();
        return property.isSerializablePropertyField(conversionService);
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new ConvertingNodePropertyFieldAccessor(property, template);
    }

    public static class ConvertingNodePropertyFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {

        private final Class<String> targetType = String.class;
        private final PropertyConverter propertyConverter;

        public ConvertingNodePropertyFieldAccessor(Neo4jPersistentProperty property, Neo4jTemplate template) {
            super(template, property);
            propertyConverter = new PropertyConverter(template.getConversionService(),property);
        }

        private boolean doConvert(final Object value) {
            // Do convert by default
            boolean doConvert = true;
            // If the value if of type Object and a neo4j supported type, do not convert it
            if (property.getType().equals(Object.class) && property.isNeo4jPropertyValue(value)) {
                doConvert = false;
            }

            return doConvert;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal, MappingPolicy mappingPolicy) {
            Object value = newVal;
            // Convert the value if it's not of a neo4j supported type
            if (doConvert(value)) {
                value = propertyConverter.serializePropertyValue(value, targetType);
            }
            super.setValue(entity, value, mappingPolicy);
            return newVal;
        }

        @Override
        public Object doGetValue(final Object entity) {
            Object ret = super.doGetValue(entity);
            if (doConvert(ret)) {
                ret = propertyConverter.deserializePropertyValue(ret);
            }

            return ret;
        }

        @Override
        protected Object convertSimplePropertyValue(Object value) {
            return value;
        }


    }

}
