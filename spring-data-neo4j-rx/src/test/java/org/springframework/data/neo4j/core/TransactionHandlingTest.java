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
package org.springframework.data.neo4j.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.TransactionConfig;
import org.neo4j.driver.internal.SessionParameters;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.reactive.RxStatementRunner;
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

	@Mock
	private Driver driver;

	@Mock
	private Session session;

	@Mock
	private TypeSystem typeSystem;

	@Mock
	private SessionParameters.Template sessionParametersTemplate;

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

		@Mock
		private Transaction transaction;

		@Nested
		class AutoCloseableStatementRunnerHandlerTest {

			@Test
			public void shouldCallCloseOnSession() {

				ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);

				when(driver.session(any(Consumer.class))).thenReturn(session);
				when(sessionParametersTemplate.withDatabase(anyString())).thenReturn(sessionParametersTemplate);
				when(sessionParametersTemplate.withBookmarks(anyList())).thenReturn(sessionParametersTemplate);
				when(sessionParametersTemplate.withDefaultAccessMode(any(AccessMode.class)))
					.thenReturn(sessionParametersTemplate);

				// Make template acquire session
				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(driver);
				try (DefaultNeo4jClient.AutoCloseableStatementRunner s = neo4jClient.getStatementRunner("aDatabase")) {
					s.run("MATCH (n) RETURN n");
				}

				verify(driver).session(consumerCaptor.capture());
				consumerCaptor.getValue().accept(sessionParametersTemplate);
				verify(sessionParametersTemplate).withDatabase("aDatabase");

				verify(session).run(any(String.class));
				verify(session).close();

				verifyNoMoreInteractions(driver, sessionParametersTemplate, session, transaction);
			}

			@Test
			public void shouldNotInvokeCloseOnTransaction() {

				AtomicBoolean transactionIsOpen = new AtomicBoolean(true);

				when(driver.session(any(Consumer.class))).thenReturn(session);
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

				DefaultNeo4jClient neo4jClient = new DefaultNeo4jClient(driver);
				txTemplate.execute(tx -> {
					try (DefaultNeo4jClient.AutoCloseableStatementRunner s = neo4jClient.getStatementRunner(null)) {
						s.run("MATCH (n) RETURN n");
					}
					return null;
				});

				verify(transaction, times(2)).isOpen();
				verify(transaction).run(anyString());
				// Called by the transaction manager
				verify(transaction).success();
				verify(transaction).close();
				verify(session).isOpen();
				verify(session).close();
				verifyNoMoreInteractions(driver, session, transaction);
			}
		}
	}

	@Nested
	class ReactiveNeo4jClientTest {

		@Mock
		private RxSession session;

		@Mock
		private RxTransaction transaction;

		@Test
		public void shouldNotOpenTransactionsWithoutSubscription() {

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(driver);

			@SuppressWarnings("unused")
			Mono<RxStatementRunner> runner = neo4jClient.getStatementRunner("aDatabase");

			verify(driver, never()).rxSession(any(Consumer.class));
			verifyZeroInteractions(driver, session);
		}

		// TODO The same like shouldNotOpenTransactionsWithoutSubscription but with managed transactions

		@Test
		public void shouldFallbackToImplicitTransaction() {

			when(driver.rxSession(any(Consumer.class))).thenReturn(session);
			when(session.beginTransaction()).thenReturn(Mono.just(transaction));

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(driver);

			@SuppressWarnings("unused")
			Mono<RxStatementRunner> runner = neo4jClient.getStatementRunner("aDatabase");

			StepVerifier.create(runner)
				.expectNextCount(1L)
				.verifyComplete();

			verify(driver).rxSession(any(Consumer.class));
			verify(session).close();
			verifyZeroInteractions(driver, session, transaction);
		}

		@Test
		public void shouldCloseUnmanagedSessionOnComplete() {

			when(driver.rxSession(any(Consumer.class))).thenReturn(session);
			when(session.beginTransaction()).thenReturn(Mono.just(transaction));
			when(transaction.commit()).thenReturn(Mono.empty());
			when(session.close()).thenReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(driver);

			Mono<String> sequence = Mono
				.usingWhen(neo4jClient.getStatementRunner("aDatabase"), r -> Mono.just("A node"),
					DefaultReactiveNeo4jClient::asyncComplete, DefaultReactiveNeo4jClient::asyncError);

			StepVerifier.create(sequence)
				.expectNext("A node")
				.verifyComplete();

			verify(driver).rxSession(any(Consumer.class));
			verify(transaction).commit();
			verify(session).close();
			verifyZeroInteractions(driver, session, transaction);
		}

		@Test
		public void shouldCloseUnmanagedSessionOnError() {

			when(driver.rxSession(any(Consumer.class))).thenReturn(session);
			when(session.beginTransaction()).thenReturn(Mono.just(transaction));
			when(transaction.rollback()).thenReturn(Mono.empty());
			when(session.close()).thenReturn(Mono.empty());

			DefaultReactiveNeo4jClient neo4jClient = new DefaultReactiveNeo4jClient(driver);

			Mono<String> sequence = Mono
				.usingWhen(neo4jClient.getStatementRunner("aDatabase"), r -> Mono.error(new RuntimeException()),
					DefaultReactiveNeo4jClient::asyncComplete, DefaultReactiveNeo4jClient::asyncError);

			StepVerifier.create(sequence)
				.expectError()
				.verify();

			verify(driver).rxSession(any(Consumer.class));
			verify(transaction).rollback();
			verify(session).close();
			verifyZeroInteractions(driver, session, transaction);
		}
	}
}
