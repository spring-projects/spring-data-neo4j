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
import java.util.Objects;

import org.springframework.data.neo4j.core.context.tracking.EntityChangeEvent;
import org.springframework.data.neo4j.core.context.tracking.EntityComparisonStrategy;
import org.springframework.data.neo4j.core.context.tracking.EntityTrackingStrategy;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Schema;

/**
 * @author Michael J. Simons
 */
public class DefaultPersistenceContext implements PersistenceContext {

	/**
	 * The schema is required to find out which properties to track with the {@link #entityTrackingStrategy}.
	 */
	private final Schema schema;

	private final EntityTrackingStrategy entityTrackingStrategy;

	public DefaultPersistenceContext(final Schema schema) {

		this.schema = schema;
		//todo choose the right / fitting implementation
		this.entityTrackingStrategy = new EntityComparisonStrategy();
	}

	@Override
	public void register(Object entity) {

		Objects.requireNonNull(entity, "Cannot track null-entity!");
		NodeDescription nodeDescription = schema.getNodeDescription(entity.getClass())
			.orElseThrow(() -> new IllegalStateException("Cannot track entity that has no schema entry!"));
		entityTrackingStrategy.track(nodeDescription, entity);
	}

	@Override
	public void deregister(Object managedEntity) {

		throw new UnsupportedOperationException("Not there yet.");
	}

	private void somethingToGetCalledBeforeSave() {
		getDeltasForEachEntity();
	}

	private Collection<EntityChangeEvent> getDeltasForEachEntity() {
		return entityTrackingStrategy.getAggregatedDelta(new Object());
	}

}
