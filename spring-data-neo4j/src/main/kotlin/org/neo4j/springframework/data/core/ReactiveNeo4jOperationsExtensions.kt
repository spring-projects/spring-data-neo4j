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
package org.neo4j.springframework.data.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

/**
 * Coroutines [Flow] variant of [ReactiveNeo4jOperations.ExecutableQuery.getResults].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.ExecutableQuery<T>.fetchAllResults(): Flow<T> =
    results.asFlow()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jOperations.ExecutableQuery.getSingleResult].
 *
 * @author Michael J. Simons
 * @since 1.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jOperations.ExecutableQuery<T>.awaitSingleResultOrNull(): T? =
    singleResult.awaitFirstOrNull()
