/*
 * Copyright 2011-2023 the original author or authors.
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
import org.neo4j.cypherdsl.core.Statement
import org.springframework.data.neo4j.integration.shared.common.KotlinPerson

/**
 * @author Michael J. Simons
 */
class Neo4jOperationsExtensionsTest {

	private val template = mockk<Neo4jOperations>(relaxed = true)
	private val statement = mockk<Statement>()

	@Test
	fun `count extension should call its Java counterpart`() {

		template.count<KotlinPerson>()

		verify { template.count(KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (1)`() {

		template.findAll<KotlinPerson>()

		verify { template.findAll(KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (2)`() {

		template.findAll<KotlinPerson>(statement)

		verify { template.findAll(statement, KotlinPerson::class.java) }
	}

	@Test
	fun `findAll extension should call its Java counterpart (3)`() {

		template.findAll<KotlinPerson>(statement, mapOf())

		verify { template.findAll(statement, mapOf(), KotlinPerson::class.java) }
	}

	@Test
	fun `findOne extension should call its Java counterpart`() {

		template.findOne<KotlinPerson>(statement, mapOf())

		verify { template.findOne(statement, mapOf(), KotlinPerson::class.java) }
	}

	@Test
	fun `findById extension should call its Java counterpart`() {

		template.findById<KotlinPerson>(1L)

		verify { template.findById(1L, KotlinPerson::class.java) }
	}

	@Test
	fun `findAllById extension should call its Java counterpart`() {

		template.findAllById<KotlinPerson>(listOf(1L, 2L))

		verify { template.findAllById(listOf(1L, 2L), KotlinPerson::class.java) }
	}

	@Test
	fun `deleteById extension should call its Java counterpart`() {

		template.deleteById<KotlinPerson>(1L)

		verify { template.deleteById(1L, KotlinPerson::class.java) }
	}

	@Test
	fun `deleteAllById extension should call its Java counterpart`() {

		template.deleteAllById<KotlinPerson>(listOf(1L, 2L))

		verify { template.deleteAllById(listOf(1L, 2L), KotlinPerson::class.java) }
	}

	@Test
	fun `deleteAll extension should call its Java counterpart`() {

		template.deleteAll<KotlinPerson>()

		verify { template.deleteAll(KotlinPerson::class.java) }
	}
}
