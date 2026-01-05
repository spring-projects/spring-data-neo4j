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
package org.springframework.data.neo4j.core;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Ensure correct behaviour of both imperative and reactive clients in and outside Springs
 * transaction management.
 *
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class TransactionHandlingTests {

	@Mock
	private Driver driver;

	@Mock
	private Session session;

	private static final class SomeException extends RuntimeException {

	}

	@Nested
	class Neo4jClientTest {

		@Mock
		private Transaction transaction;

		@Nested
		class AutoCloseableQueryRunnerHandlerTest {

			@Test
			void shouldCallCloseOnSession() {

				ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

				given(TransactionHandlingTests.this.driver.session(any(SessionConfig.class)))
					.willReturn(TransactionHandlingTests.this.session);

				// Make template acquire session
				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(
						Neo4jClient.with(TransactionHandlingTests.this.driver));
				try (QueryRunner s = neo4jClient.getQueryRunner(DatabaseSelection.byName("aDatabase"))) {
					s.run("MATCH (n) RETURN n");
				}
				catch (Exception ex) {
					throw new RuntimeException(ex);
				}

				verify(TransactionHandlingTests.this.driver).session(configArgumentCaptor.capture());
				SessionConfig sessionConfig = configArgumentCaptor.getValue();
				assertThat(sessionConfig.database()).isPresent().contains("aDatabase");

				verify(TransactionHandlingTests.this.session).run(any(String.class));
				verify(TransactionHandlingTests.this.session).lastBookmarks();
				verify(TransactionHandlingTests.this.session).close();

				verifyNoMoreInteractions(TransactionHandlingTests.this.driver, TransactionHandlingTests.this.session,
						Neo4jClientTest.this.transaction);
			}

			@Test
			void shouldNotInvokeCloseOnTransaction() {

				AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

				given(TransactionHandlingTests.this.driver.session(any(SessionConfig.class)))
					.willReturn(TransactionHandlingTests.this.session);
				given(TransactionHandlingTests.this.session.isOpen()).willReturn(true);
				given(TransactionHandlingTests.this.session.beginTransaction(any(TransactionConfig.class)))
					.willReturn(Neo4jClientTest.this.transaction);
				// Mock closing of the transaction
				BDDMockito.doAnswer(invocation -> {
					transactionIsOpen.set(false);
					return null;
				}).when(Neo4jClientTest.this.transaction).close();
				given(Neo4jClientTest.this.transaction.isOpen()).willAnswer(invocation -> transactionIsOpen.get());

				Neo4jTransactionManager txManager = new Neo4jTransactionManager(TransactionHandlingTests.this.driver);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(
						Neo4jClient.with(TransactionHandlingTests.this.driver));
				txTemplate.execute(tx -> {
					try (QueryRunner s = neo4jClient.getQueryRunner(DatabaseSelection.undecided())) {
						s.run("MATCH (n) RETURN n");
					}
					catch (Exception ex) {
						throw new RuntimeException(ex);
					}
					return null;
				});

				verify(Neo4jClientTest.this.transaction, times(2)).isOpen();
				verify(Neo4jClientTest.this.transaction).run(anyString());
				// Called by the transaction manager
				verify(Neo4jClientTest.this.transaction).commit();
				verify(Neo4jClientTest.this.transaction).close();
				verify(TransactionHandlingTests.this.session).isOpen();
				verify(TransactionHandlingTests.this.session).lastBookmarks();
				verify(TransactionHandlingTests.this.session).close();
				verifyNoMoreInteractions(TransactionHandlingTests.this.driver, TransactionHandlingTests.this.session,
						Neo4jClientTest.this.transaction);
			}

		}

	}

	@Nested
	class ReactiveNeo4jClientTest {

		@Mock
		private ReactiveSession session;

		@Mock
		private ReactiveTransaction transaction;

		@Test
		void shouldNotOpenTransactionsWithoutSubscription() {
			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(
					ReactiveNeo4jClient.with(TransactionHandlingTests.this.driver));
			neo4jClient.query("RETURN 1").in("aDatabase").fetch().one();

			verify(TransactionHandlingTests.this.driver, never()).session(eq(ReactiveSession.class),
					any(SessionConfig.class));
			verifyNoMoreInteractions(TransactionHandlingTests.this.driver, this.session);
		}

		@Test
		void shouldCloseUnmanagedSessionOnComplete() {

			given(TransactionHandlingTests.this.driver.session(eq(ReactiveSession.class), any(SessionConfig.class)))
				.willReturn(this.session);
			given(this.session.close()).willReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(
					ReactiveNeo4jClient.with(TransactionHandlingTests.this.driver));

			Mono<String> sequence = neo4jClient.doInQueryRunnerForMono(Mono.just(DatabaseSelection.byName("aDatabase")),
					Mono.just(UserSelection.connectedUser()), tx -> Mono.just("1"));

			StepVerifier.create(sequence).expectNext("1").verifyComplete();

			verify(TransactionHandlingTests.this.driver).session(eq(ReactiveSession.class), any(SessionConfig.class));
			verify(this.session).lastBookmarks();
			verify(this.session).close();
			verifyNoMoreInteractions(TransactionHandlingTests.this.driver, this.session, this.transaction);
		}

		@Test
		void shouldCloseUnmanagedSessionOnError() {

			given(TransactionHandlingTests.this.driver.session(eq(ReactiveSession.class), any(SessionConfig.class)))
				.willReturn(this.session);
			given(this.session.close()).willReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(
					ReactiveNeo4jClient.with(TransactionHandlingTests.this.driver));

			Mono<String> sequence = neo4jClient.doInQueryRunnerForMono(Mono.just(DatabaseSelection.byName("aDatabase")),
					Mono.just(UserSelection.connectedUser()), tx -> Mono.error(new SomeException()));

			StepVerifier.create(sequence).expectError(SomeException.class).verify();

			verify(TransactionHandlingTests.this.driver).session(eq(ReactiveSession.class), any(SessionConfig.class));
			verify(this.session).lastBookmarks();
			verify(this.session).close();
			verifyNoMoreInteractions(TransactionHandlingTests.this.driver, this.session, this.transaction);
		}

	}

}
