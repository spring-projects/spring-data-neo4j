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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.neo4j.driver.summary.ResultSummary
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Michael J. Simons
 */
class ReactiveNeo4jClientExtensionsTest {

    @Test
    fun `RunnableSpec#inDatabase(targetDatabase) extension should call its Java counterpart`() {

        val runnableSpec = mockk<ReactiveNeo4jClient.RunnableSpec>(relaxed = true)

        runnableSpec.inDatabase("foobar")

        verify(exactly = 1) { runnableSpec.`in`("foobar") }
    }

    @Test
    fun `OngoingDelegation#inDatabase(targetDatabase) extension should call its Java counterpart`() {

        val ongoingDelegation = mockk<ReactiveNeo4jClient.OngoingDelegation<Any>>(relaxed = true)

        ongoingDelegation.inDatabase("foobar")

        verify(exactly = 1) { ongoingDelegation.`in`("foobar") }
    }

    @Test
    fun `ReactiveRunnableDelegation#fetchAs() extension should call its Java counterpart`() {

        val runnableSpec = mockk<ReactiveNeo4jClient.RunnableSpecTightToDatabase>(relaxed = true)

        val mappingSpec: ReactiveNeo4jClient.MappingSpec<String> = runnableSpec.fetchAs()

        verify(exactly = 1) { runnableSpec.fetchAs(String::class.java) }
    }

    @Test
    fun runnableSpecShouldReturnSuspendedResultSummary() {

        val runnableSpec = mockk<ReactiveNeo4jClient.RunnableSpecTightToDatabase>()
        val resultSummary = mockk<ResultSummary>()
        every { runnableSpec.run() } returns Mono.just(resultSummary)

        runBlocking {
            assertThat(runnableSpec.await()).isEqualTo(resultSummary)
        }

        verify {
            runnableSpec.run()
        }
    }

    @Nested
    inner class CoroutinesVariantsOfRunnableDelegation {

        private val runnableDelegation = mockk<ReactiveNeo4jClient.RunnableDelegation<String>>()

        @Test
        fun `awaitFirstOrNull should return value`() {

            every { runnableDelegation.run() } returns Mono.just("bazbar")

            runBlocking {
                assertThat(runnableDelegation.awaitFirstOrNull()).isEqualTo("bazbar")
            }

            verify {
                runnableDelegation.run()
            }
        }

        @Test
        fun `awaitFirstOrNull should return null`() {

            every { runnableDelegation.run() } returns Mono.empty()

            runBlocking {
                assertThat(runnableDelegation.awaitFirstOrNull()).isNull()
            }

            verify {
                runnableDelegation.run()
            }
        }
    }

    @Nested
    inner class CoroutinesVariantsOfRecordFetchSpec {

        private val recordFetchSpec = mockk<ReactiveNeo4jClient.RecordFetchSpec<String>>()

        @Test
        fun `awaitOne should return value`() {
            every { recordFetchSpec.one() } returns Mono.just("foo")

            runBlocking {
                assertThat(recordFetchSpec.awaitOneOrNull()).isEqualTo("foo")
            }
            verify {
                recordFetchSpec.one()
            }
        }

        @Test
        fun `awaitOne should return null`() {
            every { recordFetchSpec.one() } returns Mono.empty()

            runBlocking {
                assertThat(recordFetchSpec.awaitOneOrNull()).isNull()
            }
            verify {
                recordFetchSpec.one()
            }
        }

        @Test
        fun `awaitFirstOrNull should return value`() {
            every { recordFetchSpec.first() } returns Mono.just("bar")

            runBlocking {
                assertThat(recordFetchSpec.awaitFirstOrNull()).isEqualTo("bar")
            }
            verify {
                recordFetchSpec.first()
            }
        }

        @Test
        fun `awaitFirstOrNull should return null`() {
            every { recordFetchSpec.first() } returns Mono.empty()

            runBlocking {
                assertThat(recordFetchSpec.awaitFirstOrNull()).isNull()
            }
            verify {
                recordFetchSpec.first()
            }
        }

        @Test
        fun `fetchAll should return a flow of thing`() {

            every { recordFetchSpec.all() } returns Flux.just("foo", "bar")

            runBlocking {
                assertThat(recordFetchSpec.fetchAll().toList()).contains("foo", "bar")
            }

            verify {
                recordFetchSpec.all()
            }
        }
    }
}
