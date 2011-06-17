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

package org.springframework.data.graph.neo4j.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * 
 * @author Oliver Gierke
 */
public class Neo4JMappingContext extends AbstractMappingContext<Neo4JPersistentEntityImpl<?>, Neo4JPersistentProperty> {

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> Neo4JPersistentEntityImpl<?> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new Neo4JPersistentEntityImpl<T>(typeInformation);
	}

	/* (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(java.lang.reflect.Field, java.beans.PropertyDescriptor, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4JPersistentProperty createPersistentProperty(Field field, PropertyDescriptor descriptor,
			Neo4JPersistentEntityImpl<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new Neo4JPersistentPropertyImpl(field, descriptor, owner, simpleTypeHolder);
	}
}
