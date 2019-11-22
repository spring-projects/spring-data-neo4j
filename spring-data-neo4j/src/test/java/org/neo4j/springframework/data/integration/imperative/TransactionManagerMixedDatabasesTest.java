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
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.Values;
import org.neo4j.driver.SessionConfig;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The goal of this tests is to ensure a sensible coexistence of declarative {@link Transactional @Transactional}
 * transaction when the user uses the {@link Neo4jClient} in the same or another database.
 * <p>
 * While it does not integrate against a real database (multi-database is an enterprise feature), it is still an integration
 * test due to the high integration with Spring framework code.
 */
@ExtendWith(SpringExtension.class)
class TransactionManagerMixedDatabasesTest {

	protected static final String DATABASE_NAME = "boom";
	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	private final Driver driver;

	private final Neo4jClient neo4jClient;

	private final TransactionTemplate transactionTemplate;

	private final PersonRepository repository;

	@Autowired
	TransactionManagerMixedDatabasesTest(Driver driver, Neo4jClient neo4jClient,
		Neo4jTransactionManager neo4jTransactionManager,
		PersonRepository repository) {
		this.driver = driver;
		this.neo4jClient = neo4jClient;
		this.transactionTemplate = new TransactionTemplate(neo4jTransactionManager);
		this.repository = repository;
	}

	@Test
	void withoutActiveTransactions() {

		Optional<Long> numberOfNodes =
			neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Transactional
	@Test
	void usingTheSameDatabaseDeclarative() {

		Optional<Long> numberOfNodes =
			neo4jClient.query(TEST_QUERY).in(null).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(0L);
	}

	@Test
	void usingSameDatabaseExplicitTx() {
		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(driver, DATABASE_NAME);
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		Optional<Long> numberOfNodes = otherTransactionTemplate.execute(
			tx -> neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one());
		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Test
	@Transactional
	void usingAnotherDatabaseDeclarative() {

		assertThatIllegalStateException().isThrownBy(
			() -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one())
			.withMessage("There is already an ongoing Spring transaction for the default database, but you request 'boom'");

	}

	@Test
	void usingAnotherDatabaseExplicitTx() {

		assertThatIllegalStateException().isThrownBy(
			() -> transactionTemplate.execute(
				tx -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one()))
			.withMessage("There is already an ongoing Spring transaction for the default database, but you request 'boom'");
	}

	@Test
	void usingAnotherDatabaseDeclarativeFromRepo() {

		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(driver, DATABASE_NAME);
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		assertThatIllegalStateException().isThrownBy(
			() -> otherTransactionTemplate.execute(
				tx -> repository.save(new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
					LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null))))
			.withMessage("There is already an ongoing Spring transaction for 'boom', but you request the default database");
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			Record boomRecord = mock(Record.class);
			when(boomRecord.size()).thenReturn(1);
			when(boomRecord.get(0)).thenReturn(Values.value(1L));

			Record defaultRecord = mock(Record.class);
			when(defaultRecord.size()).thenReturn(1);
			when(defaultRecord.get(0)).thenReturn(Values.value(0L));

			Result boomResult = mock(Result.class);
			when(boomResult.hasNext()).thenReturn(true);
			when(boomResult.single()).thenReturn(boomRecord);

			Result defaultResult = mock(Result.class);
			when(defaultResult.hasNext()).thenReturn(true);
			when(defaultResult.single()).thenReturn(defaultRecord);

			Transaction boomTransaction = mock(Transaction.class);
			when(boomTransaction.run(eq(TEST_QUERY), any(Map.class))).thenReturn(boomResult);
			when(boomTransaction.isOpen()).thenReturn(true);

			Transaction defaultTransaction = mock(Transaction.class);
			when(defaultTransaction.run(eq(TEST_QUERY), any(Map.class))).thenReturn(defaultResult);
			when(defaultTransaction.isOpen()).thenReturn(true);

			Session boomSession = mock(Session.class);
			when(boomSession.run(eq(TEST_QUERY), any(Map.class))).thenReturn(boomResult);
			when(boomSession.beginTransaction(any(TransactionConfig.class))).thenReturn(boomTransaction);
			when(boomSession.isOpen()).thenReturn(true);

			Session defaultSession = mock(Session.class);
			when(defaultSession.run(eq(TEST_QUERY), any(Map.class))).thenReturn(defaultResult);
			when(defaultSession.beginTransaction(any(TransactionConfig.class))).thenReturn(defaultTransaction);
			when(defaultSession.isOpen()).thenReturn(true);

			Driver driver = mock(Driver.class);
			when(driver.session()).thenReturn(defaultSession);
			when(driver.session(any(SessionConfig.class))).then(invocation -> {
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
	}
}
