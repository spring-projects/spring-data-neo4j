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
package org.springframework.data.neo4j.core.schema;

import java.util.Optional;

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;

/**
 * Description how to generate Ids for entities.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class IdDescription {

	/**
	 * The selected strategy.
	 */
	private final Id.Strategy idStrategy;

	/**
	 * The class representing a generator for new ids.
	 */
	private @Nullable final Class<? extends IdGenerator> idGeneratorClass;

	/**
	 * The property that stores the id if applicable.
	 */
	private @Nullable final String graphPropertyName;

	public IdDescription() {
		this(Id.Strategy.INTERNAL, null, null);
	}

	public IdDescription(Id.Strategy idStrategy,
		@Nullable Class<? extends IdGenerator> idGeneratorClass, @Nullable String graphPropertyName) {
		this.idStrategy = idStrategy;
		this.idGeneratorClass = idGeneratorClass;
		this.graphPropertyName = graphPropertyName;
	}

	public Id.Strategy getIdStrategy() {
		return idStrategy;
	}

	public Optional<Class<? extends IdGenerator>> getIdGeneratorClass() {
		return Optional.ofNullable(idGeneratorClass);
	}

	public Optional<String> getGraphPropertyName() {
		return Optional.ofNullable(graphPropertyName);
	}
}
