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
package org.neo4j.springframework.data.repository.support;

import reactor.core.publisher.Mono;

import org.neo4j.springframework.data.repository.event.ReactiveBeforeBindCallback;
import org.springframework.data.mapping.callback.ReactiveEntityCallbacks;
import org.springframework.lang.Nullable;

/**
 * Utility class that orchestrates {@link ReactiveEntityCallbacks}.
 * All the methods provided here check for their availability and do nothing when an event cannot be published.
 *
 * @author Michael J. Simons
 * @soundtrack Iron Maiden - Killers
 * @since 1.0
 */
final class ReactiveNeo4jEvents {

	private final @Nullable ReactiveEntityCallbacks entityCallbacks;

	ReactiveNeo4jEvents(@Nullable ReactiveEntityCallbacks entityCallbacks) {
		this.entityCallbacks = entityCallbacks;
	}

	<T> Mono<T> maybeCallBeforeBind(T object) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(ReactiveBeforeBindCallback.class, object);
		}

		return Mono.just(object);
	}
}
