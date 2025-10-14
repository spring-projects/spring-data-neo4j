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
