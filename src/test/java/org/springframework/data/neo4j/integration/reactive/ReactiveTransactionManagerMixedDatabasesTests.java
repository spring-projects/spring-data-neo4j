/*
 * Copyright 2011-2025 the original author or authors.
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import org.neo4j.driver.summary.ResultSummary;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.reactive.repositories.ReactivePersonRepository;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * The goal of these tests is to ensure a sensible coexistence of declarative
 * {@link Transactional @Transactional} transaction when the user uses the
 * {@link Neo4jClient} in the same or another database.
 * <p>
 * While it does not integrate against a real database (multi-database is an enterprise
 * feature), it is still an integration test due to the high integration with Spring
 * framework code.
 */
@ExtendWith(SpringExtension.class)
class ReactiveTransactionManagerMixedDatabasesTests {

	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	protected static final String DATABASE_NAME = "boom";

	private final Driver driver;

	private final ReactiveNeo4jTransactionManager neo4jTransactionManager;

	@Autowired
	ReactiveTransactionManagerMixedDatabasesTests(Driver driver,
			ReactiveNeo4jTransactionManager neo4jTransactionManager) {

		this.driver = driver;
		this.neo4jTransactionManager = neo4jTransactionManager;
	}

	@Test
	void withoutActiveTransactions(@Autowired ReactiveNeo4jClient neo4jClient) {

		Mono<Long> numberOfNodes = neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();

		StepVerifier.create(numberOfNodes).expectNext(1L).verifyComplete();
	}

	@Test
	void usingTheSameDatabaseDeclarative(@Autowired WrapperService wrapperService) {

		StepVerifier.create(wrapperService.usingTheSameDatabaseDeclarative()).expectNext(0L).verifyComplete();
	}

	@Test
	void usingSameDatabaseExplicitTx(@Autowired ReactiveNeo4jClient neo4jClient) {
		ReactiveNeo4jTransactionManager otherTransactionManger = new ReactiveNeo4jTransactionManager(this.driver,
				ReactiveDatabaseSelectionProvider.createStaticDatabaseSelectionProvider(DATABASE_NAME));
		TransactionalOperator otherTransactionTemplate = TransactionalOperator.create(otherTransactionManger);

		Mono<Long> numberOfNodes = neo4jClient.query(TEST_QUERY)
			.in(DATABASE_NAME)
			.fetchAs(Long.class)
			.one()
			.as(otherTransactionTemplate::transactional);

		StepVerifier.create(numberOfNodes).expectNext(1L).verifyComplete();
	}

	@Test
	void usingAnotherDatabaseDeclarative(@Autowired WrapperService wrapperService) {

		StepVerifier.create(wrapperService.usingAnotherDatabaseDeclarative())
			.expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage()
				.equals("There is already an ongoing Spring transaction for the default user of the default database, but you requested the default user of 'boom'"))
			.verify();
	}

	@Test
	void usingAnotherDatabaseExplicitTx(@Autowired ReactiveNeo4jClient neo4jClient) {

		TransactionalOperator transactionTemplate = TransactionalOperator.create(this.neo4jTransactionManager);

		Mono<Long> numberOfNodes = neo4jClient.query("MATCH (n) RETURN COUNT(n)")
			.in(DATABASE_NAME)
			.fetchAs(Long.class)
			.one()
			.as(transactionTemplate::transactional);

		StepVerifier.create(numberOfNodes)
			.expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage()
				.equals("There is already an ongoing Spring transaction for the default user of the default database, but you requested the default user of 'boom'"))
			.verify();
	}

	@Test
	void usingAnotherDatabaseDeclarativeFromRepo(@Autowired ReactivePersonRepository repository) {

		ReactiveNeo4jTransactionManager otherTransactionManger = new ReactiveNeo4jTransactionManager(this.driver,
				ReactiveDatabaseSelectionProvider.createStaticDatabaseSelectionProvider(DATABASE_NAME));
		TransactionalOperator otherTransactionTemplate = TransactionalOperator.create(otherTransactionManger);

		Mono<PersonWithAllConstructor> p = repository
			.save(new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
					LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null))
			.as(otherTransactionTemplate::transactional);

		StepVerifier.create(p)
			.expectErrorMatches(e -> e instanceof IllegalStateException && e.getMessage()
				.equals("There is already an ongoing Spring transaction for the default user of 'boom', but you requested the default user of the default database"))
			.verify();
	}

	/**
	 * We need this wrapper service, as reactive {@link Transactional @Transactional}
	 * annotated methods are not recognized as such (See other also
	 * https://github.com/spring-projects/spring-framework/issues/23277). The class must
	 * be public to make the declarative transactions work. Please don't change its
	 * visibility.
	 */
	public static class WrapperService {

		private final ReactiveNeo4jClient neo4jClient;

		WrapperService(ReactiveNeo4jClient neo4jClient) {
			this.neo4jClient = neo4jClient;
		}

		@Transactional
		public Mono<Long> usingTheSameDatabaseDeclarative() {

			return this.neo4jClient.query(TEST_QUERY).fetchAs(Long.class).one();
		}

		@Transactional
		public Mono<Long> usingAnotherDatabaseDeclarative() {
			return this.neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();
		}

	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		@Override
		public Driver driver() {

			Record boomRecord = mock(Record.class);
			given(boomRecord.size()).willReturn(1);
			given(boomRecord.get(0)).willReturn(Values.value(1L));

			Record defaultRecord = mock(Record.class);
			given(defaultRecord.size()).willReturn(1);
			given(defaultRecord.get(0)).willReturn(Values.value(0L));

			ReactiveResult boomResult = mock(ReactiveResult.class);
			given(boomResult.records()).willReturn(Mono.just(boomRecord));
			given(boomResult.consume()).willReturn(Mono.just(mock(ResultSummary.class)));

			ReactiveResult defaultResult = mock(ReactiveResult.class);
			given(defaultResult.records()).willReturn(Mono.just(defaultRecord));
			given(defaultResult.consume()).willReturn(Mono.just(mock(ResultSummary.class)));

			ReactiveTransaction boomTransaction = mock(ReactiveTransaction.class);
			given(boomTransaction.run(eq(TEST_QUERY), any(Map.class))).willReturn(Mono.just(boomResult));
			given(boomTransaction.commit()).willReturn(Mono.empty());
			given(boomTransaction.rollback()).willReturn(Mono.empty());

			ReactiveTransaction defaultTransaction = mock(ReactiveTransaction.class);
			given(defaultTransaction.run(eq(TEST_QUERY), any(Map.class))).willReturn(Mono.just(defaultResult));
			given(defaultTransaction.commit()).willReturn(Mono.empty());
			given(defaultTransaction.rollback()).willReturn(Mono.empty());

			ReactiveSession boomSession = mock(ReactiveSession.class);
			given(boomSession.run(eq(TEST_QUERY), any(Map.class))).willReturn(Mono.just(boomResult));
			given(boomSession.beginTransaction()).willReturn(Mono.just(boomTransaction));
			given(boomSession.beginTransaction(any(TransactionConfig.class))).willReturn(Mono.just(boomTransaction));
			given(boomSession.close()).willReturn(Mono.empty());

			ReactiveSession defaultSession = mock(ReactiveSession.class);
			given(defaultSession.run(eq(TEST_QUERY), any(Map.class))).willReturn(Mono.just(defaultResult));
			given(defaultSession.beginTransaction()).willReturn(Mono.just(defaultTransaction));
			given(defaultSession.beginTransaction(any(TransactionConfig.class)))
				.willReturn(Mono.just(defaultTransaction));
			given(defaultSession.close()).willReturn(Mono.empty());

			Driver driver = mock(Driver.class);
			given(driver.session(ReactiveSession.class)).willReturn(defaultSession);
			given(driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).will(invocation -> {
				SessionConfig sessionConfig = invocation.getArgument(1);
				return sessionConfig.database()
					.filter(n -> n.equals(DATABASE_NAME))
					.map(n -> boomSession)
					.orElse(defaultSession);
			});

			return driver;
		}

		@Bean
		WrapperService wrapperService(ReactiveNeo4jClient reactiveNeo4jClient) {
			return new WrapperService(reactiveNeo4jClient);
		}

	}

}
