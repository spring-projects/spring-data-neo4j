/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.executables.ExecutableResultStatement;
import org.neo4j.driver.Query;
import org.neo4j.driver.Record;
import org.neo4j.driver.SimpleQueryRunner;
import org.neo4j.driver.async.AsyncQueryRunner;
import org.neo4j.driver.reactivestreams.ReactiveQueryRunner;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.summary.ResultSummary;
import org.reactivestreams.Publisher;
import org.springframework.data.neo4j.core.mapping.IdentitySupport;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.TestIdentitySupport;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveNeo4jClientIT {

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		try (
				Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()
		) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
		}
	}

	@Test // GH-2238
	void clientShouldIntegrateWithCypherDSL(@Autowired TransactionalOperator transactionalOperator,
			@Autowired ReactiveNeo4jClient client,
			@Autowired BookmarkCapture bookmarkCapture) {

		Node namedAnswer = Cypher.node("TheAnswer", Cypher.mapOf("value",
				Cypher.literalOf(23).multiply(Cypher.literalOf(2)).subtract(Cypher.literalOf(4)))).named("n");
		NewReactiveExecutableResultStatement statement = new NewReactiveExecutableResultStatement(namedAnswer);

		AtomicLong vanishedId = new AtomicLong();
		transactionalOperator.execute(transaction -> {
					Flux<Long> inner = client.getQueryRunner()
							.flatMapMany(statement::fetchWith)
							.doOnNext(r -> vanishedId.set(TestIdentitySupport.getInternalId(r.get("n").asNode())))
							.map(record -> record.get("n").get("value").asLong());

					transaction.setRollbackOnly();
					return inner;
				}).as(StepVerifier::create)
				.expectNext(42L)
				.verifyComplete();

		// Make sure we actually interacted with the managed transaction (that had been rolled back)
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			long cnt = session.run("MATCH (n) WHERE id(n) = $id RETURN count(n)",
					Collections.singletonMap("id", vanishedId.get())).single().get(0).asLong();
			assertThat(cnt).isEqualTo(0L);
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override // needed here because there is no implicit registration of entities upfront some methods under test
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
			return TransactionalOperator.create(transactionManager);
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}

	private static class NewReactiveExecutableResultStatement implements ExecutableResultStatement {

		private final Statement delegate;

		NewReactiveExecutableResultStatement(Node namedAnswer) {
			delegate = Cypher.create(namedAnswer)
					.returning(namedAnswer)
					.build();
		}

		/**
		 * This method should move into a future Cypher-DSL version.
		 * @param reactiveQueryRunner The runner to run the statement with
		 * @return a publisher of records
		 */
		public Publisher<Record> fetchWith(ReactiveQueryRunner reactiveQueryRunner) {
			return Mono.fromCallable(this::createQuery).flatMapMany(reactiveQueryRunner::run)
					.flatMap(ReactiveResult::records);
		}

		@Override
		public <T> List<T> fetchWith(SimpleQueryRunner queryRunner, Function<Record, T> function) {
			throw new UnsupportedOperationException();
		}

		@Override public <T> CompletableFuture<List<T>> fetchWith(AsyncQueryRunner asyncQueryRunner,
				Function<Record, T> function) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ResultSummary streamWith(SimpleQueryRunner queryRunner, Consumer<Stream<Record>> consumer) {
			throw new UnsupportedOperationException();
		}

		@Override
		public ResultSummary executeWith(SimpleQueryRunner queryRunner) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CompletableFuture<ResultSummary> executeWith(AsyncQueryRunner queryRunner) {
			throw new UnsupportedOperationException();
		}

		Query createQuery() {
			return new Query(this.delegate.getCypher(), this.delegate.getParameters());
		}

		@Override
		public Map<String, Object> getParameters() {
			return this.delegate.getParameters();
		}

		@Override
		public Collection<String> getParameterNames() {
			return this.delegate.getParameterNames();
		}

		@Override
		public String getCypher() {
			return this.delegate.getCypher();
		}
	}
}
