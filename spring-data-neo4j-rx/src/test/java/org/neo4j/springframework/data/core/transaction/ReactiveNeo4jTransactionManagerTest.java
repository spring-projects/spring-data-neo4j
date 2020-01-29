/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.neo4j.springframework.data.core.transaction.ReactiveNeo4jTransactionManager.*;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import javax.transaction.UserTransaction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.driver.Driver;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.springframework.data.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveNeo4jTransactionManagerTest {

	private String databaseName = "aDatabase";

	@Mock
	private Driver driver;

	@Mock
	private RxSession session;
	@Mock
	private RxTransaction transaction;
	@Mock
	UserTransaction userTransaction;

	@BeforeEach
	void setUp() {

		when(driver.rxSession(any(SessionConfig.class))).thenReturn(session);
		when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(Mono.just(transaction));
		when(transaction.rollback()).thenReturn(Mono.empty());
		when(transaction.commit()).thenReturn(Mono.empty());
		when(session.close()).thenReturn(Mono.empty());
	}

	@Test
	void shouldWorkWithoutSynchronizations() {

		Mono<RxTransaction> transactionMono = retrieveReactiveTransaction(driver, databaseName);

		StepVerifier.create(transactionMono)
			.verifyComplete();
	}

	@Nested
	class BasedOnNeo4jTransactions {
		@Test
		void shouldUseTxFromNeo4jTxManager() {

			ReactiveNeo4jTransactionManager txManager = new ReactiveNeo4jTransactionManager(driver, ReactiveDatabaseSelectionProvider
				.createStaticDatabaseSelectionProvider(databaseName));
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator
				.execute(transactionStatus -> TransactionSynchronizationManager
					.forCurrentTransaction().doOnNext(tsm -> {
						assertThat(tsm.hasResource(driver)).isTrue();
						transactionStatus.setRollbackOnly();
					}).then(retrieveReactiveTransaction(driver, databaseName))
				)
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(driver).rxSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}

		@Test
		void shouldParticipateInOngoingTransaction() {

			ReactiveNeo4jTransactionManager txManager = new ReactiveNeo4jTransactionManager(driver, ReactiveDatabaseSelectionProvider
				.createStaticDatabaseSelectionProvider(databaseName));
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator
				.execute(outerStatus -> {
					assertThat(outerStatus.isNewTransaction()).isTrue();
					outerStatus.setRollbackOnly();
					return transactionalOperator.execute(innerStatus -> {
						assertThat(innerStatus.isNewTransaction()).isFalse();
						return retrieveReactiveTransaction(driver, databaseName);
					}).then(retrieveReactiveTransaction(driver, databaseName));
				})
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(driver).rxSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}
	}

	@Nested
	class BasedOnOtherTransactions {

		@Test
		void shouldSynchronizeWithExternalWithCommit() {

			R2dbcTransactionManager t = new R2dbcTransactionManager(new H2ConnectionFactory(
				H2ConnectionConfiguration.builder().inMemory("test").build()));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(t);

			transactionalOperator
				.execute(transactionStatus -> TransactionSynchronizationManager
					.forCurrentTransaction().doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isFalse())
					.then(retrieveReactiveTransaction(driver, databaseName))
					.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
						.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isTrue()))
				)
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(driver).rxSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).commit();
			verify(transaction, never()).rollback();
		}

		@Test
		void shouldSynchronizeWithExternalWithRollback() {

			R2dbcTransactionManager t = new R2dbcTransactionManager(new H2ConnectionFactory(
				H2ConnectionConfiguration.builder().inMemory("test").build()));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(t);

			transactionalOperator
				.execute(transactionStatus -> TransactionSynchronizationManager
					.forCurrentTransaction()
					.doOnNext(tsm -> {
						assertThat(tsm.hasResource(driver)).isFalse();
						transactionStatus.setRollbackOnly();
					})
					.then(retrieveReactiveTransaction(driver, databaseName))
					.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
						.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isTrue()))
				)
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(driver).rxSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}
	}
}
