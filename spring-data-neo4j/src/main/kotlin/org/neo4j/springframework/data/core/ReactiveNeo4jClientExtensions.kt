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

import org.neo4j.driver.Record
import org.neo4j.driver.types.TypeSystem
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * Extension for [ReactiveNeo4jClient.ReactiveRunnableSpec.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun ReactiveNeo4jClient.ReactiveRunnableSpec.inDatabase(targetDatabase: String): ReactiveNeo4jClient.ReactiveRunnableSpecTightToDatabase =
		`in`(targetDatabase)

/**
 * Extension for [ReactiveNeo4jClient.OngoingReactiveDelegation.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun <T : Any?> ReactiveNeo4jClient.OngoingReactiveDelegation<T>.inDatabase(targetDatabase: String): ReactiveNeo4jClient.ReactiveRunnableDelegation<T>
		= `in`(targetDatabase)

/**
 * Extension for [ReactiveNeo4jClient.RunnableSpecTightToDatabase.fetchAs] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> ReactiveNeo4jClient.ReactiveRunnableSpecTightToDatabase.fetchAs(): Neo4jClient.MappingSpec<Mono<T>, Flux<T>, T>
		= fetchAs(T::class.java)

/**
 * Extension for [ReactiveNeo4jClient.RunnableSpecTightToDatabase.mappedBy] leveraging reified type parameters and removing
 * the need for an explicit `fetchAs`.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> ReactiveNeo4jClient.ReactiveRunnableSpecTightToDatabase.mappedBy(noinline mappingFunction: (TypeSystem, Record) -> T): Neo4jClient.RecordFetchSpec<Mono<T>, Flux<T>, T>
		= fetchAs(T::class.java).mappedBy(mappingFunction)
