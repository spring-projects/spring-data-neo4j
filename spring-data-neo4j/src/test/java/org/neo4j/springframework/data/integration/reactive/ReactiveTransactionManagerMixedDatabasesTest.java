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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * The goal of this tests is to ensure a sensible coexistence of declarative {@link Transactional @Transactional}
 * transaction when the user uses the {@link Neo4jClient} in the same or another database.
 * <p>
 * While it does not integrate against a real database (multi-database is an enterprise feature), it is still an integration
 * test due to the high integration with Spring framework code.
 */
@ExtendWith(SpringExtension.class)
class ReactiveTransactionManagerMixedDatabasesTest {

	protected static final String DATABASE_NAME = "boom";
	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	private final Driver driver;

	private final ReactiveNeo4jClient neo4jClient;

	private final ReactiveNeo4jTransactionManager neo4jTransactionManager;

	private final ReactivePersonRepository repository;

	private final WrapperService wrappingComponent;

	@Autowired
	ReactiveTransactionManagerMixedDatabasesTest(Driver driver, ReactiveNeo4jClient neo4jClient,
		ReactiveNeo4jTransactionManager neo4jTransactionManager,
		ReactivePersonRepository repository,
		WrapperService wrappingComponent) {
		this.driver = driver;
		this.neo4jClient = neo4jClient;
		this.neo4jTransactionManager = neo4jTransactionManager;
		this.repository = repository;
		this.wrappingComponent = wrappingComponent;
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
	void usingSameDatabaseExplicitTx() {
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
	void usingAnotherDatabaseExplicitTx() {

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
				LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null))
				.as(otherTransactionTemplate::transactional);

		StepVerifier
			.create(p)
			.expectErrorMatches(e ->
				e instanceof IllegalStateException && e.getMessage().equals(
					"There is already an ongoing Spring transaction for 'boom', but you request the default database"))
			.verify();
	}

	/**
	 * We need this wrapper service, as reactive {@link Transactional @Transactional} annotated methods are not
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

			Record boomRecord = mock(Record.class);
			when(boomRecord.size()).thenReturn(1);
			when(boomRecord.get(0)).thenReturn(Values.value(1L));

			Record defaultRecord = mock(Record.class);
			when(defaultRecord.size()).thenReturn(1);
			when(defaultRecord.get(0)).thenReturn(Values.value(0L));

			RxResult boomResult = mock(RxResult.class);
			when(boomResult.records()).thenReturn(Mono.just(boomRecord));

			RxResult defaultResult = mock(RxResult.class);
			when(defaultResult.records()).thenReturn(Mono.just(defaultRecord));

			RxTransaction boomTransaction = mock(RxTransaction.class);
			when(boomTransaction.run(eq(TEST_QUERY), any(Map.class))).thenReturn(boomResult);
			when(boomTransaction.commit()).thenReturn(Mono.empty());
			when(boomTransaction.rollback()).thenReturn(Mono.empty());

			RxTransaction defaultTransaction = mock(RxTransaction.class);
			when(defaultTransaction.run(eq(TEST_QUERY), any(Map.class))).thenReturn(defaultResult);
			when(defaultTransaction.commit()).thenReturn(Mono.empty());
			when(defaultTransaction.rollback()).thenReturn(Mono.empty());

			RxSession boomSession = mock(RxSession.class);
			when(boomSession.run(eq(TEST_QUERY), any(Map.class))).thenReturn(boomResult);
			when(boomSession.beginTransaction()).thenReturn(Mono.just(boomTransaction));
			when(boomSession.beginTransaction(any(TransactionConfig.class))).thenReturn(Mono.just(boomTransaction));
			when(boomSession.close()).thenReturn(Mono.empty());

			RxSession defaultSession = mock(RxSession.class);
			when(defaultSession.run(eq(TEST_QUERY), any(Map.class))).thenReturn(defaultResult);
			when(defaultSession.beginTransaction()).thenReturn(Mono.just(defaultTransaction));
			when(defaultSession.beginTransaction(any(TransactionConfig.class)))
				.thenReturn(Mono.just(defaultTransaction));
			when(defaultSession.close()).thenReturn(Mono.empty());

			Driver driver = mock(Driver.class);
			when(driver.rxSession()).thenReturn(defaultSession);
			when(driver.rxSession(any(SessionConfig.class))).then(invocation -> {
				SessionConfig sessionConfig = invocation.getArgument(0);
				return sessionConfig.database().map(n -> n.equals(DATABASE_NAME) ? boomSession : defaultSession)
					.orElse(defaultSession);
			});

			return driver;
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
