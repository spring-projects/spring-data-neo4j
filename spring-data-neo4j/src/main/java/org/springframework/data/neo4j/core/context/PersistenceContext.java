/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.context;

import java.util.Collection;

import org.apiguardian.api.API;

/**
 * Represents the state of varies instances being tracked. Those include:
 * <ul>
 * <li>All nodes</li>
 * <li>All relationships</li>
 * </ul>
 * including properties of them.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface PersistenceContext {

	/**
	 * Registers an entity within the context and starts tracking the entitys state.
	 *
	 * @param entity The entity to register
	 */
	void register(Object entity);

	/**
	 * Removes an entity from this context and stops tracking the entitys state.
	 *
	 * @param managedEntity The entity to remove
	 */
	void deregister(Object managedEntity);

	/**
	 * Calculates the deltas for each registered object and returns them grouped by the object identifying function
	 * defined in
	 * {@link org.springframework.data.neo4j.core.context.tracking.EntityTrackingStrategy#getObjectIdentifier(Object)} or
	 * one of its implementations.
	 *
	 * @param objects to calculate and return changes for
	 * @return All change events that got registered since registration.
	 */
	Collection<DefaultPersistenceContext.EntityChanges> getEntityChanges(Object... objects);
}
