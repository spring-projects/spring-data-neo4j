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

import org.springframework.data.mapping.PersistentProperty;

/**
 * FalkorDB-specific {@link PersistentProperty} that provides metadata about FalkorDB
 * entity properties.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
public interface FalkorDBPersistentProperty extends PersistentProperty<FalkorDBPersistentProperty> {

	/**
	 * Returns the graph property name for this property. This takes into account the
	 * {@link org.springframework.data.falkordb.core.schema.Property} annotation.
	 * @return the graph property name
	 */
	String getGraphPropertyName();

	/**
	 * Returns whether this property represents a relationship.
	 * @return true if this is a relationship property
	 */
	boolean isRelationship();

	/**
	 * Returns whether this property is the internal FalkorDB ID.
	 * @return true if this is the internal ID property
	 */
	boolean isInternalIdProperty();

	/**
	 * Returns whether this property represents a generated value.
	 * @return true if this is a generated value
	 */
	boolean isGeneratedValue();

	/**
	 * Returns the relationship type if this is a relationship property.
	 * @return the relationship type or null if not a relationship
	 */
	String getRelationshipType();

}
