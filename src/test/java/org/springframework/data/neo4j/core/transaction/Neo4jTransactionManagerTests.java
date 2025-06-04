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
package org.springframework.data.neo4j.core.transaction;

import java.lang.reflect.Field;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.transaction.Status;
import jakarta.transaction.UserTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.support.BookmarkManagerReference;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jTransactionManagerTests {

	private final DatabaseSelection databaseSelection = DatabaseSelection.byName("aDatabase");

	private final UserSelection userSelection = UserSelection.connectedUser();

	@Mock
	private Driver driver;

	@Mock
	private Session session;

	@Mock
	private TypeSystem typeSystem;

	@Mock
	private Transaction transaction;

	@Mock
	private Result statementResult;

	@Mock
	private UserTransaction userTransaction;

	@Mock
	private ResultSummary resultSummary;

	@Test
	void shouldWorkWithoutSynchronizations() {
		Transaction optionalTransaction = Neo4jTransactionManager.retrieveTransaction(this.driver,
				this.databaseSelection, this.userSelection);

		assertThat(optionalTransaction).isNull();

		verifyNoInteractions(this.driver, this.session, this.transaction);
	}

	@Test
	void triggerCommitCorrectly() {

		given(this.driver.session(any(SessionConfig.class))).willReturn(this.session);
		given(this.session.beginTransaction(any(TransactionConfig.class))).willReturn(this.transaction);
		given(this.transaction.run(anyString(), anyMap())).willReturn(this.statementResult);
		given(this.session.isOpen()).willReturn(true);
		given(this.statementResult.consume()).willReturn(this.resultSummary);
		given(this.transaction.isOpen()).willReturn(true, false);

		Neo4jTransactionManager txManager = new Neo4jTransactionManager(this.driver);
		TransactionStatus txStatus = txManager.getTransaction(new DefaultTransactionDefinition());

		Neo4jClient client = Neo4jClient.create(this.driver);
		client.query("RETURN 1").run();

		txManager.commit(txStatus);

		verify(this.driver).session(any(SessionConfig.class));

		verify(this.session).isOpen();
		verify(this.session).beginTransaction(any(TransactionConfig.class));

		verify(this.transaction, times(2)).isOpen();
		verify(this.transaction).commit();
		verify(this.transaction).close();

		verify(this.session).close();
	}

	@Test
	void usesBookmarksCorrectly() throws Exception {

		given(this.driver.session(any(SessionConfig.class))).willReturn(this.session);
		given(this.session.beginTransaction(any(TransactionConfig.class))).willReturn(this.transaction);
		Set<Bookmark> bookmark = Set.of(new BookmarkForTesting("blubb"));
		given(this.session.lastBookmarks()).willReturn(bookmark);
		given(this.transaction.run(anyString(), anyMap())).willReturn(this.statementResult);
		given(this.session.isOpen()).willReturn(true);
		given(this.transaction.isOpen()).willReturn(true, false);
		given(this.statementResult.consume()).willReturn(this.resultSummary);

		Neo4jTransactionManager txManager = spy(new Neo4jTransactionManager(this.driver));
		AssertableBookmarkManager bookmarkManager = new AssertableBookmarkManager();
		injectBookmarkManager(txManager, bookmarkManager);

		TransactionStatus txStatus = txManager.getTransaction(new DefaultTransactionDefinition());

		Neo4jClient client = Neo4jClient.create(this.driver);
		client.query("RETURN 1").run();

		txManager.commit(txStatus);

		verify(txManager).doBegin(any(), any(TransactionDefinition.class));
		assertThat(bookmarkManager.getBookmarksCalled).isTrue();
		verify(txManager).doCommit(any(DefaultTransactionStatus.class));
		assertThat(bookmarkManager.updateBookmarksCalled).containsEntry(bookmark, true);
	}

	private void injectBookmarkManager(Neo4jTransactionManager txManager, Neo4jBookmarkManager value)
			throws NoSuchFieldException, IllegalAccessException {
		Field bookmarkManager = Neo4jTransactionManager.class.getDeclaredField("bookmarkManager");
		bookmarkManager.setAccessible(true);
		bookmarkManager.set(txManager, new BookmarkManagerReference(Neo4jBookmarkManager::create, value));
	}

	@Nested
	class TransactionParticipation {

		@BeforeEach
		void setUp() {

			AtomicBoolean sessionIsOpen = new AtomicBoolean(true);
			AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

			given(Neo4jTransactionManagerTests.this.driver.session(any(SessionConfig.class)))
				.willReturn(Neo4jTransactionManagerTests.this.session);

			given(Neo4jTransactionManagerTests.this.session.beginTransaction(any(TransactionConfig.class)))
				.willReturn(Neo4jTransactionManagerTests.this.transaction);

			BDDMockito.doAnswer(invocation -> {
				sessionIsOpen.set(false);
				return null;
			}).when(Neo4jTransactionManagerTests.this.session).close();
			given(Neo4jTransactionManagerTests.this.session.isOpen()).willAnswer(invocation -> sessionIsOpen.get());

			BDDMockito.doAnswer(invocation -> {
				transactionIsOpen.set(false);
				return null;
			}).when(Neo4jTransactionManagerTests.this.transaction).close();
			given(Neo4jTransactionManagerTests.this.transaction.isOpen())
				.willAnswer(invocation -> transactionIsOpen.get());
		}

		@AfterEach
		void verifyTransactionSynchronizationManagerState() {

			assertThat(TransactionSynchronizationManager.getResourceMap().isEmpty()).isTrue();
			assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
			assertThat(TransactionSynchronizationManager.getCurrentTransactionName()).isNull();
			assertThat(TransactionSynchronizationManager.isCurrentTransactionReadOnly()).isFalse();
			assertThat(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).isNull();
			assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
		}

		@Nested
		class BasedOnNeo4jTransactions {

			@Test
			void shouldUseTxFromNeo4jTxManager() {

				Neo4jTransactionManager txManager = Neo4jTransactionManager
					.with(Neo4jTransactionManagerTests.this.driver)
					.withDatabaseSelectionProvider(() -> Neo4jTransactionManagerTests.this.databaseSelection)
					.build();
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(
								TransactionSynchronizationManager.hasResource(Neo4jTransactionManagerTests.this.driver))
							.isTrue();

						Transaction optionalTransaction = Neo4jTransactionManager.retrieveTransaction(
								Neo4jTransactionManagerTests.this.driver,
								Neo4jTransactionManagerTests.this.databaseSelection,
								Neo4jTransactionManagerTests.this.userSelection);
						assertThat(optionalTransaction).isNotNull();

						transactionStatus.setRollbackOnly();
					}
				});

				verify(Neo4jTransactionManagerTests.this.driver).session(any(SessionConfig.class));

				verify(Neo4jTransactionManagerTests.this.session).isOpen();
				verify(Neo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
				verify(Neo4jTransactionManagerTests.this.session).close();

				verify(Neo4jTransactionManagerTests.this.transaction, times(2)).isOpen();
				verify(Neo4jTransactionManagerTests.this.transaction).rollback();
				verify(Neo4jTransactionManagerTests.this.transaction).close();
			}

			@Test
			void shouldParticipateInOngoingTransaction() {

				Neo4jTransactionManager txManager = Neo4jTransactionManager
					.with(Neo4jTransactionManagerTests.this.driver)
					.withDatabaseSelectionProvider(() -> Neo4jTransactionManagerTests.this.databaseSelection)
					.build();
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus outerStatus) {

						Transaction outerNativeTransaction = Neo4jTransactionManager.retrieveTransaction(
								Neo4jTransactionManagerTests.this.driver,
								Neo4jTransactionManagerTests.this.databaseSelection,
								Neo4jTransactionManagerTests.this.userSelection);
						assertThat(outerNativeTransaction).isNotNull();
						assertThat(outerStatus.isNewTransaction()).isTrue();

						txTemplate.execute(new TransactionCallbackWithoutResult() {

							@Override
							protected void doInTransactionWithoutResult(TransactionStatus innerStatus) {

								assertThat(innerStatus.isNewTransaction()).isFalse();

								Transaction innerNativeTransaction = Neo4jTransactionManager.retrieveTransaction(
										Neo4jTransactionManagerTests.this.driver,
										Neo4jTransactionManagerTests.this.databaseSelection,
										Neo4jTransactionManagerTests.this.userSelection);
								assertThat(innerNativeTransaction).isNotNull();
							}
						});

						outerStatus.setRollbackOnly();
					}
				});

				verify(Neo4jTransactionManagerTests.this.driver).session(any(SessionConfig.class));

				verify(Neo4jTransactionManagerTests.this.session).isOpen();
				verify(Neo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
				verify(Neo4jTransactionManagerTests.this.session).close();

				verify(Neo4jTransactionManagerTests.this.transaction, times(2)).isOpen();
				verify(Neo4jTransactionManagerTests.this.transaction).rollback();
				verify(Neo4jTransactionManagerTests.this.transaction).close();
			}

		}

		@Nested
		class BasedOnJtaTransactions {

			@Test
			void shouldParticipateInOngoingTransactionWithCommit() throws Exception {

				given(Neo4jTransactionManagerTests.this.userTransaction.getStatus())
					.willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

				JtaTransactionManager txManager = new JtaTransactionManager(
						Neo4jTransactionManagerTests.this.userTransaction);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(
								TransactionSynchronizationManager.hasResource(Neo4jTransactionManagerTests.this.driver))
							.isFalse();

						Transaction nativeTransaction = Neo4jTransactionManager.retrieveTransaction(
								Neo4jTransactionManagerTests.this.driver,
								Neo4jTransactionManagerTests.this.databaseSelection,
								Neo4jTransactionManagerTests.this.userSelection);

						assertThat(nativeTransaction).isNotNull();
						assertThat(
								TransactionSynchronizationManager.hasResource(Neo4jTransactionManagerTests.this.driver))
							.isTrue();
					}
				});

				verify(Neo4jTransactionManagerTests.this.userTransaction).begin();

				verify(Neo4jTransactionManagerTests.this.driver).session(any(SessionConfig.class));

				verify(Neo4jTransactionManagerTests.this.session, times(2)).isOpen();
				verify(Neo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
				verify(Neo4jTransactionManagerTests.this.session).close();

				verify(Neo4jTransactionManagerTests.this.transaction, times(3)).isOpen();
				verify(Neo4jTransactionManagerTests.this.transaction).commit();
				verify(Neo4jTransactionManagerTests.this.transaction).close();
			}

			@Test
			void shouldParticipateInOngoingTransactionWithRollback() throws Exception {

				given(Neo4jTransactionManagerTests.this.userTransaction.getStatus())
					.willReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE, Status.STATUS_ACTIVE);

				JtaTransactionManager txManager = new JtaTransactionManager(
						Neo4jTransactionManagerTests.this.userTransaction);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(
								TransactionSynchronizationManager.hasResource(Neo4jTransactionManagerTests.this.driver))
							.isFalse();

						Transaction nativeTransaction = Neo4jTransactionManager.retrieveTransaction(
								Neo4jTransactionManagerTests.this.driver,
								Neo4jTransactionManagerTests.this.databaseSelection,
								Neo4jTransactionManagerTests.this.userSelection);

						assertThat(nativeTransaction).isNotNull();
						assertThat(
								TransactionSynchronizationManager.hasResource(Neo4jTransactionManagerTests.this.driver))
							.isTrue();

						transactionStatus.setRollbackOnly();
					}
				});

				verify(Neo4jTransactionManagerTests.this.userTransaction).begin();
				verify(Neo4jTransactionManagerTests.this.userTransaction).rollback();

				verify(Neo4jTransactionManagerTests.this.driver).session(any(SessionConfig.class));

				verify(Neo4jTransactionManagerTests.this.session, times(2)).isOpen();
				verify(Neo4jTransactionManagerTests.this.session).beginTransaction(any(TransactionConfig.class));
				verify(Neo4jTransactionManagerTests.this.session).close();

				verify(Neo4jTransactionManagerTests.this.transaction, times(3)).isOpen();
				verify(Neo4jTransactionManagerTests.this.transaction).rollback();
				verify(Neo4jTransactionManagerTests.this.transaction).close();
			}

		}

	}

}
