/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * This stores all processed nested relations and objects during save of objects so that the recursive descent can be
 * stopped accordingly.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Heart Attack No. 1
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public final class NestedRelationshipProcessingStateMachine {

	/**
	 * Valid processing states.
	 */
	public enum ProcessState {
		PROCESSED_NONE, PROCESSED_BOTH, PROCESSED_ALL_RELATIONSHIPS, PROCESSED_ALL_VALUES
	}

	private final StampedLock lock = new StampedLock();

	private final Neo4jMappingContext mappingContext;

	/**
	 * The set of already processed relationships.
	 */
	private final Set<RelationshipDescriptionWithSourceId> processedRelationshipDescriptions = new HashSet<>();

	/**
	 * A map of processed objects pointing towards a possible new instance of themselves.
	 * This will happen for immutable entities.
	 */
	private final Map<Integer, Object> processedObjectsAlias = new HashMap<>();

	/**
	 * A map pointing from a processed object to the internal id.
	 * This will be useful during the persistence to avoid another DB network round-trip.
	 */
	private final Map<Integer, String> processedObjectsIds = new HashMap<>();

	public NestedRelationshipProcessingStateMachine(final Neo4jMappingContext mappingContext) {

		Assert.notNull(mappingContext, "Mapping context is required");

		this.mappingContext = mappingContext;
	}

	public NestedRelationshipProcessingStateMachine(final Neo4jMappingContext mappingContext, Object initialObject, String elementId) {
		this(mappingContext);

		Assert.notNull(initialObject, "Initial object must not be null");
		Assert.notNull(elementId, "The initial objects element ID must not be null");

		storeHashedVersionInProcessedObjectsIds(initialObject, elementId);
	}

	/**
	 * @param relationshipDescription Check whether this relationship description has been processed
	 * @param valuesToStore Check whether all the values in the collection have been processed
	 * @return The state of things processed
	 */
	public ProcessState getStateOf(Object fromId, RelationshipDescription relationshipDescription, @Nullable Collection<?> valuesToStore) {

		final long stamp = lock.readLock();
		try {
			boolean hasProcessedRelationship = hasProcessedRelationship(fromId, relationshipDescription);
			boolean hasProcessedAllValues = hasProcessedAllOf(valuesToStore);
			if (hasProcessedRelationship && hasProcessedAllValues) {
				return ProcessState.PROCESSED_BOTH;
			}
			if (hasProcessedRelationship) {
				return ProcessState.PROCESSED_ALL_RELATIONSHIPS;
			}
			if (hasProcessedAllValues) {
				return ProcessState.PROCESSED_ALL_VALUES;
			}
			return ProcessState.PROCESSED_NONE;
		} finally {
			lock.unlock(stamp);
		}
	}

	/**
	 * Combination of relationship description and fromId to differentiate between `equals`-wise equal relationship
	 * descriptions by their source identifier. This is needed because sometimes the very same relationship definition
	 * can get processed for different objects of the same entity.
	 * One could say that this is a Tuple but it has a nicer name.
	 */
	private static class RelationshipDescriptionWithSourceId {
		private final Object id;
		private final RelationshipDescription relationshipDescription;

		RelationshipDescriptionWithSourceId(Object id, RelationshipDescription relationshipDescription) {
			this.id = id;
			this.relationshipDescription = relationshipDescription;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			RelationshipDescriptionWithSourceId that = (RelationshipDescriptionWithSourceId) o;
			return id.equals(that.id) && relationshipDescription.equals(that.relationshipDescription);
		}

		@Override
		public int hashCode() {
			return Objects.hash(id, relationshipDescription);
		}
	}

	/**
	 * Marks the passed objects as processed
	 *
	 * @param relationshipDescription To be marked as processed
	 */
	public void markRelationshipAsProcessed(Object fromId, @Nullable RelationshipDescription relationshipDescription) {
		if (relationshipDescription == null) {
			return;
		}

		final long stamp = lock.writeLock();
		try {
			this.processedRelationshipDescriptions.add(new RelationshipDescriptionWithSourceId(fromId, relationshipDescription));
		} finally {
			lock.unlock(stamp);
		}
	}

	/**
	 * Marks the passed objects as processed
	 *
	 * @param valueToStore If not {@literal null}, all non-null values will be marked as processed
	 * @param elementId The internal id of the value processed
	 */
	public void markValueAsProcessed(Object valueToStore, String elementId) {

		final long stamp = lock.writeLock();
		try {
			doMarkValueAsProcessed(valueToStore, elementId);
			storeProcessedInAlias(valueToStore, valueToStore);
		} finally {
			lock.unlock(stamp);
		}
	}

	private void doMarkValueAsProcessed(Object valueToStore, String elementId) {

		Object value = extractRelatedValueFromRelationshipProperties(valueToStore);
		storeHashedVersionInProcessedObjectsIds(valueToStore, elementId);
		storeHashedVersionInProcessedObjectsIds(value, elementId);
	}

	/**
	 * Checks if the value has already been processed.
	 *
	 * @param value the object that should be looked for in the registry.
	 * @return processed yes (true) / no (false)
	 */
	public boolean hasProcessedValue(Object value) {

		long stamp = lock.readLock();
		try {
			Object valueToCheck = extractRelatedValueFromRelationshipProperties(value);
			boolean processed = hasProcessed(valueToCheck);
			// This can be the case the object has been loaded via an additional findXXX call
			// We can enforce sets and so on, but this is more user-friendly
			Class<?> typeOfValue = valueToCheck.getClass();
			if (!processed && mappingContext.hasPersistentEntityFor(typeOfValue)) {
				Neo4jPersistentEntity<?> entity = mappingContext.getRequiredPersistentEntity(typeOfValue);
				Neo4jPersistentProperty idProperty = entity.getIdProperty();
				Object id = idProperty == null ? null : entity.getPropertyAccessor(valueToCheck).getProperty(idProperty);
				// After the lookup by system.identityHashCode failed for a processed object alias,
				// we must traverse or iterate over all value with the matching type and compare the domain ids
				// to figure out if the logical object has already been processed through a different object instance.
				// The type check is needed to avoid relationship ids <> node id conflicts.
				Optional<Object> alreadyProcessedObject = id == null ? Optional.empty() : processedObjectsAlias.values().stream()
						.filter(typeOfValue::isInstance)
						.filter(processedObject -> id.equals(entity.getPropertyAccessor(processedObject).getProperty(idProperty)))
						.findAny();
				if (alreadyProcessedObject.isPresent()) { // Skip the show the next time around.
					processed = true;
					String internalId = getInternalId(alreadyProcessedObject.get());
					if (internalId != null) {
						stamp = lock.tryConvertToWriteLock(stamp);
						doMarkValueAsProcessed(valueToCheck, internalId);
					}
				}
			}
			return processed;
		} finally {
			lock.unlock(stamp);
		}
	}

	/**
	 * Checks if the relationship has already been processed.
	 *
	 * @param relationshipDescription the relationship that should be looked for in the registry.
	 * @return processed yes (true) / no (false)
	 */
	public boolean hasProcessedRelationship(Object fromId, @Nullable RelationshipDescription relationshipDescription) {
		if (relationshipDescription != null) {
			final long stamp = lock.readLock();
			try {
				return processedRelationshipDescriptions.contains(new RelationshipDescriptionWithSourceId(fromId, relationshipDescription));
			} finally {
				lock.unlock(stamp);
			}
		}
		return false;
	}

	public void markValueAsProcessedAs(Object valueToStore, Object bean) {
		final long stamp = lock.writeLock();
		try {
			storeProcessedInAlias(valueToStore, bean);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Nullable
	public String getInternalId(Object object) {
		final long stamp = lock.readLock();
		try {
			Object valueToCheck = extractRelatedValueFromRelationshipProperties(object);
			String possibleId = getProcessedObjectIds(valueToCheck);
			return possibleId != null ? possibleId : getProcessedObjectIds(getProcessedAs(valueToCheck));
		} finally {
			lock.unlock(stamp);
		}

	}

	public Object getProcessedAs(Object entity) {

		final long stamp = lock.readLock();
		try {
			return getProcessedAsWithDefaults(entity);
		} finally {
			lock.unlock(stamp);
		}
	}

	@Nullable
	private String getProcessedObjectIds(@Nullable Object entity) {
		if (entity == null) {
			return null;
		}
		return processedObjectsIds.get(System.identityHashCode(entity));
	}

	@NonNull
	private Object extractRelatedValueFromRelationshipProperties(Object valueToStore) {
		Object value;
		if (valueToStore instanceof MappingSupport.RelationshipPropertiesWithEntityHolder) {
			value = ((MappingSupport.RelationshipPropertiesWithEntityHolder) valueToStore).getRelatedEntity();
		} else {
			value = valueToStore;
		}
		return value;
	}

	/*
	 * Convenience wrapper functions to avoid exposing the System.identityHashCode "everywhere" in this class.
	 */
	private void storeHashedVersionInProcessedObjectsIds(Object initialObject, String elementId) {
		processedObjectsIds.put(System.identityHashCode(initialObject), elementId);
	}

	private void storeProcessedInAlias(Object valueToStore, Object bean) {
		processedObjectsAlias.put(System.identityHashCode(valueToStore), bean);
	}

	private Object getProcessedAsWithDefaults(Object entity) {
		return processedObjectsAlias.getOrDefault(System.identityHashCode(entity), entity);
	}

	private boolean hasProcessed(Object valueToCheck) {
		return processedObjectsAlias.containsKey(System.identityHashCode(valueToCheck));
	}

	private boolean hasProcessedAllOf(@Nullable Collection<?> valuesToStore) {
		// there can be null elements in the unified collection of values to store.
		if (valuesToStore == null) {
			return false;
		}
		return processedObjectsIds.keySet().containsAll(valuesToStore.stream().map(System::identityHashCode).collect(Collectors.toList()));
	}
}
