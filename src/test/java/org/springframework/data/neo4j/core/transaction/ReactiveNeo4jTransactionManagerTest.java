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
package org.springframework.data.neo4j.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
import reactor.adapter.JdkFlowAdapter;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.reactive.ReactiveSession;
import org.neo4j.driver.reactive.ReactiveTransaction;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
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

	private DatabaseSelection databaseSelection = DatabaseSelection.byName("aDatabase");
	private UserSelection userSelection = UserSelection.connectedUser();

	@Mock private Driver driver;

	@Mock private ReactiveSession session;
	@Mock private ReactiveTransaction transaction;

	@BeforeEach
	void setUp() {

		when(driver.reactiveSession(any(SessionConfig.class))).thenReturn(session);
		when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(JdkFlowAdapter.publisherToFlowPublisher(Mono.just(transaction)));
		when(transaction.rollback()).thenReturn(JdkFlowAdapter.publisherToFlowPublisher(Mono.empty()));
		when(transaction.commit()).thenReturn(JdkFlowAdapter.publisherToFlowPublisher(Mono.empty()));
		when(session.close()).thenReturn(JdkFlowAdapter.publisherToFlowPublisher(Mono.empty()));
	}

	@Test
	void shouldWorkWithoutSynchronizations() {

		Mono<ReactiveTransaction> transactionMono = ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver,
				databaseSelection, userSelection);

		StepVerifier.create(transactionMono).verifyComplete();
	}

	@Nested
	class BasedOnNeo4jTransactions {
		@Test
		void shouldUseTxFromNeo4jTxManager() {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager.with(driver)
					.withDatabaseSelectionProvider(() -> Mono.just(databaseSelection))
					.build();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator
					.execute(transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction().doOnNext(tsm -> {
						assertThat(tsm.hasResource(driver)).isTrue();
						transactionStatus.setRollbackOnly();
					}).then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection)))
					.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(driver).reactiveSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}

		@Test
		void shouldParticipateInOngoingTransaction() {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager.with(driver)
					.withDatabaseSelectionProvider(() -> Mono.just(databaseSelection))
					.build();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator.execute(outerStatus -> {
				assertThat(outerStatus.isNewTransaction()).isTrue();
				outerStatus.setRollbackOnly();
				return transactionalOperator.execute(innerStatus -> {
					assertThat(innerStatus.isNewTransaction()).isFalse();
					return ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection);
				}).then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection));
			}).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(driver).reactiveSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}

		@Test
		void usesBookmarksCorrectly() throws Exception {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager.with(driver)
					.withDatabaseSelectionProvider(() -> Mono.just(databaseSelection))
					.build();

			AssertableBookmarkManager bookmarkManager = new AssertableBookmarkManager();
			injectBookmarkManager(txManager, bookmarkManager);

			Set<Bookmark> bookmark = Set.of(new BookmarkForTesting("blubb"));
			when(session.lastBookmarks()).thenReturn(bookmark);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator
					.execute(transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction()
							.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isTrue())
							.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection)))
					.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(driver).reactiveSession(any(SessionConfig.class));
			verify(session).beginTransaction(any(TransactionConfig.class));
			assertThat(bookmarkManager.getBookmarksCalled).isTrue();
			verify(session).close();
			verify(transaction).commit();
			assertThat(bookmarkManager.updateBookmarksCalled)
					.containsEntry(bookmark, true);
		}

		private void injectBookmarkManager(ReactiveNeo4jTransactionManager txManager, Neo4jBookmarkManager value)
				throws NoSuchFieldException, IllegalAccessException {
			Field bookmarkManager = ReactiveNeo4jTransactionManager.class.getDeclaredField("bookmarkManager");
			bookmarkManager.setAccessible(true);
			bookmarkManager.set(txManager, value);
		}
	}

	@Nested
	class BasedOnOtherTransactions {

		@Test
		void shouldSynchronizeWithExternalWithCommit() {

			R2dbcTransactionManager t = new R2dbcTransactionManager(
					new H2ConnectionFactory(H2ConnectionConfiguration.builder().inMemory("test").build()));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(t);

			transactionalOperator
					.execute(transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction()
							.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isFalse())
							.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection))
							.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
									.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isTrue())))
					.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(driver).reactiveSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).commit();
			verify(transaction, never()).rollback();
		}

		@Test
		void shouldSynchronizeWithExternalWithRollback() {

			R2dbcTransactionManager t = new R2dbcTransactionManager(
					new H2ConnectionFactory(H2ConnectionConfiguration.builder().inMemory("test").build()));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(t);

			transactionalOperator
					.execute(transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction().doOnNext(tsm -> {
						assertThat(tsm.hasResource(driver)).isFalse();
						transactionStatus.setRollbackOnly();
					}).then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(driver, databaseSelection, userSelection))
							.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
									.doOnNext(tsm -> assertThat(tsm.hasResource(driver)).isTrue())))
					.as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(driver).reactiveSession(any(SessionConfig.class));

			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();
			verify(transaction).rollback();
			verify(transaction, never()).commit();
		}
	}
}
