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
import java.util.*
import java.util.function.BiFunction

/**
 * Extension for [Neo4jClient.RunnableSpec.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun Neo4jClient.RunnableSpec.inDatabase(targetDatabase: String): Neo4jClient.RunnableSpecTightToDatabase =
		`in`(targetDatabase)

/**
 * Extension for [Neo4jClient.OngoingDelegation.in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
fun <T : Any?> Neo4jClient.OngoingDelegation<T>.inDatabase(targetDatabase: String): Neo4jClient.RunnableDelegation<T> =
		`in`(targetDatabase)

/**
 * An implementation of a fetch spec that replaces Java's Optional with a nullable.
 * @author Michael J. Simons
 */
class DelegatingFetchSpec<T : Any>(private val delegate: Neo4jClient.RecordFetchSpec<Optional<T>, Collection<T>, T>) : Neo4jClient.RecordFetchSpec<T?, Collection<T>, T> {
	override fun one(): T? = delegate.one().orElse(null)

	override fun first(): T = delegate.first().orElse(null)

	override fun all(): Collection<T> = delegate.all()
}

/**
 * An implementation of a mapping spec that replaces Java's Optional with a nullable.
 * @author Michael J. Simons
 */
class DelegatingMappingSpec<T : Any>(private val delegate: Neo4jClient.MappingSpec<Optional<T>, Collection<T>, T>) : Neo4jClient.MappingSpec<T?, Collection<T>, T> {
	override fun mappedBy(mappingFunction: BiFunction<TypeSystem, Record, T>): Neo4jClient.RecordFetchSpec<T?, Collection<T>, T> =
		DelegatingFetchSpec(delegate.mappedBy(mappingFunction))

	override fun one(): T? = delegate.one().orElse(null)

	override fun first(): T = delegate.first().orElse(null)

	override fun all(): Collection<T> = delegate.all()
}

/**
 * Extension for [Neo4jClient.RunnableSpecTightToDatabase.fetchAs] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> Neo4jClient.RunnableSpecTightToDatabase.fetchAs(): Neo4jClient.MappingSpec<T?, Collection<T>, T>
		= DelegatingMappingSpec(fetchAs(T::class.java))

/**
 * Extension for [Neo4jClient.RunnableSpecTightToDatabase.mappedBy] leveraging reified type parameters and removing
 * the need for an explicit `fetchAs`.
 * @author Michael J. Simons
 * @since 1.0
 */
inline fun <reified T : Any> Neo4jClient.RunnableSpecTightToDatabase.mappedBy(noinline mappingFunction: (TypeSystem, Record) -> T): Neo4jClient.RecordFetchSpec<T?, Collection<T>, T>
		= DelegatingFetchSpec(fetchAs(T::class.java).mappedBy(mappingFunction))
