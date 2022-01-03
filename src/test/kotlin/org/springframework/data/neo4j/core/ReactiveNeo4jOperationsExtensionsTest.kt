/*
 * Copyright 2011-2022 the original author or authors.
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.neo4j.cypherdsl.core.Statement
import org.springframework.data.neo4j.integration.shared.common.KotlinPerson
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

/**
 * @author Michael J. Simons
 */
class ReactiveNeo4jOperationsExtensionsTest {

	private val template = mockk<ReactiveNeo4jOperations>()
	private val statement = mockk<Statement>()

	@Test
	fun `count extension should call its Java counterpart`() {

		every { template.count(KotlinPerson::class.java) } returns Mono.just(1L)

		runBlocking {
			assertThat(template.count<KotlinPerson>()).isEqualTo(1L)
		}

		verify { template.count(KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (1)`() {

		every { template.findAll(KotlinPerson::class.java) } returns Flux.empty()

		runBlocking {
			assertThat(template.findAll<KotlinPerson>().toList()).isEmpty()
		}

		verify { template.findAll(KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (2)`() {

		every { template.findAll(statement, KotlinPerson::class.java) } returns Flux.empty()

		runBlocking {
			assertThat(template.findAll<KotlinPerson>(statement).toList()).isEmpty()
		}

		verify { template.findAll(statement, KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (3)`() {

		every { template.findAll(statement, mapOf(), KotlinPerson::class.java) } returns Flux.empty()

		runBlocking {
			assertThat(template.findAll<KotlinPerson>(statement, mapOf()).toList()).isEmpty()
		}

		verify { template.findAll(statement, mapOf(), KotlinPerson::class.java) }
	}

	@Test
	fun `findOne extension should call its Java counterpart`() {

		every { template.findOne(statement, mapOf(), KotlinPerson::class.java) } returns Mono.empty()

		runBlocking {
			assertThat(template.findOne<KotlinPerson>(statement, mapOf())).isNull()
		}

		verify { template.findOne(statement, mapOf(), KotlinPerson::class.java) }
	}

	@Test
	fun `findById extension should call its Java counterpart`() {

		every { template.findById(1L, KotlinPerson::class.java) } returns Mono.empty()

		runBlocking {
			assertThat(template.findById<KotlinPerson>(1L)).isNull()
		}

		verify { template.findById(1L, KotlinPerson::class.java) }
	}

	@Test
	fun `findAllById extension should call its Java counterpart`() {

		every { template.findAllById(listOf(1L, 2L), KotlinPerson::class.java) } returns Flux.empty()

		runBlocking {
			assertThat(template.findAllById<KotlinPerson>(listOf(1L, 2L)).toList()).isEmpty()
		}

		verify { template.findAllById(listOf(1L, 2L), KotlinPerson::class.java) }
	}

	@Test
	fun `deleteById extension should call its Java counterpart`() {

		every { template.deleteById(1L, KotlinPerson::class.java) } returns Mono.empty()

		runBlocking {
			assertThat(template.deleteById<KotlinPerson>(1L).awaitSingleOrNull()).isNull()
		}

		verify { template.deleteById(1L, KotlinPerson::class.java) }
	}

	@Test
	fun `deleteAllById extension should call its Java counterpart`() {

		every { template.deleteAllById(listOf(1L, 2L), KotlinPerson::class.java) } returns Mono.empty()

		runBlocking {
			assertThat(template.deleteAllById<KotlinPerson>(listOf(1L, 2L)).awaitSingleOrNull()).isNull()
		}

		verify { template.deleteAllById(listOf(1L, 2L), KotlinPerson::class.java) }
	}

	@Test
	fun `deleteAll extension should call its Java counterpart`() {

		every { template.deleteAll(KotlinPerson::class.java) } returns Mono.empty()

		runBlocking {
			template.deleteAll<KotlinPerson>()
		}

		verify { template.deleteAll(KotlinPerson::class.java) }
	}

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
