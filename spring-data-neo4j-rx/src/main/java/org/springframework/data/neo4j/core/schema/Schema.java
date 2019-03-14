/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.schema;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apiguardian.api.API;

/**
 * Contains the descriptions of all nodes, their properties and relationships known to SDN-RX.
 *
 * The schema is currently designed to be mutual.
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Schema {

	private final Map<String, NodeDescription> nodeDescriptionsByPrimaryLabel = new HashMap<>();

	// Just added as a reminder. Add adaquate locks
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	/**
	 * Registers a node description under it's primary label.
	 *
	 * @param newDescription The new node description.
	 * @return This schema.
	 * @throws IllegalSchemaChangeException when the description is already registered (under any label)
	 */
	public Schema registerNodeDescription(NodeDescription newDescription) {

		String primaryLabel = newDescription.getPrimaryLabel();
		if (this.nodeDescriptionsByPrimaryLabel.containsKey(primaryLabel)) {
			throw new IllegalSchemaChangeException(String
				.format(Locale.ENGLISH, "The schema already contains a node description under the primary label %s",
					primaryLabel));
		}

		if (this.nodeDescriptionsByPrimaryLabel.containsValue(newDescription)) {
			Optional<String> label = this.nodeDescriptionsByPrimaryLabel.entrySet().stream()
				.filter(e -> e.getValue().equals(newDescription)).map(
					Map.Entry::getKey).findFirst();

			throw new IllegalSchemaChangeException(String
				.format(Locale.ENGLISH, "The schema already contains description %s under the primary label %s",
					newDescription, label.orElse("n/a")));
		}

		this.nodeDescriptionsByPrimaryLabel.put(primaryLabel, newDescription);
		return this;
	}

	/**
	 * Retrieves a nodes description by its primary label.
	 *
	 * @param primaryLabel The primary label under which the node is described
	 * @return The description if any
	 */
	public Optional<NodeDescription> getNodeDescription(String primaryLabel) {
		return Optional.ofNullable(this.nodeDescriptionsByPrimaryLabel.get(primaryLabel));
	}
}
