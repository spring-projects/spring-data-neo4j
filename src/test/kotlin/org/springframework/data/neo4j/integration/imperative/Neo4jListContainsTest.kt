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
package org.springframework.data.neo4j.integration.imperative

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.config.AbstractNeo4jConfig
import org.springframework.data.neo4j.core.DatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.data.neo4j.integration.shared.common.TestNode
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.neo4j.test.BookmarkCapture
import org.springframework.data.neo4j.test.Neo4jExtension
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement

@Neo4jIntegrationTest
class Neo4jListContainsTest {

	companion object {
		@JvmStatic
		private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport

		@BeforeAll
		@JvmStatic
		fun prepareDatabase(@Autowired driver: Driver, @Autowired bookmarkCapture: BookmarkCapture) {
			driver.session().use { session ->
				session.run("MATCH (n) DETACH DELETE n").consume()
				session.run("CREATE (testNode:GH2444{id: 1, items: ['item 1', 'item 2', 'item 3'], description: 'desc 1'})").consume();
				bookmarkCapture.seedWith(session.lastBookmarks())
			}
		}
	}

	@Autowired
	private lateinit var testNodeRepository: TestNodeRepository

	@Test
	fun `Should find test node by items containing`() {
		val testNode = testNodeRepository.findByItemsContains("item 2")
		assertNotNull(testNode)
	}

	@Test
	fun `Should find test node by using explicit query`() {
		val testNode = testNodeRepository.findByItemsContainsWithExplicitQuery("item 2")
		assertNotNull(testNode)
	}

	@Test
	fun `Should find test node by description in`() {
		val testNode = testNodeRepository.findByDescriptionIn(listOf("desc 1", "desc 2", "desc 3"))
		assertNotNull(testNode)
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories
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
	}
}

interface TestNodeRepository : Neo4jRepository<TestNode, Long> {

	fun findByItemsContains(item: String): TestNode?

	@Query("""MATCH (t:GH2444) WHERE ${"$"}item IN t.items RETURN t""")
	fun findByItemsContainsWithExplicitQuery(item: String): TestNode?

	fun findByDescriptionIn(descriptions: List<String>): TestNode?
}