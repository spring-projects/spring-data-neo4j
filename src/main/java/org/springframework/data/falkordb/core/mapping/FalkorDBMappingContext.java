/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
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
