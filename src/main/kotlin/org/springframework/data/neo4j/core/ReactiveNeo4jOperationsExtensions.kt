/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.neo4j.cypherdsl.core.Statement
import reactor.core.publisher.Mono

/**
 * Coroutines [Flow] variant of [ReactiveNeo4jOperations.ExecutableQuery.getResults].
 *
 * @author Michael J. Simons
 * @since 6.0
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.ExecutableQuery<T>.fetchAllResults(): Flow<T> =
		results.asFlow()

/**
 * Nullable Coroutines variant of [ReactiveNeo4jOperations.ExecutableQuery.getSingleResult].
 *
 * @author Michael J. Simons
 * @since 6.0
 */
suspend inline fun <reified T : Any> ReactiveNeo4jOperations.ExecutableQuery<T>.awaitSingleResultOrNull(): T? =
		singleResult.awaitSingleOrNull()

/**
 * Extension for [ReactiveNeo4jOperations.count] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
suspend inline fun <reified T : Any> ReactiveNeo4jOperations.count(): Long = count(T::class.java).awaitSingle()

/**
 * Extension for [ReactiveNeo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.findAll(): Flow<T> = findAll(T::class.java).asFlow()

/**
 * Extension for [ReactiveNeo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.findAll(statement: Statement): Flow<T> = findAll(statement, T::class.java).asFlow()

/**
 * Extension for [ReactiveNeo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.findAll(statement: Statement, parameters: Map<String, Any>): Flow<T> = findAll(statement, parameters, T::class.java).asFlow()

/**
 * Extension for [ReactiveNeo4jOperations.findOne] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
suspend inline fun <reified T : Any> ReactiveNeo4jOperations.findOne(statement: Statement, parameters: Map<String, Any>): T? = findOne(statement, parameters, T::class.java).awaitSingleOrNull()

/**
 * Extension for [ReactiveNeo4jOperations.findById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
suspend inline fun <reified T : Any> ReactiveNeo4jOperations.findById(id: Any): T? = findById(id, T::class.java).awaitSingleOrNull()

/**
 * Extension for [ReactiveNeo4jOperations.findAllById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.findAllById(ids: Iterable<Any>): Flow<T> = findAllById(ids, T::class.java).asFlow()

/**
 * Extension for [ReactiveNeo4jOperations.deleteById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.deleteById(id: Any): Mono<Void> = deleteById(id, T::class.java);

/**
 * Extension for [ReactiveNeo4jOperations.deleteAllById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.deleteAllById(ids: Iterable<Any>): Mono<Void> = deleteAllById(ids, T::class.java)

/**
 * Extension for [ReactiveNeo4jOperations.deleteAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> ReactiveNeo4jOperations.deleteAll(): Mono<Void> = deleteAll(T::class.java)
