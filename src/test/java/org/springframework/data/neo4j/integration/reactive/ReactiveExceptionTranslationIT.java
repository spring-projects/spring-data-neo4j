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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assumptions.assumeThat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.internal.util.ServerVersion;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.integration.shared.common.SimplePerson;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.ReactivePersistenceExceptionTranslationPostProcessor;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
// Not actually incompatible, but not worth the effort adding additional complexity for handling bookmarks
// between fixture and test
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
class ReactiveExceptionTranslationIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	// @formatter:off
	private final Predicate<Throwable> aTranslatedException = ex -> ex instanceof DataIntegrityViolationException && //
			ex.getMessage().matches(
					"Node\\(\\d+\\) already exists with label `SimplePerson` and property `name` = '[\\w\\s]+'; Error code 'Neo.ClientError.Schema.ConstraintValidationFailed';.*");
	// @formatter:on

	@BeforeAll
	static void createConstraints(@Autowired Driver driver) {

		Flux.using(driver::rxSession,
				session -> session.run("CREATE CONSTRAINT ON (person:SimplePerson) ASSERT person.name IS UNIQUE").consume(),
				RxSession::close).then().as(StepVerifier::create).verifyComplete();
	}

	@AfterAll
	static void dropConstraints(@Autowired Driver driver) {

		Flux.using(driver::rxSession,
				session -> session.run("DROP CONSTRAINT ON (person:SimplePerson) ASSERT person.name IS UNIQUE").consume(),
				RxSession::close).then().as(StepVerifier::create).verifyComplete();
	}

	@BeforeEach
	void clearDatabase(@Autowired Driver driver) {

		Flux.using(driver::rxSession, session -> session.run("MATCH (n) DETACH DELETE n").consume(), RxSession::close)
				.then().as(StepVerifier::create).verifyComplete();
	}

	@BeforeEach
	void assumeNeo4jLowerThan44() {

		assumeThat(neo4jConnectionSupport.getServerVersion()
				.lessThanOrEqual(ServerVersion.version("Neo4j/4.3.0"))).isTrue();
	}

	@Test
	void exceptionsFromClientShouldBeTranslated(@Autowired ReactiveNeo4jClient neo4jClient) {

		neo4jClient.query("CREATE (:SimplePerson {name: 'Tom'})").run().then().as(StepVerifier::create).verifyComplete();

		neo4jClient.query("CREATE (:SimplePerson {name: 'Tom'})").run().as(StepVerifier::create)
				.verifyErrorMatches(aTranslatedException);
	}

	@Test
	void exceptionsFromRepositoriesShouldBeTranslated(@Autowired SimplePersonRepository repository) {
		repository.save(new SimplePerson("Tom")).then().as(StepVerifier::create).verifyComplete();

		repository.save(new SimplePerson("Tom")).as(StepVerifier::create).verifyErrorMatches(aTranslatedException);
	}

	@Test
	void exceptionsOnRepositoryBeansShouldBeTranslated(@Autowired CustomDAO customDAO) {
		customDAO.createPerson().then().as(StepVerifier::create).verifyComplete();

		customDAO.createPerson().as(StepVerifier::create).verifyErrorMatches(aTranslatedException);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public CustomDAO customDAO(ReactiveNeo4jClient neo4jClient) {
			return new CustomDAO(neo4jClient);
		}

		// If someone wants to use the plain driver or the delegating mechanism of the client, than they must provide a
		// couple of more beans.
		@Bean
		public Neo4jPersistenceExceptionTranslator neo4jPersistenceExceptionTranslator() {
			return new Neo4jPersistenceExceptionTranslator();
		}

		@Bean
		public ReactivePersistenceExceptionTranslationPostProcessor persistenceExceptionTranslationPostProcessor() {
			return new ReactivePersistenceExceptionTranslationPostProcessor();
		}
	}

	interface SimplePersonRepository extends ReactiveNeo4jRepository<SimplePerson, Long> {}

	@Repository
	static class CustomDAO {

		private final ReactiveNeo4jClient neo4jClient;

		CustomDAO(ReactiveNeo4jClient neo4jClient) {
			this.neo4jClient = neo4jClient;
		}

		public Mono<ResultSummary> createPerson() {
			return neo4jClient.delegateTo(rxQueryRunner -> {
				RxResult rxResult = rxQueryRunner.run("CREATE (:SimplePerson {name: 'Tom'})");
				return Flux.from(rxResult.records()).then(Mono.from(rxResult.consume()));
			}).run();
		}
	}
}
