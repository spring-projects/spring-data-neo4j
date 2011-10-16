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

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.helpers.collection.IteratorUtil;

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.neo4j.support.Neo4jTemplate;

/**
 * This accessor factory creates {@link DynamicPropertiesFieldAccessor}s for @NodeEntity properties of type
 * {@link DynamicProperties}.
 */
public class DynamicPropertiesFieldAccessorFactory implements FieldAccessorFactory {

    private final Neo4jTemplate template;

    public DynamicPropertiesFieldAccessorFactory(final Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public boolean accept(Neo4jPersistentProperty f) {
        return DynamicProperties.class.isAssignableFrom(f.getType());
    }

    @Override
    public FieldAccessor forField(Neo4jPersistentProperty field) {
        return new DynamicPropertiesFieldAccessor(template,
                field.getNeo4jPropertyName(), field);
    }

    public static class DynamicPropertiesFieldAccessor implements FieldAccessor {
        private final String propertyNamePrefix;
        private final Neo4jPersistentProperty field;
        private final Neo4jTemplate template;

        public DynamicPropertiesFieldAccessor(Neo4jTemplate template, String propertyName, Neo4jPersistentProperty field) {
            this.template = template;
            this.propertyNamePrefix = propertyName;
            this.field = field;
        }

        @Override
        public Object setValue(final Object entity, final Object newVal) {
        	final PropertyContainer propertyContainer = template.getPersistentState(entity);
        	PrefixedDynamicProperties dynamicProperties;
            if (newVal instanceof ManagedPrefixedDynamicProperties) {
                // newVal is already a managed container
                dynamicProperties = (ManagedPrefixedDynamicProperties) newVal;
            }
            else {
                // newVal is not a managed prefixed container and therefore contains
                // pure key/values that must be converted to a prefixed form
        		dynamicProperties = new PrefixedDynamicProperties(propertyNamePrefix);
                if (newVal != null) {
                    DynamicProperties newPropertiesVal = (DynamicProperties) newVal;
                    for (String key : newPropertiesVal.getPropertyKeys()) {
                        dynamicProperties.setProperty(key, newPropertiesVal.getProperty(key));
                    }
                }
            }


            Set<String> dynamicProps = dynamicProperties.getPrefixedPropertyKeys();
            Set<String> nodeProps = new HashSet<String>();
            IteratorUtil.addToCollection(propertyContainer.getPropertyKeys(), nodeProps);

            // Get the properties that are not present in the DynamicProperties container anymore
            // by removing all present keys from the actual node properties.
            for (String prop : dynamicProps) {
            	nodeProps.remove(prop);
            }
            
            // nodeProps now contains the properties that are present on the node, but not in the DynamicProperties -
            // in other words: properties that have been removed. Remove them from the node as well.
			for(String removedKey : nodeProps) {
				if (dynamicProperties.isPrefixedKey(removedKey)) {
					propertyContainer.removeProperty(removedKey);
				}
			}
            
			// Add all properties to the propertyContainer
            for (String key : dynamicProps) {
                propertyContainer.setProperty(key, dynamicProperties.getPrefixedProperty(key));
            }
            return newVal;
        }

        @Override
        public Object getValue(final Object entity) {
            PropertyContainer element = template.getPersistentState(entity);
            ManagedPrefixedDynamicProperties props = ManagedPrefixedDynamicProperties.create(propertyNamePrefix, field, entity, template,this);
            for (String key : element.getPropertyKeys()) {
                props.setPropertyIfPrefixed(key, element.getProperty(key));
            }
            return DoReturn.doReturn(props);
        }

        @Override
        public boolean isWriteable(final Object entity) {
            return true;
        }

		@Override
		public Object getDefaultImplementation() {
			return new DynamicPropertiesContainer();
		}
    }
}
