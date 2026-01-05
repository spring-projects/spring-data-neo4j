/*
 * Copyright 2011-present the original author or authors.
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

package org.springframework.data.neo4j.integration.k

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.config.AbstractNeo4jConfig
import org.springframework.data.neo4j.core.DatabaseSelectionProvider
import org.springframework.data.neo4j.core.Neo4jTemplate
import org.springframework.data.neo4j.core.schema.Id
import org.springframework.data.neo4j.core.schema.Node
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.test.BookmarkCapture
import org.springframework.data.neo4j.test.Neo4jExtension
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.stereotype.Repository
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.support.TransactionTemplate
import java.util.*

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
internal class KotlinIssuesIT {

	companion object {
		@JvmStatic
		private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport

		@BeforeAll
		@JvmStatic
		fun clearDatabase(@Autowired driver: Driver, @Autowired bookmarkCapture: BookmarkCapture) {
			driver.session().use { session ->
				session.run("MATCH (n) DETACH DELETE n").consume()
				bookmarkCapture.seedWith(session.lastBookmarks())
			}
		}
	}

	@Test // GH-2899
	fun requiredPropertiesMustBeIncludedInProjections(@Autowired someRepository: KotlinDataClassEntityRepository) {
		someRepository.save(KotlinDataClassEntity(propertyOne = "one", propertyTwo = "two"))
		val p = someRepository.findAllProjectedBy()
        assertThat(p).hasSizeGreaterThan(0)
	        .first().matches { v -> v.propertyOne == "one" }
	}

	@Node
	data class KotlinDataClassEntity (

		@Id
		val id: String = UUID.randomUUID().toString(),
		val propertyOne: String,
		val propertyTwo: String
	)

	interface KotlinDataClassEntityProjection {
		val propertyOne: String
	}

    @Repository
    internal interface KotlinDataClassEntityRepository : Neo4jRepository<KotlinDataClassEntity, String> {
        fun findAllProjectedBy(): List<KotlinDataClassEntityProjection>
    }

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	open class MyConfig : AbstractNeo4jConfig() {
		@Bean
		override fun driver(): Driver {
			return neo4jConnectionSupport.driver
		}

		@Bean
		open fun bookmarkCapture(): BookmarkCapture {
			return BookmarkCapture()
		}

		@Bean
		override fun transactionManager(driver: Driver, databaseNameProvider: DatabaseSelectionProvider): PlatformTransactionManager {
			val bookmarkCapture = bookmarkCapture()
			return Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture))
		}

		@Bean
		open fun transactionTemplate(transactionManager: PlatformTransactionManager): TransactionTemplate {
			return TransactionTemplate(transactionManager)
		}
	}
}
