/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.mapping.callback;

import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * Common logic for retrieving entity metadata from the context and incrementing the version property if necessary.
 *
 * @author Michael J. Simons
 * @soundtrack Body Count - Violent Demise: The Last Days
 */
final class OptimisticLockingSupport {

	private final Neo4jMappingContext mappingContext;

	OptimisticLockingSupport(Neo4jMappingContext mappingContext) {
		this.mappingContext = mappingContext;
	}

	Object getAndIncrementVersionPropertyIfNecessary(Object entity) {

		Neo4jPersistentEntity<?> neo4jPersistentEntity = (Neo4jPersistentEntity<?>) mappingContext
				.getRequiredNodeDescription(entity.getClass());

		if (neo4jPersistentEntity.hasVersionProperty()) {
			PersistentPropertyAccessor<Object> propertyAccessor = neo4jPersistentEntity.getPropertyAccessor(entity);
			Neo4jPersistentProperty versionProperty = neo4jPersistentEntity.getRequiredVersionProperty();

			if (!Long.class.isAssignableFrom(versionProperty.getType())) {
				return entity;
			}

			Long versionPropertyValue = (Long) propertyAccessor.getProperty(versionProperty);

			long newVersionValue = 0;
			if (versionPropertyValue != null) {
				newVersionValue = versionPropertyValue + 1;
			}

			propertyAccessor.setProperty(versionProperty, newVersionValue);
		}
		return entity;
	}
}
