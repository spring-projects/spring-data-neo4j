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

package org.springframework.data.neo4j.integration.imperative

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.config.AbstractNeo4jConfig
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.neo4j.core.findById
import org.springframework.data.neo4j.integration.shared.common.ConcreteDataNodeWithAbstractKotlinBase
import org.springframework.data.neo4j.integration.shared.common.ConcreteDataNodeWithOpenKotlinBase
import org.springframework.data.neo4j.integration.shared.common.ConcreteNodeWithAbstractKotlinBase
import org.springframework.data.neo4j.integration.shared.common.ConcreteNodeWithOpenKotlinBase
import org.springframework.data.neo4j.test.Neo4jExtension
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * @author Michael J. Simons
 * @soundtrack Genesis - Invisible Touch
 */
@Neo4jIntegrationTest
class KotlinInheritanceIT @Autowired constructor(
		private val template: Neo4jTemplate,
		private val driver: Driver
) {

	companion object {
		@JvmStatic
		private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport

		@BeforeAll
		@JvmStatic
		fun clearDatabase(@Autowired driver: Driver) {
			driver.session().use { session ->
				session.run("MATCH (n) DETACH DELETE n")
			}
		}
	}

	@Test // GH-1903
	fun mappingWithAbstractBaseClassShouldWork() {

		var existingId: Long?
		driver.session().use { session ->
			session.beginTransaction().use { tx ->
				existingId = tx.run("CREATE (t:AbstractKotlinBase:ConcreteNodeWithAbstractKotlinBase {name: 'Foo', anotherProperty: 'Bar'}) RETURN id(t) AS id")
						.single()["id"].asLong()
				tx.commit()
			}
		}

		val existingThing = template.findById<ConcreteNodeWithAbstractKotlinBase>(existingId!!)!!

		assertThat(existingThing.name).isEqualTo("Foo")
		assertThat(existingThing.anotherProperty).isEqualTo("Bar")

		val thing = template.save(ConcreteNodeWithAbstractKotlinBase("onBase", "onDependent"));
		assertThat(thing.id).isNotNull()
		assertThat(thing.name).isEqualTo("onBase")
		assertThat(thing.anotherProperty).isEqualTo("onDependent")

		val cnt = driver.session().use { session ->
			session.readTransaction { tx ->
				tx.run("MATCH (t:AbstractKotlinBase:ConcreteNodeWithAbstractKotlinBase) WHERE id(t) = \$id RETURN count(t)", mapOf("id" to thing.id)).single()[0].asLong()
			}
		}
		assertThat(cnt).isEqualTo(1L)
	}

	@Test // GH-1903
	fun mappingWithDataExtendingAbstractBaseClassShouldWork() {

		var existingId: Long?
		driver.session().use { session ->
			session.beginTransaction().use { tx ->
				existingId = tx.run("CREATE (t:AbstractKotlinBase:ConcreteDataNodeWithAbstractKotlinBase {name: 'Foo', anotherProperty: 'Bar'}) RETURN id(t) AS id")
						.single()["id"].asLong()
				tx.commit()
			}
		}

		val existingThing = template.findById<ConcreteDataNodeWithAbstractKotlinBase>(existingId!!)!!

		assertThat(existingThing.name).isEqualTo("Foo")
		assertThat(existingThing.anotherProperty).isEqualTo("Bar")

		val thing = template.save(ConcreteDataNodeWithAbstractKotlinBase("onBase", "onDependent"));
		assertThat(thing.id).isNotNull()
		assertThat(thing.name).isEqualTo("onBase")
		assertThat(thing.anotherProperty).isEqualTo("onDependent")

		val cnt = driver.session().use { session ->
			session.readTransaction { tx ->
				tx.run("MATCH (t:AbstractKotlinBase:ConcreteDataNodeWithAbstractKotlinBase) WHERE id(t) = \$id RETURN count(t)", mapOf("id" to thing.id)).single()[0].asLong()
			}
		}
		assertThat(cnt).isEqualTo(1L)
	}

	@Test // GH-1903
	fun mappingWithOpenBaseClassShouldWork() {

		var existingId: Long =
				driver.session().use { session ->
					session.writeTransaction { tx ->
						tx.run("CREATE (t:OpenKotlinBase:ConcreteNodeWithOpenKotlinBase {name: 'Foo', anotherProperty: 'Bar'}) RETURN id(t) AS id")
								.single()["id"].asLong()
					}
				}

		val existingThing = template.findById<ConcreteNodeWithOpenKotlinBase>(existingId!!)!!

		assertThat(existingThing.name).isEqualTo("Foo")
		assertThat(existingThing.anotherProperty).isEqualTo("Bar")

		val thing = template.save(ConcreteNodeWithOpenKotlinBase("onBase", "onDependent"));
		assertThat(thing.id).isNotNull()
		assertThat(thing.name).isEqualTo("onBase")
		assertThat(thing.anotherProperty).isEqualTo("onDependent")

		// Note: The open base class used here is not abstract, there fore labels are not inherited
		val cnt = driver.session().use { session ->
			session.readTransaction { tx ->
				tx.run("MATCH (t:ConcreteNodeWithOpenKotlinBase) WHERE NOT t:OpenKotlinBase AND id(t) = \$id RETURN count(t)", mapOf("id" to thing.id)).single()[0].asLong()
			}
		}
		assertThat(cnt).isEqualTo(1L)
	}

	@Test // GH-1903
	fun mappingWithDataExtendingOpenBaseClassShouldWork() {

		var existingId: Long =
				driver.session().use { session ->
					session.writeTransaction { tx ->
						tx.run("CREATE (t:OpenKotlinBase:ConcreteDataNodeWithOpenKotlinBase {name: 'Foo', anotherProperty: 'Bar'}) RETURN id(t) AS id")
								.single()["id"].asLong()
					}
				}

		val existingThing = template.findById<ConcreteDataNodeWithOpenKotlinBase>(existingId!!)!!

		assertThat(existingThing.name).isEqualTo("Foo")
		assertThat(existingThing.anotherProperty).isEqualTo("Bar")

		val thing = template.save(ConcreteDataNodeWithOpenKotlinBase("onBase", "onDependent"));
		assertThat(thing.id).isNotNull()
		assertThat(thing.name).isEqualTo("onBase")
		assertThat(thing.anotherProperty).isEqualTo("onDependent")

		// Note: The open base class used here is not abstract, there fore labels are not inherited
		val cnt = driver.session().use { session ->
			session.readTransaction { tx ->
				tx.run("MATCH (t:ConcreteDataNodeWithOpenKotlinBase) WHERE NOT t:OpenKotlinBase AND id(t) = \$id RETURN count(t)", mapOf("id" to thing.id)).single()[0].asLong()
			}
		}
		assertThat(cnt).isEqualTo(1L)
	}

	@Configuration
	@EnableTransactionManagement
	open class MyConfig : AbstractNeo4jConfig() {
		@Bean
		override fun driver(): Driver {
			return neo4jConnectionSupport.driver
		}
	}
}
