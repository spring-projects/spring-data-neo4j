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
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.data.neo4j.integration.shared.common.DepartmentEntity
import org.springframework.data.neo4j.integration.shared.common.PersonDepartmentQueryResult
import org.springframework.data.neo4j.integration.shared.common.PersonEntity
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.repository.query.Query
import org.springframework.data.neo4j.test.BookmarkCapture
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import java.util.function.Consumer

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
internal class KotlinProjectionIT {

	companion object {
		@JvmStatic
		private lateinit var neo4jConnectionSupport: Neo4jConnectionSupport

		@BeforeAll
		@JvmStatic
		fun clearDatabase(@Autowired driver: Driver, @Autowired bookmarkCapture: BookmarkCapture) {
			driver.session(bookmarkCapture.createSessionConfig()).use { session ->
				session.run("MATCH (n) DETACH DELETE n")
				session.run("""					
					CREATE (p:PersonEntity {id: 'p1', email: 'p1@dep1.org'}) -[:MEMBER_OF]->(department:DepartmentEntity {id: 'd1', name: 'Dep1'})
					RETURN p					
				""".trimIndent()).consume();
				bookmarkCapture.seedWith(session.lastBookmarks())
			}
		}
	}

	@Test // GH-2349
	fun projectionsContainingKnownEntitiesShouldWorkFromRepository(@Autowired personRepository: PersonRepository) {

		val results = personRepository.findPersonWithDepartment()
		assertThat(results)
				.hasSize(1)
				.first()
				.satisfies(Consumer {
					projectedEntities(it)
				})
	}

	@Test // GH-2349
	fun projectionsContainingKnownEntitiesShouldWorkFromTemplate(@Autowired template: Neo4jTemplate) {

		val results = template.find(PersonEntity::class.java).`as`(PersonDepartmentQueryResult::class.java)
				.matching("MATCH (person:PersonEntity)-[:MEMBER_OF]->(department:DepartmentEntity) RETURN person, department")
				.all()
		assertThat(results)
				.hasSize(1)
				.first()
				.satisfies(Consumer {
					projectedEntities(it)
				})
	}

	private fun projectedEntities(personAndDepartment: PersonDepartmentQueryResult) {
		assertThat(personAndDepartment.person).extracting { obj: PersonEntity -> obj.id }.isEqualTo("p1")
		assertThat(personAndDepartment.person).extracting { obj: PersonEntity -> obj.email }.isEqualTo("p1@dep1.org")
		assertThat(personAndDepartment.department).extracting { obj: DepartmentEntity -> obj.id }.isEqualTo("d1")
		assertThat(personAndDepartment.department).extracting { obj: DepartmentEntity -> obj.name }.isEqualTo("Dep1")
	}

	internal interface PersonRepository : Neo4jRepository<PersonEntity, String> {
		@Query(
				"""
        MATCH (person:PersonEntity)-[:MEMBER_OF]->(department:DepartmentEntity)
        RETURN person, department
    """
		)
		fun findPersonWithDepartment(): List<PersonDepartmentQueryResult>
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	open class Config : AbstractNeo4jConfig() {

		@Bean
		open fun bookmarkCapture(): BookmarkCapture {
			return BookmarkCapture()
		}

		@Bean
		override fun transactionManager(driver: Driver, databaseNameProvider: DatabaseSelectionProvider): PlatformTransactionManager {
			val bookmarkCapture = bookmarkCapture()
			return Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture))
		}

		/**
		 * Make sure that particular entity is know. This is essential to all tests GH-2349
		 */
		override fun getMappingBasePackages(): Collection<String> {
			return listOf(DepartmentEntity::class.java.getPackage().name)
		}

		@Bean
		override fun driver(): Driver {
			return neo4jConnectionSupport.driver
		}
	}
}
