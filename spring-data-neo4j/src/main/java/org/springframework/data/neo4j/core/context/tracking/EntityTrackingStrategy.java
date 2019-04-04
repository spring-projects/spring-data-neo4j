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
package org.springframework.data.neo4j.core.context.tracking;

import java.util.Collection;
import java.util.function.Function;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.schema.NodeDescription;

/**
 * A tracking strategy is used to determine if an entity has changed it state over time in a transaction. It is also
 * responsible to generate and return a complete list of all these changes on an entity's attribute level.
 *
 * @author Gerrit Meier
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public interface EntityTrackingStrategy {

	static Function<Object, Integer> getDefaultObjectIdentifier() {
		return System::identityHashCode;
	}

	/**
	 * Register an entity in the entity tracking strategy. {@link NodeDescription} is needed to determine the fields that
	 * are considered mapping relevant.
	 *
	 * @param nodeDescription the "rich" entity's class description.
	 * @param entity the object that should get tracked.
	 */
	void track(NodeDescription nodeDescription, Object entity);

	/**
	 * Remove an entity from tracking. This method should get called after an entity was deleted to have a clean change
	 * history state.
	 *
	 * @param entity to get removed from entity tracking
	 */
	void untrack(Object entity);

	/**
	 * Aggregates all changes that were registered for this entity since the start of tracking.
	 *
	 * @param entity for which a summarized collection of {@EntityChangeEvent}s should get created.
	 * @return all events that occurred since tracking started.
	 */
	Collection<EntityChangeEvent> getAggregatedEntityChangeEvents(Object entity);

	/**
	 * Returns an object identifier for a given entity.
	 *
	 * @param entity the entity to get an unique identifier for.
	 * @return hash or something similar that represents the entity.
	 */
	default int getObjectIdentifier(Object entity) {
		return getDefaultObjectIdentifier().apply(entity);
	}
}
