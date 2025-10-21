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

import org.springframework.data.mapping.PersistentEntity;

/**
 * FalkorDB-specific {@link PersistentEntity} that provides metadata about FalkorDB graph
 * entities.
 *
 * @param <T> the entity type
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface FalkorDBPersistentEntity<T> extends PersistentEntity<T, FalkorDBPersistentProperty> {

	/**
	 * Returns the primary label for this entity as defined by the
	 * {@link org.springframework.data.falkordb.core.schema.Node} annotation.
	 * @return the primary label
	 */
	String getPrimaryLabel();

	/**
	 * Returns all labels for this entity.
	 * @return all labels
	 */
	String[] getLabels();

	/**
	 * Returns whether this entity represents a graph node.
	 * @return true if this is a node entity
	 */
	boolean isNodeEntity();

	/**
	 * Returns whether this entity has a composite primary key.
	 * @return true if the entity has a composite key
	 */
	boolean hasCompositeId();

}
