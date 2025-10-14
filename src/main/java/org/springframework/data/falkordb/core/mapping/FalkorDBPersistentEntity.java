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
