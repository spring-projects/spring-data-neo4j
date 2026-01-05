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
package org.springframework.data.neo4j.integration.imperative;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.ResultSummary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.imperative.repositories.PersonRepository;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;

/**
 * The goal of this tests is to ensure a sensible coexistence of declarative
 * {@link Transactional @Transactional} transaction when the user uses the
 * {@link Neo4jClient} in the same or another database.
 * <p>
 * While it does not integrate against a real database (multi-database is an enterprise
 * feature), it is still an integration test due to the high integration with Spring
 * framework code.
 *
 * @author Michael J. Simons
 */
@ExtendWith(SpringExtension.class)
class TransactionManagerMixedDatabasesTests {

	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	protected static final String DATABASE_NAME = "boom";

	private final Driver driver;

	private final TransactionTemplate transactionTemplate;

	@Autowired
	TransactionManagerMixedDatabasesTests(Driver driver, Neo4jTransactionManager neo4jTransactionManager) {

		this.driver = driver;
		this.transactionTemplate = new TransactionTemplate(neo4jTransactionManager);
	}

	@Test
	void withoutActiveTransactions(@Autowired Neo4jClient neo4jClient) {

		Optional<Long> numberOfNodes = neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Transactional
	@Test
	void usingTheSameDatabaseDeclarative(@Autowired Neo4jClient neo4jClient) {

		Optional<Long> numberOfNodes = neo4jClient.query(TEST_QUERY).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(0L);
	}

	@Test
	void usingSameDatabaseExplicitTx(@Autowired Neo4jClient neo4jClient) {

		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(this.driver,
				DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(DATABASE_NAME));
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		Optional<Long> numberOfNodes = otherTransactionTemplate
			.execute(tx -> neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one());
		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Test
	@Transactional
	void usingAnotherDatabaseDeclarative(@Autowired Neo4jClient neo4jClient) {

		assertThatIllegalStateException()
			.isThrownBy(
					() -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one())
			.withMessage(
					"There is already an ongoing Spring transaction for the default user of the default database, but you requested the default user of 'boom'");

	}

	@Test
	void usingAnotherDatabaseExplicitTx(@Autowired Neo4jClient neo4jClient) {

		assertThatIllegalStateException()
			.isThrownBy(() -> this.transactionTemplate.execute(
					tx -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one()))
			.withMessage(
					"There is already an ongoing Spring transaction for the default user of the default database, but you requested the default user of 'boom'");
	}

	@Test
	void usingAnotherDatabaseDeclarativeFromRepo(@Autowired PersonRepository repository) {

		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(this.driver,
				DatabaseSelectionProvider.createStaticDatabaseSelectionProvider(DATABASE_NAME));
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		assertThatIllegalStateException()
			.isThrownBy(() -> otherTransactionTemplate
				.execute(tx -> repository.save(new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true,
						1509L, LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null))))
			.withMessage(
					"There is already an ongoing Spring transaction for the default user of 'boom', but you requested the default user of the default database");
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		@Override
		public Driver driver() {

			Record boomRecord = mock(Record.class);
			given(boomRecord.size()).willReturn(1);
			given(boomRecord.get(0)).willReturn(Values.value(1L));

			Record defaultRecord = mock(Record.class);
			given(defaultRecord.size()).willReturn(1);
			given(defaultRecord.get(0)).willReturn(Values.value(0L));

			Result boomResult = mock(Result.class);
			given(boomResult.hasNext()).willReturn(true);
			given(boomResult.single()).willReturn(boomRecord);
			given(boomResult.consume()).willReturn(mock(ResultSummary.class));

			Result defaultResult = mock(Result.class);
			given(defaultResult.hasNext()).willReturn(true);
			given(defaultResult.single()).willReturn(defaultRecord);
			given(defaultResult.consume()).willReturn(mock(ResultSummary.class));

			Transaction boomTransaction = mock(Transaction.class);
			given(boomTransaction.run(eq(TEST_QUERY), any(Map.class))).willReturn(boomResult);
			given(boomTransaction.isOpen()).willReturn(true);

			Transaction defaultTransaction = mock(Transaction.class);
			given(defaultTransaction.run(eq(TEST_QUERY), any(Map.class))).willReturn(defaultResult);
			given(defaultTransaction.isOpen()).willReturn(true);

			Session boomSession = mock(Session.class);
			given(boomSession.run(eq(TEST_QUERY), any(Map.class))).willReturn(boomResult);
			given(boomSession.beginTransaction(any(TransactionConfig.class))).willReturn(boomTransaction);
			given(boomSession.isOpen()).willReturn(true);

			Session defaultSession = mock(Session.class);
			given(defaultSession.run(eq(TEST_QUERY), any(Map.class))).willReturn(defaultResult);
			given(defaultSession.beginTransaction(any(TransactionConfig.class))).willReturn(defaultTransaction);
			given(defaultSession.isOpen()).willReturn(true);

			Driver driver = mock(Driver.class);
			given(driver.session()).willReturn(defaultSession);
			given(driver.session(any(SessionConfig.class))).will(invocation -> {
				SessionConfig sessionConfig = invocation.getArgument(0);
				return sessionConfig.database()
					.map(n -> n.equals(DATABASE_NAME) ? boomSession : defaultSession)
					.orElse(defaultSession);
			});

			return driver;
		}

	}

}
