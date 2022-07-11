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
package org.springframework.data.neo4j.integration.reactive;

import org.junit.jupiter.api.AfterAll;
import org.neo4j.driver.reactive.ReactiveSession;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;

import ac.simons.neo4j.migrations.core.Migrations;
import ac.simons.neo4j.migrations.core.MigrationsConfig;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Predicate;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
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
 * In case you are wondering about the class naming: It is indeed an integration test. But if we name it `IT`, it will
 * be run the SDN Jar and only that, our migration scripts won't be included in that Jar, and migrations can't be found.
 * This is likely due to the parent build and it's not worth investigating much here unless we decide to have more tests
 * based on migrations.
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
// Not actually incompatible, but not worth the effort adding additional complexity for handling bookmarks
// between fixture and test
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
class ReactiveExceptionTranslationTest {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	// @formatter:off
	private final Predicate<Throwable> aTranslatedException = ex -> ex instanceof DataIntegrityViolationException && (
			ex.getMessage().matches(
					"Node\\(\\d+\\) already exists with label `SimplePerson` and property `name` = '[\\w\\s]+'; Error code 'Neo\\.ClientError\\.Schema\\.ConstraintValidationFailed'.*") ||
			ex.getMessage().matches(
					"New data does not satisfy Constraint\\( id=\\d+, name='simple_person__unique_name', type='UNIQUENESS', schema=\\(:SimplePerson \\{name}\\), ownedIndex=\\d+ \\): Both node \\d+ and node -?\\d+ share the property value \\( String\\(\"Tom\"\\) \\); Error code 'Neo\\.ClientError\\.Schema\\.ConstraintValidationFailed'")
	);
	// @formatter:on

	@BeforeAll
	static void createConstraints(@Autowired Driver driver, @Autowired Migrations migrations) {
		try (var session = driver.session()) {
			session.run("MATCH (l:`__Neo4jMigrationsLock`) delete l").consume();
		}
		migrations.apply();
	}

	@AfterAll
	static void cleanMigrations(@Autowired Migrations migrations) {
		migrations.clean(true);
	}

	@BeforeEach
	void clearDatabase(@Autowired Driver driver) {

		Flux.using(driver::reactiveSession,
						session -> JdkFlowAdapter.flowPublisherToFlux(session.run("MATCH (n:SimplePerson) DETACH DELETE n"))
								.flatMap(rs -> JdkFlowAdapter.flowPublisherToFlux(rs.consume())),
						ReactiveSession::close)
				.then().as(StepVerifier::create).verifyComplete();
	}

	@Test
	void exceptionsFromClientShouldBeTranslated(@Autowired ReactiveNeo4jClient neo4jClient) {
		neo4jClient.query("CREATE (:SimplePerson {name: 'Tom'})").run().then().as(StepVerifier::create)
				.verifyComplete();

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
	static class Config extends Neo4jReactiveTestConfiguration {

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

		@Bean
		public Migrations migrations(@Autowired Driver driver) {
			return new Migrations(
					MigrationsConfig.builder().withLocationsToScan("classpath:/data/migrations")
							.build(), driver);
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	interface SimplePersonRepository extends ReactiveNeo4jRepository<SimplePerson, Long> {
	}

	@Repository
	static class CustomDAO {

		private final ReactiveNeo4jClient neo4jClient;

		CustomDAO(ReactiveNeo4jClient neo4jClient) {
			this.neo4jClient = neo4jClient;
		}

		public Mono<ResultSummary> createPerson() {
			return neo4jClient.delegateTo(
					rxQueryRunner ->
							JdkFlowAdapter.flowPublisherToFlux(rxQueryRunner.run("CREATE (:SimplePerson {name: 'Tom'})"))
							.flatMap(result -> JdkFlowAdapter.flowPublisherToFlux(result.consume())).single()).run();
		}
	}
}
