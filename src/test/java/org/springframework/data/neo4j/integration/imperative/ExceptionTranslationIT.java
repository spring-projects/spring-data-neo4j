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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.internal.util.ServerVersion;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator;
import org.springframework.data.neo4j.integration.shared.common.SimplePerson;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
// Not actually incompatible, but not worth the effort adding additional complexity for handling bookmarks
// between fixture and test
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
class ExceptionTranslationIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void createConstraints(@Autowired Driver driver) {

		try (Session session = driver.session()) {
			session.run("CREATE CONSTRAINT ON (person:SimplePerson) ASSERT person.name IS UNIQUE").consume();
		}
	}

	@AfterAll
	static void dropConstraints(@Autowired Driver driver) {

		try (Session session = driver.session()) {
			session.run("DROP CONSTRAINT ON (person:SimplePerson) ASSERT person.name IS UNIQUE").consume();
		}
	}

	@BeforeEach
	void clearDatabase(@Autowired Driver driver) {
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
		}
	}

	@BeforeEach
	void assumeNeo4jLowerThan44() {

		assumeThat(neo4jConnectionSupport.getServerVersion()
				.lessThan(ServerVersion.version("Neo4j/4.4.0"))).isTrue();
	}

	@Test
	void exceptionsFromClientShouldBeTranslated(@Autowired Neo4jClient neo4jClient) {
		neo4jClient.query("CREATE (:SimplePerson {name: 'Tom'})").run();

		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> neo4jClient.query("CREATE (:SimplePerson {name: 'Tom'})").run()).withMessageMatching(
						"Node\\(\\d+\\) already exists with label `SimplePerson` and property `name` = '[\\w\\s]+'; Error code 'Neo.ClientError.Schema.ConstraintValidationFailed';.*");
	}

	@Test
	void exceptionsFromRepositoriesShouldBeTranslated(@Autowired SimplePersonRepository repository) {
		repository.save(new SimplePerson("Jerry"));

		assertThatExceptionOfType(DataIntegrityViolationException.class)
				.isThrownBy(() -> repository.save(new SimplePerson("Jerry"))).withMessageMatching(
						"Node\\(\\d+\\) already exists with label `SimplePerson` and property `name` = '[\\w\\s]+'; Error code 'Neo.ClientError.Schema.ConstraintValidationFailed';.*");
	}

	/*
	 * Only when an additional {@link PersistenceExceptionTranslationPostProcessor} has been provided.
	 */
	@Test
	void exceptionsOnRepositoryBeansShouldBeTranslated(@Autowired CustomDAO customDAO) {
		ResultSummary summary = customDAO.createPerson();
		assertThat(summary.counters().nodesCreated()).isEqualTo(1L);

		assertThatExceptionOfType(DataIntegrityViolationException.class).isThrownBy(() -> customDAO.createPerson())
				.withMessageMatching(
						"Node\\(\\d+\\) already exists with label `SimplePerson` and property `name` = '[\\w\\s]+'; Error code 'Neo.ClientError.Schema.ConstraintValidationFailed';.*");
	}

	interface SimplePersonRepository extends Neo4jRepository<SimplePerson, Long> {}

	@Repository
	static class CustomDAO {

		private final Neo4jClient neo4jClient;

		CustomDAO(Neo4jClient neo4jClient) {
			this.neo4jClient = neo4jClient;
		}

		public ResultSummary createPerson() {

			return neo4jClient
					.delegateTo(queryRunner -> Optional.of(queryRunner.run("CREATE (:SimplePerson {name: 'Tom'})").consume()))
					.run().get();
		}
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
					value = ExceptionTranslationIT.SimplePersonRepository.class))
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public CustomDAO customDAO(Neo4jClient neo4jClient) {
			return new CustomDAO(neo4jClient);
		}

		// If someone wants to use the plain driver or the delegating mechanism of the client, than they must provide a
		// couple of more beans.
		@Bean
		public Neo4jPersistenceExceptionTranslator neo4jPersistenceExceptionTranslator() {
			return new Neo4jPersistenceExceptionTranslator();
		}

		@Bean
		public PersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
			return new PersistenceExceptionTranslationPostProcessor();
		}
	}
}
