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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jTransactionManagerTest {

	private final DatabaseSelection databaseSelection = DatabaseSelection.byName("aDatabase");
	private final UserSelection userSelection = UserSelection.connectedUser();

	@Mock private Driver driver;
	@Mock private Session session;
	@Mock private TypeSystem typeSystem;
	@Mock private Transaction transaction;
	@Mock private Result statementResult;
	@Mock private UserTransaction userTransaction;
	@Mock private ResultSummary resultSummary;

	@Test
	void shouldWorkWithoutSynchronizations() {
		Transaction optionalTransaction = Neo4jTransactionManager.retrieveTransaction(driver, databaseSelection,
				userSelection);

		assertThat(optionalTransaction).isNull();

		verifyNoInteractions(driver, session, transaction);
	}

	@Test
	void triggerCommitCorrectly() {

		when(driver.defaultTypeSystem()).thenReturn(typeSystem);
		when(driver.session(any(SessionConfig.class))).thenReturn(session);
		when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(transaction);
		when(transaction.run(anyString(), anyMap())).thenReturn(statementResult);
		when(session.isOpen()).thenReturn(true);
		when(statementResult.consume()).thenReturn(resultSummary);
		when(transaction.isOpen()).thenReturn(true, false);

		Neo4jTransactionManager txManager = new Neo4jTransactionManager(driver);
		TransactionStatus txStatus = txManager.getTransaction(new DefaultTransactionDefinition());

		Neo4jClient client = Neo4jClient.create(driver);
		client.query("RETURN 1").run();

		txManager.commit(txStatus);

		verify(driver).session(any(SessionConfig.class));

		verify(session).isOpen();
		verify(session).beginTransaction(any(TransactionConfig.class));

		verify(transaction, times(2)).isOpen();
		verify(transaction).commit();
		verify(transaction).close();

		verify(session).close();
	}

	@Test
	void usesBookmarksCorrectly() throws Exception {

		when(driver.defaultTypeSystem()).thenReturn(typeSystem);
		when(driver.session(any(SessionConfig.class))).thenReturn(session);
		when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(transaction);
		Set<Bookmark> bookmark = Set.of(new BookmarkForTesting("blubb"));
		when(session.lastBookmarks()).thenReturn(bookmark);
		when(transaction.run(anyString(), anyMap())).thenReturn(statementResult);
		when(session.isOpen()).thenReturn(true);
		when(transaction.isOpen()).thenReturn(true, false);
		when(statementResult.consume()).thenReturn(resultSummary);

		Neo4jTransactionManager txManager = spy(new Neo4jTransactionManager(driver));
		AssertableBookmarkManager bookmarkManager = new AssertableBookmarkManager();
		injectBookmarkManager(txManager, bookmarkManager);

		TransactionStatus txStatus = txManager.getTransaction(new DefaultTransactionDefinition());

		Neo4jClient client = Neo4jClient.create(driver);
		client.query("RETURN 1").run();

		txManager.commit(txStatus);

		verify(txManager).doBegin(any(), any(TransactionDefinition.class));
		assertThat(bookmarkManager.getBookmarksCalled).isTrue();
		verify(txManager).doCommit(any(DefaultTransactionStatus.class));
		assertThat(bookmarkManager.updateBookmarksCalled)
				.containsEntry(bookmark, true);
	}

	private void injectBookmarkManager(Neo4jTransactionManager txManager, Neo4jBookmarkManager value)
			throws NoSuchFieldException, IllegalAccessException {
		Field bookmarkManager = Neo4jTransactionManager.class.getDeclaredField("bookmarkManager");
		bookmarkManager.setAccessible(true);
		bookmarkManager.set(txManager, value);
	}

	@Nested
	class TransactionParticipation {

		@BeforeEach
		void setUp() {

			AtomicBoolean sessionIsOpen = new AtomicBoolean(true);
			AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

			when(driver.session(any(SessionConfig.class))).thenReturn(session);

			when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(transaction);
			doAnswer(invocation -> {
				sessionIsOpen.set(false);
				return null;
			}).when(session).close();
			when(session.isOpen()).thenAnswer(invocation -> sessionIsOpen.get());

			doAnswer(invocation -> {
				transactionIsOpen.set(false);
				return null;
			}).when(transaction).close();
			when(transaction.isOpen()).thenAnswer(invocation -> transactionIsOpen.get());
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

				Neo4jTransactionManager txManager = Neo4jTransactionManager.with(driver)
						.withDatabaseSelectionProvider(() -> databaseSelection)
						.build();
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();

						Transaction optionalTransaction = Neo4jTransactionManager.retrieveTransaction(driver,
								databaseSelection,
								userSelection);
						assertThat(optionalTransaction).isNotNull();

						transactionStatus.setRollbackOnly();
					}
				});

				verify(driver).session(any(SessionConfig.class));

				verify(session).isOpen();
				verify(session).beginTransaction(any(TransactionConfig.class));
				verify(session).close();

				verify(transaction, times(2)).isOpen();
				verify(transaction).rollback();
				verify(transaction).close();
			}

			@Test
			void shouldParticipateInOngoingTransaction() {

				Neo4jTransactionManager txManager = Neo4jTransactionManager.with(driver)
						.withDatabaseSelectionProvider(() -> databaseSelection)
						.build();
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus outerStatus) {

						Transaction outerNativeTransaction = Neo4jTransactionManager.retrieveTransaction(driver,
								databaseSelection,
								userSelection);
						assertThat(outerNativeTransaction).isNotNull();
						assertThat(outerStatus.isNewTransaction()).isTrue();

						txTemplate.execute(new TransactionCallbackWithoutResult() {

							@Override
							protected void doInTransactionWithoutResult(TransactionStatus innerStatus) {

								assertThat(innerStatus.isNewTransaction()).isFalse();

								Transaction innerNativeTransaction = Neo4jTransactionManager.retrieveTransaction(driver,
										databaseSelection,
										userSelection);
								assertThat(innerNativeTransaction).isNotNull();
							}
						});

						outerStatus.setRollbackOnly();
					}
				});

				verify(driver).session(any(SessionConfig.class));

				verify(session).isOpen();
				verify(session).beginTransaction(any(TransactionConfig.class));
				verify(session).close();

				verify(transaction, times(2)).isOpen();
				verify(transaction).rollback();
				verify(transaction).close();
			}

		}

		@Nested
		class BasedOnJtaTransactions {

			@Test
			void shouldParticipateInOngoingTransactionWithCommit() throws Exception {

				when(userTransaction.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE,
						Status.STATUS_ACTIVE);

				JtaTransactionManager txManager = new JtaTransactionManager(userTransaction);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(TransactionSynchronizationManager.hasResource(driver)).isFalse();

						Transaction nativeTransaction = Neo4jTransactionManager.retrieveTransaction(driver,
								databaseSelection,
								userSelection);

						assertThat(nativeTransaction).isNotNull();
						assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();
					}
				});

				verify(userTransaction).begin();

				verify(driver).session(any(SessionConfig.class));

				verify(session, times(2)).isOpen();
				verify(session).beginTransaction(any(TransactionConfig.class));
				verify(session).close();

				verify(transaction, times(3)).isOpen();
				verify(transaction).commit();
				verify(transaction).close();
			}

			@Test
			void shouldParticipateInOngoingTransactionWithRollback() throws Exception {

				when(userTransaction.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION, Status.STATUS_ACTIVE,
						Status.STATUS_ACTIVE);

				JtaTransactionManager txManager = new JtaTransactionManager(userTransaction);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				txTemplate.execute(new TransactionCallbackWithoutResult() {

					@Override
					protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

						assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
						assertThat(transactionStatus.isNewTransaction()).isTrue();
						assertThat(TransactionSynchronizationManager.hasResource(driver)).isFalse();

						Transaction nativeTransaction = Neo4jTransactionManager.retrieveTransaction(driver,
								databaseSelection,
								userSelection);

						assertThat(nativeTransaction).isNotNull();
						assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();

						transactionStatus.setRollbackOnly();
					}
				});

				verify(userTransaction).begin();
				verify(userTransaction).rollback();

				verify(driver).session(any(SessionConfig.class));

				verify(session, times(2)).isOpen();
				verify(session).beginTransaction(any(TransactionConfig.class));
				verify(session).close();

				verify(transaction, times(3)).isOpen();
				verify(transaction).rollback();
				verify(transaction).close();
			}
		}
	}
}
