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
import org.springframework.data.neo4j.integration.shared.common.Inheritance
import org.springframework.data.neo4j.integration.shared.common.KotlinAnimationMovie
import org.springframework.data.neo4j.integration.shared.common.KotlinCinema
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.test.Neo4jExtension
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate

/**
 * @author Michael J. Simons
 * @soundtrack Genesis - Invisible Touch
 */
@Neo4jIntegrationTest
class KotlinInheritanceIT @Autowired constructor(
		private val template: Neo4jTemplate,
		private val driver: Driver,
		private val transactionTemplate: TransactionTemplate
) {

	companion object {
		@JvmStatic
		private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport

		@BeforeAll
		@JvmStatic
		fun clearDatabase(@Autowired driver: Driver) {
			driver.session().use { session ->
				session.run("MATCH (n) DETACH DELETE n").consume()
			}
		}
	}

	@Test // GH-2262
	fun shouldMatchPolymorphicInterfacesWhenFetchingAll(@Autowired cinemaRepository: KotlinCinemaRepository) {

		driver.session().use { session ->
			session.writeTransaction { tx ->
				tx.run("CREATE (:KotlinMovie:KotlinAnimationMovie {id: 'movie001', name: 'movie-001', studio: 'Pixar'})<-[:Plays]-(c:KotlinCinema {id:'cine-01', name: 'GrandRex'}) RETURN id(c) AS id")
						.single()["id"].asLong()
			}
		}

		val cinemas = cinemaRepository.findAll()
		assertThat(cinemas).hasSize(1);
		assertThat(cinemas).first().satisfies { c ->
			assertThat(c.plays).hasSize(1);
			assertThat(c.plays).first().isInstanceOf(KotlinAnimationMovie::class.java)
					.extracting { m -> (m as KotlinAnimationMovie).studio }
					.isEqualTo("Pixar")
		}
	}

	interface KotlinCinemaRepository : Neo4jRepository<KotlinCinema, String>

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	open class MyConfig : AbstractNeo4jConfig() {
		@Bean
		override fun driver(): Driver {
			return neo4jConnectionSupport.driver
		}

		override fun getMappingBasePackages(): Collection<String?>? {
			return setOf(Inheritance::class.java.getPackage().name)
		}

		@Bean
		open fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
			return TransactionTemplate(transactionManager)
		}
	}
}
