/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.neo4j.springframework.data.core.schema.RelationshipDescription;
import org.springframework.lang.Nullable;

/**
 * This stores all processed nested relations and objects during save of objects so that the recursive descent can be
 * stopped accordingly.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Heart Attack No. 1
 */
final class NestedRelationshipProcessState {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	/**
	 * The set of already processed relationships.
	 */
	private final Set<RelationshipDescription> processedRelationshipDescriptions = new HashSet<>();

	/**
	 * The set of already processed related objects.
	 */
	private final Set<Object> processedObjects = new HashSet<>();

	/**
	 * @param relationshipDescription Check whether this relationship description has been processed
	 * @param valuesToStore           Check whether all the values in the collection have been processed
	 * @return True, if either the relationship has been already process or all of the values to store.
	 */
	boolean hasProcessedEither(RelationshipDescription relationshipDescription, @Nullable Collection<?> valuesToStore) {

		try {
			read.lock();
			return hasProcessed(relationshipDescription) || hasProcessedAllOf(valuesToStore);
		} finally {
			read.unlock();
		}
	}

	/**
	 * Marks the passed objects as processed
	 *
	 * @param relationshipDescription To be marked as processed
	 * @param valuesToStore           If not {@literal null}, all non-null values will be marked as processed
	 */
	void markAsProcessed(RelationshipDescription relationshipDescription, @Nullable Collection<?> valuesToStore) {

		try {
			write.lock();
			this.processedRelationshipDescriptions.add(relationshipDescription);
			if (valuesToStore != null) {
				valuesToStore.stream().filter(v -> v != null).forEach(processedObjects::add);
			}
		} finally {
			write.unlock();
		}
	}

	private boolean hasProcessedAllOf(@Nullable Collection<?> valuesToStore) {
		// there can be null elements in the unified collection of values to store.
		if (valuesToStore == null) {
			return false;
		}
		return processedObjects.containsAll(valuesToStore);
	}

	private boolean hasProcessed(RelationshipDescription relationshipDescription) {

		if (relationshipDescription != null) {
			return processedRelationshipDescriptions.contains(relationshipDescription);
		}
		return false;
	}
}
