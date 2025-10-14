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

import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.mapping.context.AbstractMappingContext;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.TypeInformation;

/**
 * Default implementation of {@link FalkorDBMappingContext}.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public class DefaultFalkorDBMappingContext
		extends AbstractMappingContext<DefaultFalkorDBPersistentEntity<?>, FalkorDBPersistentProperty>
		implements FalkorDBMappingContext {

	@Override
	protected <T> DefaultFalkorDBPersistentEntity<T> createPersistentEntity(TypeInformation<T> typeInformation) {
		return new DefaultFalkorDBPersistentEntity<>(typeInformation);
	}

	@Override
	protected FalkorDBPersistentProperty createPersistentProperty(Property property,
			DefaultFalkorDBPersistentEntity<?> owner, SimpleTypeHolder simpleTypeHolder) {
		return new DefaultFalkorDBPersistentProperty(property, owner, simpleTypeHolder);
	}

	@Override
	public DefaultFalkorDBPersistentEntity<?> getRequiredPersistentEntity(Class<?> type) {
		DefaultFalkorDBPersistentEntity<?> entity = (DefaultFalkorDBPersistentEntity<?>) getPersistentEntity(type);
		if (entity == null) {
			throw new IllegalArgumentException("No persistent entity found for type: " + type.getName());
		}
		return entity;
	}

	@Override
	public boolean hasPersistentEntityFor(Class<?> type) {
		return getPersistentEntity(type) != null;
	}

	@Override
	protected boolean shouldCreatePersistentEntityFor(TypeInformation<?> type) {
		// Create entities for classes annotated with @Node or explicitly requested
		return type.getType().isAnnotationPresent(Node.class) || super.shouldCreatePersistentEntityFor(type);
	}

}
