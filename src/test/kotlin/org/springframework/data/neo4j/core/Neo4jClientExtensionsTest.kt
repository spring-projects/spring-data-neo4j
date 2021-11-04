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

import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

/**
 * @author Michael J. Simons
 */
class Neo4jClientExtensionsTest {

    @Test
    fun `RunnableSpec#inDatabase(targetDatabase) extension should call its Java counterpart`() {

        val runnableSpec = mockk<Neo4jClient.UnboundRunnableSpec>(relaxed = true)

        runnableSpec.inDatabase("foobar")

        verify(exactly = 1) { runnableSpec.`in`("foobar") }
    }

    @Test
    fun `OngoingDelegation#inDatabase(targetDatabase) extension should call its Java counterpart`() {

        val ongoingDelegation = mockk<Neo4jClient.OngoingDelegation<Any>>(relaxed = true)

        ongoingDelegation.inDatabase("foobar")

        verify(exactly = 1) { ongoingDelegation.`in`("foobar") }
    }

    @Test
    fun `RunnableSpecTightToDatabase#fetchAs() extension should call its Java counterpart`() {

        val runnableSpec = mockk<Neo4jClient.RunnableSpecBoundToDatabase>(relaxed = true)

        @Suppress("UNUSED_VARIABLE")
        val mappingSpec: KRecordFetchSpec<String> =
                runnableSpec.mappedBy { _, _ -> "Foo" }

        verify(exactly = 1) { runnableSpec.fetchAs(String::class.java) }
    }
}
