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
        return property.isSerializableField(conversionService) && property.isDeserializableField(conversionService);
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new ConvertingNodePropertyFieldAccessor(property, template);
    }

    public static class ConvertingNodePropertyFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {

        public ConvertingNodePropertyFieldAccessor(Neo4jPersistentProperty property, Neo4jTemplate template) {
            super(template, property);
        }

        @Override
        public Object setValue(final Object entity, final Object newVal) {
            super.setValue(entity, serializePropertyValue(newVal));
            return newVal;
        }

        @Override
        public Object doGetValue(final Object entity) {
            return deserializePropertyValue(super.doGetValue(entity));
        }

        private Object serializePropertyValue(final Object newVal) {
            return template.getConversionService().convert(newVal, String.class);
        }

        private Object deserializePropertyValue(final Object value) {
            return template.getConversionService().convert(value, fieldType);
        }

    }
}
