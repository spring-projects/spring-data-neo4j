/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
