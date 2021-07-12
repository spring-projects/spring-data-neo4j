/*
 * Copyright 2011-2021 the original author or authors.
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
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apiguardian.api.API;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

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

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	/**
	 * The set of already processed relationships.
	 */
	private final Set<RelationshipDescriptionWithSourceId> processedRelationshipDescriptions = new HashSet<>();

	/**
	 * The set of already processed related objects.
	 */
	private final Set<Object> processedObjects = new HashSet<>();

	/**
	 * A map of processed objects pointing towards a possible new instance of themself.
	 * This will happen for immutable entities.
	 */
	private final Map<Object, Object> processedObjectsAlias = new HashMap<>();

	/**
	 * A map pointing from a processed object to the internal id.
	 * This will be useful during the persistence to avoid another DB network round-trip.
	 */
	private final Map<Object, Long> processedObjectsIds = new HashMap<>();

	public NestedRelationshipProcessingStateMachine(Object initialObject, Long internalId) {
		this(initialObject);
		processedObjectsIds.put(initialObject, internalId);
	}

	public NestedRelationshipProcessingStateMachine(Object initialObject) {
		processedObjects.add(initialObject);
	}

	/**
	 * @param relationshipDescription Check whether this relationship description has been processed
	 * @param valuesToStore Check whether all the values in the collection have been processed
	 * @return The state of things processed
	 */
	public ProcessState getStateOf(Object fromId, RelationshipDescription relationshipDescription, @Nullable Collection<?> valuesToStore) {

		try {
			read.lock();
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
			read.unlock();
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

		try {
			write.lock();
			this.processedRelationshipDescriptions.add(new RelationshipDescriptionWithSourceId(fromId, relationshipDescription));
		} finally {
			write.unlock();
		}
	}
	/**
	 * Marks the passed objects as processed
	 *
	 * @param valueToStore If not {@literal null}, all non-null values will be marked as processed
	 */
	public void markValueAsProcessed(Object valueToStore, Long internalId) {

		try {
			write.lock();
			Object value = extractRelatedValueFromRelationshipProperties(valueToStore);
			this.processedObjects.add(valueToStore);
			this.processedObjects.add(value);
			if (internalId != null) {
				this.processedObjectsIds.put(valueToStore, internalId);
				this.processedObjectsIds.put(value, internalId);
			}
		} finally {
			write.unlock();
		}
	}

	/**
	 * Checks if the value has already been processed.
	 *
	 * @param value the object that should be looked for in the registry.
 	 * @return processed yes (true) / no (false)
	 */
	public boolean hasProcessedValue(Object value) {
		try {
			read.lock();
			Object valueToCheck = extractRelatedValueFromRelationshipProperties(value);
			return processedObjects.contains(valueToCheck) || processedObjectsAlias.containsKey(valueToCheck);
		} finally {
			read.unlock();
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
			return processedRelationshipDescriptions.contains(new RelationshipDescriptionWithSourceId(fromId, relationshipDescription));
		}
		return false;
	}

	public void markValueAsProcessedAs(Object relatedValueToStore, Object bean) {
		try {
			write.lock();
			processedObjectsAlias.put(relatedValueToStore, bean);
		} finally {
			write.unlock();
		}
	}

	@Nullable
	public Long getInternalId(Object object) {
		try {
			read.lock();
			Object valueToCheck = extractRelatedValueFromRelationshipProperties(object);
			Long possibleId = processedObjectsIds.get(valueToCheck);
			return possibleId != null ? possibleId : processedObjectsIds.get(processedObjectsAlias.get(valueToCheck));
		} finally {
			read.unlock();
		}

	}

	public Object getProcessedAs(Object entity) {
		try {
			read.lock();
			return processedObjectsAlias.getOrDefault(entity, entity);
		} finally {
			read.unlock();
		}
	}

	private boolean hasProcessedAllOf(@Nullable Collection<?> valuesToStore) {
		// there can be null elements in the unified collection of values to store.
		if (valuesToStore == null) {
			return false;
		}
		return processedObjects.containsAll(valuesToStore);
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
}
