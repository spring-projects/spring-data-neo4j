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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.executables.ReactiveExecutableResultStatement;
import org.neo4j.cypherdsl.core.executables.ReactiveExecutableStatement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
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
		ReactiveExecutableResultStatement statement = ReactiveExecutableStatement.makeExecutable(
				Cypher.create(namedAnswer)
						.returning(namedAnswer)
						.build());

		AtomicLong vanishedId = new AtomicLong();
		transactionalOperator.execute(transaction -> {
					Flux<Long> inner = client.getQueryRunner()
							.flatMapMany(statement::fetchWith)
							.doOnNext(r -> vanishedId.set(r.get("n").asNode().id()))
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
	static class Config extends AbstractReactiveNeo4jConfig {

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
	}
}
