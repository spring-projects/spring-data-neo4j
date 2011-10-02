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
import org.springframework.data.neo4j.support.GraphDatabaseContext;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
public class ConvertingNodePropertyFieldAccessorFactory implements FieldAccessorFactory {


    private final GraphDatabaseContext graphDatabaseContext;
    private ConversionService conversionService;

    public ConvertingNodePropertyFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
        this.conversionService = graphDatabaseContext.getConversionService();
    }

    
	@Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return property.isSerializableField(conversionService) && property.isDeserializableField(conversionService);
    }

    @Override
    public FieldAccessor forField(final Neo4jPersistentProperty property) {
        return new ConvertingNodePropertyFieldAccessor(property,graphDatabaseContext);
    }

    public static class ConvertingNodePropertyFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {

        public ConvertingNodePropertyFieldAccessor(Neo4jPersistentProperty property, GraphDatabaseContext graphDatabaseContext) {
            super(graphDatabaseContext, property);
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
            return graphDatabaseContext.getConversionService().convert(newVal, String.class);
        }

        private Object deserializePropertyValue(final Object value) {
            return graphDatabaseContext.getConversionService().convert(value, fieldType);
        }

    }
}
