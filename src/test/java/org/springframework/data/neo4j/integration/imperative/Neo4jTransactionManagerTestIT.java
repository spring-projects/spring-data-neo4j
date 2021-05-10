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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class Neo4jTransactionManagerTestIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void clearDatabase(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n").consume());
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2193
	void exceptionShouldNotBeShadowed(
			@Autowired TransactionTemplate transactionTemplate,
			@Autowired Neo4jClient client,
			@Autowired BookmarkCapture bookmarkCapture,
			@Autowired SomeRepository someRepository) {

		assertThatExceptionOfType(InvalidDataAccessResourceUsageException.class).isThrownBy(() ->
				// Need to wrap so that we actually trigger the setRollBackOnly on the outer transaction
				transactionTemplate.executeWithoutResult(tx -> {
					client.query("CREATE (n:ShouldNotBeThere)").run();
					someRepository.broken();
				})).withMessageStartingWith("Invalid input");

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			long cnt = session
					.readTransaction(tx -> tx.run("MATCH (n:ShouldNotBeThere) RETURN count(n)").single().get(0))
					.asLong();
			assertThat(cnt).isEqualTo(0L);
		}
	}

	interface SomeRepository extends Neo4jRepository<Person, Long> {

		@Transactional // The annotation on the Neo4jRepository is not inherited on the derived methods
		@Query("Kaputt")
		Person broken();
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
