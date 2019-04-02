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
package org.springframework.data.neo4j.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementRunner;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.jta.JtaTransactionManager;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Neo4jTransactionUtilsTest {

	@Mock
	private Driver driver;
	@Mock
	private Session session;
	@Mock
	private Transaction transaction;
	@Mock
	UserTransaction userTransaction;

	@BeforeEach
	void setUp() {

		AtomicBoolean sessionIsOpen = new AtomicBoolean(true);
		AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

		when(driver.session()).thenReturn(session);
		when(driver.session(any(Consumer.class))).thenReturn(session);

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

	@Test
	void shouldWorkWithoutSynchronizations() {
		@SuppressWarnings({ "unused" })
		StatementRunner statementRunner = Neo4jTransactionUtils.retrieveTransactionalStatementRunner(driver);

		verify(driver).session();
		verifyNoMoreInteractions(driver, session, transaction);
	}

	@Nested
	class BasedOnNeo4jTransactions {
		@Test
		void shouldOpenNewTransaction() {

			Neo4jTransactionManager txManager = new Neo4jTransactionManager(driver);
			TransactionTemplate txTemplate = new TransactionTemplate(txManager);

			txTemplate.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
					assertThat(transactionStatus.isNewTransaction()).isTrue();
					assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();

					@SuppressWarnings({ "unused" })
					StatementRunner statementRunner = Neo4jTransactionUtils
						.retrieveTransactionalStatementRunner(driver);

					transactionStatus.setRollbackOnly();
				}
			});

			verify(driver).session(any(Consumer.class));

			verify(session).isOpen();
			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();

			verify(transaction, times(2)).isOpen();
			verify(transaction).failure();
			verify(transaction).close();
		}

		@Test
		void shouldParticipateInOngoingTransaction() {

			Neo4jTransactionManager txManager = new Neo4jTransactionManager(driver);
			TransactionTemplate txTemplate = new TransactionTemplate(txManager);

			txTemplate.execute(new TransactionCallbackWithoutResult() {

				@Override
				protected void doInTransactionWithoutResult(TransactionStatus outerStatus) {

					@SuppressWarnings({ "unused" })
					StatementRunner outerStatementRunner = Neo4jTransactionUtils
						.retrieveTransactionalStatementRunner(driver);
					assertThat(outerStatus.isNewTransaction()).isTrue();

					txTemplate.execute(new TransactionCallbackWithoutResult() {

						@Override
						protected void doInTransactionWithoutResult(TransactionStatus innerStatus) {

							assertThat(innerStatus.isNewTransaction()).isFalse();

							@SuppressWarnings({ "unused" })
							StatementRunner innerStatementRunner = Neo4jTransactionUtils
								.retrieveTransactionalStatementRunner(driver);
						}
					});

					outerStatus.setRollbackOnly();
				}
			});

			verify(driver).session(any(Consumer.class));

			verify(session).isOpen();
			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();

			verify(transaction, times(2)).isOpen();
			verify(transaction).failure();
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

					@SuppressWarnings({ "unused" })
					StatementRunner statementRunner = Neo4jTransactionUtils
						.retrieveTransactionalStatementRunner(driver);

					assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();
				}
			});

			verify(userTransaction).begin();

			verify(driver).session(any(Consumer.class));

			verify(session, times(2)).isOpen();
			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();

			verify(transaction, times(3)).isOpen();
			verify(transaction).success();
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

					@SuppressWarnings({ "unused" })
					StatementRunner statementRunner = Neo4jTransactionUtils
						.retrieveTransactionalStatementRunner(driver);

					assertThat(TransactionSynchronizationManager.hasResource(driver)).isTrue();

					transactionStatus.setRollbackOnly();
				}
			});

			verify(userTransaction).begin();
			verify(userTransaction).rollback();

			verify(driver).session(any(Consumer.class));

			verify(session, times(2)).isOpen();
			verify(session).beginTransaction(any(TransactionConfig.class));
			verify(session).close();

			verify(transaction, times(3)).isOpen();
			verify(transaction).failure();
			verify(transaction).close();
		}
	}

	@AfterEach
	void verifyTransactionSynchronizationManagerState() {

		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionName());
		assertFalse(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertNull(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
	}
}
