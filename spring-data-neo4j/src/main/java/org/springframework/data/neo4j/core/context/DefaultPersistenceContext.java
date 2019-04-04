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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.neo4j.core.context.tracking.EntityChangeEvent;
import org.springframework.data.neo4j.core.context.tracking.EntityComparisonStrategy;
import org.springframework.data.neo4j.core.context.tracking.EntityTrackingStrategy;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.core.schema.Schema;

/**
 * TODO explain what the persistence context should do other than being the plural of EntityTrackingStrategy :D
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@Slf4j
public class DefaultPersistenceContext implements PersistenceContext {

	/**
	 * The schema is required to find out which properties to track with the {@link #entityTrackingStrategy}.
	 */
	private final Schema schema;

	private final EntityTrackingStrategy entityTrackingStrategy;

	private final Set<Integer> registeredObjectIds = new HashSet<>();

	public DefaultPersistenceContext(final Schema schema) {
		this.schema = schema;
		this.entityTrackingStrategy = getEntityTrackingStrategy();
	}

	@Override
	public void register(Object entity) {

		Objects.requireNonNull(entity, "Cannot track null-entity!");

		int identityOfEntity = getIdentityOf(entity);

		if (registeredObjectIds.contains(identityOfEntity)) {
			log.info("Object " + entity + " was already registered");
			return;
		}

		NodeDescription nodeDescription = schema.getNodeDescription(entity.getClass())
				.orElseThrow(() -> new IllegalStateException("Cannot track entity that has no schema entry!"));

		registeredObjectIds.add(identityOfEntity);
		entityTrackingStrategy.track(nodeDescription, entity);
	}

	@Override
	public void deregister(Object managedEntity) {

		int identityOfEntity = getIdentityOf(managedEntity);
		if (!registeredObjectIds.contains(identityOfEntity)) {
			log.warn("Cannot deregister " + managedEntity + " because it were never registered");
			return;
		}

		entityTrackingStrategy.untrack(managedEntity);
	}

	@Override
	public Collection<EntityChanges> getEntityChanges(Object... objects) {

		List<EntityChanges> entityChanges = new ArrayList<>();

		for (Object object : objects) {
			Collection<EntityChangeEvent> changeEvents = entityTrackingStrategy.getAggregatedEntityChangeEvents(object);
			entityChanges.add(new EntityChanges(object, changeEvents));
		}

		return entityChanges;
	}

	EntityTrackingStrategy getEntityTrackingStrategy() {
		// todo choose the right / fitting implementation
		return new EntityComparisonStrategy();
	}

	private int getIdentityOf(Object entity) {
		return entityTrackingStrategy.getObjectIdentifier(entity);
	}

	@Getter
	static class EntityChanges {
		private final Object entity;

		private final Collection<EntityChangeEvent> changeEvents;

		EntityChanges(Object entity, Collection<EntityChangeEvent> changeEvents) {
			this.entity = entity;
			this.changeEvents = changeEvents;
		}
	}

}
