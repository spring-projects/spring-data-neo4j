/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.core.mapping;

import org.springframework.data.mapping.context.MappingContext;

/**
 * FalkorDB-specific {@link MappingContext} that handles entity mapping metadata for
 * FalkorDB graph entities.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface FalkorDBMappingContext
		extends MappingContext<DefaultFalkorDBPersistentEntity<?>, FalkorDBPersistentProperty> {

	/**
	 * Returns the {@link DefaultFalkorDBPersistentEntity} for the given type.
	 * @param type the entity type
	 * @return the persistent entity
	 * @throws IllegalArgumentException if no entity is found for the type
	 */
	DefaultFalkorDBPersistentEntity<?> getRequiredPersistentEntity(Class<?> type);

	/**
	 * Returns whether the mapping context has a persistent entity for the given type.
	 * @param type the entity type
	 * @return true if an entity exists for the type
	 */
	boolean hasPersistentEntityFor(Class<?> type);

}
