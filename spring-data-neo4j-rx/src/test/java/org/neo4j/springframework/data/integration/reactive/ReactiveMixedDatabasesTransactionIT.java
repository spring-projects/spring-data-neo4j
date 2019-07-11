/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.reactive;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.internal.SessionConfig;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * The goal of this integration tests is to ensure a sensible coexistence of declarative {@link Transactional @Transactional}
 * transaction when the user uses the {@link Neo4jClient} in the same or another database.
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(Neo4jExtension.class)
@ContextConfiguration(classes = ReactiveMixedDatabasesTransactionIT.Config.class)
public class ReactiveMixedDatabasesTransactionIT {

	protected static final String DATABASE_NAME = "boom";
	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	private final ReactiveNeo4jClient neo4jClient;

	private final ReactiveNeo4jTransactionManager neo4jTransactionManager;

	private final ReactivePersonRepository repository;

	private final WrapperService wrappingComponent;

	@Autowired
	public ReactiveMixedDatabasesTransactionIT(Driver driver, ReactiveNeo4jClient neo4jClient,
		ReactiveNeo4jTransactionManager neo4jTransactionManager,
		ReactivePersonRepository repository,
		WrapperService wrappingComponent) {
		this.driver = driver;
		this.neo4jClient = neo4jClient;
		this.neo4jTransactionManager = neo4jTransactionManager;
		this.repository = repository;
		this.wrappingComponent = wrappingComponent;
	}

	@BeforeEach
	protected void setupDatabase() {

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("DROP DATABASE " + DATABASE_NAME);
		} catch (DatabaseException e) {
			// Database does probably not exist
		}

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("CREATE DATABASE " + DATABASE_NAME);
		}

		try (Session session = driver.session(SessionConfig.forDatabase(DATABASE_NAME))) {
			session.run("CREATE (n:DbTest) RETURN n");
		}
	}

	@Test
	void withoutActiveTransactions() {

		Mono<Long> numberOfNodes =
			neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();

		StepVerifier
			.create(numberOfNodes)
			.expectNext(1L)
			.verifyComplete();
	}

	@Test
	void usingTheSameDatabaseDeclarative() {

		StepVerifier
			.create(wrappingComponent.usingTheSameDatabaseDeclarative())
			.expectNext(0L)
			.verifyComplete();
	}

	@Test
	void usingSameDatabaseExplizitTx() {
		ReactiveNeo4jTransactionManager otherTransactionManger = new ReactiveNeo4jTransactionManager(driver,
			DATABASE_NAME);
		TransactionalOperator otherTransactionTemplate = TransactionalOperator.create(otherTransactionManger);

		Mono<Long> numberOfNodes = neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one()
			.as(otherTransactionTemplate::transactional);

		StepVerifier
			.create(numberOfNodes)
			.expectNext(1L)
			.verifyComplete();
	}

	@Test
	void usingAnotherDatabaseDeclarative() {

		StepVerifier
			.create(wrappingComponent.usingAnotherDatabaseDeclarative())
			.expectErrorMatches(e ->
				e instanceof IllegalStateException && e.getMessage().equals(
					"There is already an ongoing Spring transaction for the default database, but you request 'boom'"))
			.verify();
	}

	@Test
	void usingAnotherDatabaseExplizitTx() {

		TransactionalOperator transactionTemplate = TransactionalOperator.create(neo4jTransactionManager);

		Mono<Long> numberOfNodes = neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class)
			.one()
			.as(transactionTemplate::transactional);

		StepVerifier
			.create(numberOfNodes)
			.expectErrorMatches(e ->
				e instanceof IllegalStateException && e.getMessage().equals(
					"There is already an ongoing Spring transaction for the default database, but you request 'boom'"))
			.verify();
	}

	@Test
	void usingAnotherDatabaseDeclarativeFromRepo() {

		ReactiveNeo4jTransactionManager otherTransactionManger = new ReactiveNeo4jTransactionManager(driver,
			DATABASE_NAME);
		TransactionalOperator otherTransactionTemplate = TransactionalOperator.create(otherTransactionManger);

		Mono<PersonWithAllConstructor> p =
			repository.save(new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
				LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null))
				.as(otherTransactionTemplate::transactional);

		StepVerifier
			.create(p)
			.expectErrorMatches(e ->
				e instanceof IllegalStateException && e.getMessage().equals(
					"There is already an ongoing Spring transaction for 'boom', but you request the default database"))
			.verify();
	}

	/**
	 * We need this wrapper service, as reactive {@link Transactional @Transactional} annoted methods are not
	 * recognized as such (See other also https://github.com/spring-projects/spring-framework/issues/23277).
	 *
	 * The class must be public to make the declarative transactions work. Please don't change its visibility.
	 */
	public static class WrapperService {

		private final ReactiveNeo4jClient neo4jClient;

		WrapperService(ReactiveNeo4jClient neo4jClient) {
			this.neo4jClient = neo4jClient;
		}

		@Transactional
		public Mono<Long> usingTheSameDatabaseDeclarative() {

			return neo4jClient.query(TEST_QUERY).in(null).fetchAs(Long.class).one();
		}

		@Transactional
		public Mono<Long> usingAnotherDatabaseDeclarative() {
			return neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.openConnection();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		public WrapperService wrapperService(ReactiveNeo4jClient reactiveNeo4jClient) {
			return new WrapperService(reactiveNeo4jClient);
		}
	}
}
