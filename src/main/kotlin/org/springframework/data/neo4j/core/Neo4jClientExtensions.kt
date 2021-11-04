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
package org.springframework.data.neo4j.core

import org.neo4j.driver.Record
import org.neo4j.driver.types.TypeSystem
import java.util.function.BiFunction

/**
 * Extension for [Neo4jClient.RunnableSpec. in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
fun Neo4jClient.UnboundRunnableSpec.inDatabase(targetDatabase: String): Neo4jClient.RunnableSpecBoundToDatabase =
        `in`(targetDatabase)

/**
 * Extension for [Neo4jClient.OngoingDelegation. in] providing an `inDatabase` alias since `in` is a reserved keyword in Kotlin.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
fun <T : Any?> Neo4jClient.OngoingDelegation<T>.inDatabase(targetDatabase: String): Neo4jClient.RunnableDelegation<T> =
        `in`(targetDatabase)

/**
 * A fetch spec that replaces Java's Optional with a nullable.
 * @author Michael J. Simons
 */
class KRecordFetchSpec<T : Any>(private val delegate: Neo4jClient.RecordFetchSpec<T>) {
    fun one(): T? = delegate.one().orElse(null)

    fun first(): T = delegate.first().orElse(null)

    fun all(): Collection<T> = delegate.all()
}

/**
 * A mapping spec that replaces Java's Optional with a nullable.
 * @author Michael J. Simons
 */
class KMappingSpec<T : Any>(private val delegate: Neo4jClient.MappingSpec<T>) {
    fun mappedBy(mappingFunction: BiFunction<TypeSystem, Record, T>): KRecordFetchSpec<T> =
            KRecordFetchSpec(delegate.mappedBy(mappingFunction))

    fun one(): T? = delegate.one().orElse(null)

    fun first(): T = delegate.first().orElse(null)

    fun all(): Collection<T> = delegate.all()
}

/**
 * Extension for [Neo4jClient.RunnableSpec.fetchAs] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.0
 */
inline fun <reified T : Any> Neo4jClient.RunnableSpec.fetchAs(): KMappingSpec<T> =
        KMappingSpec(fetchAs(T::class.java))

/**
 * Extension for [Neo4jClient.RunnableSpec.mappedBy] leveraging reified type parameters and removing
 * the need for an explicit `fetchAs`.
 * @author Michael J. Simons
 * @since 6.0
 */
inline fun <reified T : Any> Neo4jClient.RunnableSpec.mappedBy(
        noinline mappingFunction: (TypeSystem, Record) -> T
) =
        KRecordFetchSpec(fetchAs(T::class.java).mappedBy(mappingFunction))
