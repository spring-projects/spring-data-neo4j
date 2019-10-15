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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Michael J. Simons
 */
class ReactiveNeo4jOperationsExtensionsTest {

    @Nested
    inner class CoroutinesVariantsOfExecutableQuery {

        private val executableQuery = mockk<ReactiveNeo4jOperations.ExecutableQuery<String>>()

        @Test
        fun `fetchAllResults should return a flow of thing`() {

            every { executableQuery.results } returns Flux.just("foo", "bar")

            runBlocking {
                assertThat(executableQuery.fetchAllResults().toList()).contains("foo", "bar")
            }

            verify {
                executableQuery.results
            }
        }

        @Test
        fun `awaitSingleResultOrNull should return value`() {
            every { executableQuery.singleResult } returns Mono.just("baz")

            runBlocking {
                assertThat(executableQuery.awaitSingleResultOrNull()).isEqualTo("baz")
            }
            verify {
                executableQuery.singleResult
            }
        }

        @Test
        fun `awaitFirstOrNull should return null`() {
            every { executableQuery.singleResult } returns Mono.empty()

            runBlocking {
                assertThat(executableQuery.awaitSingleResultOrNull()).isNull()
            }
            verify {
                executableQuery.singleResult
            }
        }
    }
}
