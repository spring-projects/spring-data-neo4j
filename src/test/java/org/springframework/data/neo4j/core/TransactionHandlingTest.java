/*
 * Copyright 2011-2021 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.QueryRunner;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxTransaction;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Ensure correct behaviour of both imperative and reactive clients in and outside Springs transaction management.
 *
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class TransactionHandlingTest {

	@Mock private Driver driver;

	@Mock private Session session;

	@Mock private TypeSystem typeSystem;

	@BeforeEach
	void prepareMocks() {

		when(driver.defaultTypeSystem()).thenReturn(typeSystem);
	}

	@AfterEach
	void verifyTypeSystemOnSession() {

		verify(driver).defaultTypeSystem();
	}

	@Nested
	class Neo4jClientTest {

		@Mock private Transaction transaction;

		@Nested
		class AutoCloseableQueryRunnerHandlerTest {

			@Test
			void shouldCallCloseOnSession() {

				ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

				when(driver.session(any(SessionConfig.class))).thenReturn(session);

				// Make template acquire session
				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(Neo4jClient.with(driver));
				try (QueryRunner s = neo4jClient.getQueryRunner(DatabaseSelection.byName("aDatabase"))) {
					s.run("MATCH (n) RETURN n");
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				verify(driver).session(configArgumentCaptor.capture());
				SessionConfig sessionConfig = configArgumentCaptor.getValue();
				assertThat(sessionConfig.database()).isPresent().contains("aDatabase");

				verify(session).run(any(String.class));
				verify(session).lastBookmark();
				verify(session).close();

				verifyNoMoreInteractions(driver, session, transaction);
			}

			@Test
			void shouldNotInvokeCloseOnTransaction() {

				AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

				when(driver.session(any(SessionConfig.class))).thenReturn(session);
				when(session.isOpen()).thenReturn(true);
				when(session.beginTransaction(any(TransactionConfig.class))).thenReturn(transaction);
				// Mock closing of the transaction
				doAnswer(invocation -> {
					transactionIsOpen.set(false);
					return null;
				}).when(transaction).close();
				when(transaction.isOpen()).thenAnswer(invocation -> transactionIsOpen.get());

				Neo4jTransactionManager txManager = new Neo4jTransactionManager(driver);
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);

				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(Neo4jClient.with(driver));
				txTemplate.execute(tx -> {
					try (QueryRunner s = neo4jClient.getQueryRunner(DatabaseSelection.undecided())) {
						s.run("MATCH (n) RETURN n");
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					return null;
				});

				verify(transaction, times(2)).isOpen();
				verify(transaction).run(anyString());
				// Called by the transaction manager
				verify(transaction).commit();
				verify(transaction).close();
				verify(session).isOpen();
				verify(session).lastBookmark();
				verify(session).close();
				verifyNoMoreInteractions(driver, session, transaction);
			}
		}
	}

	@Nested
	class ReactiveNeo4jClientTest {

		@Mock private RxSession session;

		@Mock private RxTransaction transaction;

		@Test
		void shouldNotOpenTransactionsWithoutSubscription() {
			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(ReactiveNeo4jClient.with(driver));
			neo4jClient.query("RETURN 1").in("aDatabase").fetch().one();

			verify(driver, never()).rxSession(any(SessionConfig.class));
			verifyNoMoreInteractions(driver, session);
		}

		@Test
		void shouldCloseUnmanagedSessionOnComplete() {

			when(driver.rxSession(any(SessionConfig.class))).thenReturn(session);
			when(session.close()).thenReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(ReactiveNeo4jClient.with(driver));

			Mono<String> sequence = neo4jClient.doInQueryRunnerForMono(Mono.just(DatabaseSelection.byName("aDatabase")), Mono.just(UserSelection.connectedUser()), tx -> Mono.just("1"));

			StepVerifier.create(sequence).expectNext("1").verifyComplete();

			verify(driver).rxSession(any(SessionConfig.class));
			verify(session).lastBookmark();
			verify(session).close();
			verifyNoMoreInteractions(driver, session, transaction);
		}

		@Test
		void shouldCloseUnmanagedSessionOnError() {

			when(driver.rxSession(any(SessionConfig.class))).thenReturn(session);
			when(session.close()).thenReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(ReactiveNeo4jClient.with(driver));

			Mono<String> sequence = neo4jClient.doInQueryRunnerForMono(Mono.just(DatabaseSelection.byName("aDatabase")), Mono.just(UserSelection.connectedUser()), tx -> Mono.error(new SomeException()));

			StepVerifier.create(sequence).expectError(SomeException.class).verify();

			verify(driver).rxSession(any(SessionConfig.class));
			verify(session).lastBookmark();
			verify(session).close();
			verifyNoMoreInteractions(driver, session, transaction);
		}
	}

	private static class SomeException extends RuntimeException {}
}
