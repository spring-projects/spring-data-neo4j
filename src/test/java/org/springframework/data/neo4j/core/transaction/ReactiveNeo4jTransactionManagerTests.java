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
package org.springframework.data.neo4j.core.transaction;

import java.lang.reflect.Field;
import java.util.Set;

import io.r2dbc.h2.H2ConnectionConfiguration;
import io.r2dbc.h2.H2ConnectionFactory;
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
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.support.BookmarkManagerReference;
import org.springframework.data.r2dbc.connectionfactory.R2dbcTransactionManager;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReactiveNeo4jTransactionManagerTests {

	private DatabaseSelection databaseSelection = DatabaseSelection.byName("aDatabase");

	private UserSelection userSelection = UserSelection.connectedUser();

	@Mock
	private Driver driver;

	@Mock
	private ReactiveSession session;

	@Mock
	private ReactiveTransaction transaction;

	@BeforeEach
	void setUp() {

		given(this.driver.session(eq(ReactiveSession.class), any(SessionConfig.class))).willReturn(this.session);
		given(this.session.beginTransaction(any(TransactionConfig.class))).willReturn(Mono.just(this.transaction));
		given(this.transaction.rollback()).willReturn(Mono.empty());
		given(this.transaction.commit()).willReturn(Mono.empty());
		given(this.session.close()).willReturn(Mono.empty());
	}

	@Test
	void shouldWorkWithoutSynchronizations() {

		Mono<ReactiveTransaction> transactionMono = ReactiveNeo4jTransactionManager
			.retrieveReactiveTransaction(this.driver, this.databaseSelection, this.userSelection);

		StepVerifier.create(transactionMono).verifyComplete();
	}

	@Nested
	class BasedOnNeo4jTransactions {

		@Test
		void shouldUseTxFromNeo4jTxManager() {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager
				.with(ReactiveNeo4jTransactionManagerTests.this.driver)
				.withDatabaseSelectionProvider(
						() -> Mono.just(ReactiveNeo4jTransactionManagerTests.this.databaseSelection))
				.build();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator.execute(
					transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction().doOnNext(tsm -> {
						assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver)).isTrue();
						transactionStatus.setRollbackOnly();
					})
						.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
								ReactiveNeo4jTransactionManagerTests.this.driver,
								ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
								ReactiveNeo4jTransactionManagerTests.this.userSelection)))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(ReactiveNeo4jTransactionManagerTests.this.driver).session(eq(ReactiveSession.class),
					any(SessionConfig.class));

			verify(ReactiveNeo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
			verify(ReactiveNeo4jTransactionManagerTests.this.session).close();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction).rollback();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction, never()).commit();
		}

		@Test
		void shouldParticipateInOngoingTransaction() {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager
				.with(ReactiveNeo4jTransactionManagerTests.this.driver)
				.withDatabaseSelectionProvider(
						() -> Mono.just(ReactiveNeo4jTransactionManagerTests.this.databaseSelection))
				.build();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator.execute(outerStatus -> {
				assertThat(outerStatus.isNewTransaction()).isTrue();
				outerStatus.setRollbackOnly();
				return transactionalOperator.execute(innerStatus -> {
					assertThat(innerStatus.isNewTransaction()).isFalse();
					return ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
							ReactiveNeo4jTransactionManagerTests.this.driver,
							ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
							ReactiveNeo4jTransactionManagerTests.this.userSelection);
				})
					.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
							ReactiveNeo4jTransactionManagerTests.this.driver,
							ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
							ReactiveNeo4jTransactionManagerTests.this.userSelection));
			}).as(StepVerifier::create).expectNextCount(1L).verifyComplete();

			verify(ReactiveNeo4jTransactionManagerTests.this.driver).session(eq(ReactiveSession.class),
					any(SessionConfig.class));

			verify(ReactiveNeo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
			verify(ReactiveNeo4jTransactionManagerTests.this.session).close();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction).rollback();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction, never()).commit();
		}

		@Test
		void usesBookmarksCorrectly() throws Exception {

			ReactiveNeo4jTransactionManager txManager = ReactiveNeo4jTransactionManager
				.with(ReactiveNeo4jTransactionManagerTests.this.driver)
				.withDatabaseSelectionProvider(
						() -> Mono.just(ReactiveNeo4jTransactionManagerTests.this.databaseSelection))
				.build();

			AssertableBookmarkManager bookmarkManager = new AssertableBookmarkManager();
			injectBookmarkManager(txManager, bookmarkManager);

			Set<Bookmark> bookmark = Set.of(new BookmarkForTesting("blubb"));
			given(ReactiveNeo4jTransactionManagerTests.this.session.lastBookmarks()).willReturn(bookmark);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(txManager);

			transactionalOperator
				.execute(transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction()
					.doOnNext(tsm -> assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver))
						.isTrue())
					.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
							ReactiveNeo4jTransactionManagerTests.this.driver,
							ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
							ReactiveNeo4jTransactionManagerTests.this.userSelection)))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(ReactiveNeo4jTransactionManagerTests.this.driver).session(eq(ReactiveSession.class),
					any(SessionConfig.class));
			verify(ReactiveNeo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
			assertThat(bookmarkManager.getBookmarksCalled).isTrue();
			verify(ReactiveNeo4jTransactionManagerTests.this.session).close();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction).commit();
			assertThat(bookmarkManager.updateBookmarksCalled).containsEntry(bookmark, true);
		}

		private void injectBookmarkManager(ReactiveNeo4jTransactionManager txManager, Neo4jBookmarkManager value)
				throws NoSuchFieldException, IllegalAccessException {
			Field bookmarkManager = ReactiveNeo4jTransactionManager.class.getDeclaredField("bookmarkManager");
			bookmarkManager.setAccessible(true);
			bookmarkManager.set(txManager, new BookmarkManagerReference(Neo4jBookmarkManager::createReactive, value));
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
					.doOnNext(tsm -> assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver))
						.isFalse())
					.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
							ReactiveNeo4jTransactionManagerTests.this.driver,
							ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
							ReactiveNeo4jTransactionManagerTests.this.userSelection))
					.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
						.doOnNext(tsm -> assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver))
							.isTrue())))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(ReactiveNeo4jTransactionManagerTests.this.driver).session(eq(ReactiveSession.class),
					any(SessionConfig.class));

			verify(ReactiveNeo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
			verify(ReactiveNeo4jTransactionManagerTests.this.session).close();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction).commit();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction, never()).rollback();
		}

		@Test
		void shouldSynchronizeWithExternalWithRollback() {

			R2dbcTransactionManager t = new R2dbcTransactionManager(
					new H2ConnectionFactory(H2ConnectionConfiguration.builder().inMemory("test").build()));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(t);

			transactionalOperator.execute(
					transactionStatus -> TransactionSynchronizationManager.forCurrentTransaction().doOnNext(tsm -> {
						assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver)).isFalse();
						transactionStatus.setRollbackOnly();
					})
						.then(ReactiveNeo4jTransactionManager.retrieveReactiveTransaction(
								ReactiveNeo4jTransactionManagerTests.this.driver,
								ReactiveNeo4jTransactionManagerTests.this.databaseSelection,
								ReactiveNeo4jTransactionManagerTests.this.userSelection))
						.flatMap(ignoredNativeTx -> TransactionSynchronizationManager.forCurrentTransaction()
							.doOnNext(
									tsm -> assertThat(tsm.hasResource(ReactiveNeo4jTransactionManagerTests.this.driver))
										.isTrue())))
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

			verify(ReactiveNeo4jTransactionManagerTests.this.driver).session(eq(ReactiveSession.class),
					any(SessionConfig.class));

			verify(ReactiveNeo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
			verify(ReactiveNeo4jTransactionManagerTests.this.session).close();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction).rollback();
			verify(ReactiveNeo4jTransactionManagerTests.this.transaction, never()).commit();
		}

	}

}
