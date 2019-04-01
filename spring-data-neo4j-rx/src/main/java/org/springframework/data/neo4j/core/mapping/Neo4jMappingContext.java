/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping;

import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * {@link org.springframework.data.mapping.context.MappingContext} implementation for Neo4j.
 *
 * @author Michael J. Simons
 */
public class Neo4jMappingContext extends AbstractMappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> {

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentEntity(org.springframework.data.util.TypeInformation)
	 */
	@Override
	protected <T> Neo4jPersistentEntity<?> createPersistentEntity(TypeInformation<T> typeInformation) {

		return new DefaultNeo4jPersistentEntity<>(typeInformation);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.context.AbstractMappingContext#createPersistentProperty(org.springframework.data.mapping.model.Property, org.springframework.data.mapping.model.MutablePersistentEntity, org.springframework.data.mapping.model.SimpleTypeHolder)
	 */
	@Override
	protected Neo4jPersistentProperty createPersistentProperty(Property property,
		Neo4jPersistentEntity<?> neo4jPersistentProperties, SimpleTypeHolder simpleTypeHolder) {

		return new DefaultNeo4jPersistentProperty(property, neo4jPersistentProperties, simpleTypeHolder);
	}
}
