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
package org.springframework.data.neo4j.integration.issues.gh2632;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.reactivestreams.ReactiveResult;
import org.neo4j.driver.reactivestreams.ReactiveSession;
import org.neo4j.driver.reactivestreams.ReactiveTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveConnectionAcquisitionIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	protected static void setupData(@Autowired Driver driver) {
		try (Session session = driver.session()) {
			try (Transaction transaction = session.beginTransaction()) {
				transaction.run("MATCH (n) detach delete n");
				transaction.run("""
						CREATE (m:Movie {title: "I don't want warnings", id: randomUUID()})
						"""
				).consume();
				transaction.commit();
			}
		}
	}

	@Test
		// GH-2632
	void connectionAcquisitionAfterErrorViaSDNTxManagerShouldWork(@Autowired MovieRepository movieRepository, @Autowired Driver driver) {
		UUID id = UUID.randomUUID();
		Flux
			.range(1, 5)
			.flatMap(i -> movieRepository.findById(id).switchIfEmpty(Mono.error(new RuntimeException())))
			.then()
			.as(StepVerifier::create)
			.verifyError();

		try (Session session = driver.session()) {
			long aNumber = session.run("RETURN 1").single().get(0).asLong();
			assertThat(aNumber).isOne();
		}
	}

	@Test // GH-2632
	void connectionAcquisitionAfterErrorViaImplicitTXShouldWork(@Autowired Driver driver) {
		Flux
			.range(1, 5)
			.flatMap(
				i -> {
					Query query = new Query("MATCH (p:Product) WHERE p.id = $id RETURN p.title", Collections.singletonMap("id", 0));
					return Flux.usingWhen(
							Mono.fromSupplier(() -> driver.session(ReactiveSession.class)),
							session -> Flux.from(session.run(query))
									.flatMap(result -> Flux.from(result.records()))
									.map(record -> record.get(0).asString()),
							session -> Mono.fromDirect(session.close())
						).switchIfEmpty(Mono.error(new RuntimeException()));
				}
			)
			.then()
			.as(StepVerifier::create)
			.verifyError();

		try (Session session = driver.session()) {
			long aNumber = session.run("RETURN 1").single().get(0).asLong();
			assertThat(aNumber).isOne();
		}
	}

	record SessionAndTx(ReactiveSession session, ReactiveTransaction tx) {
	}

	@Test // GH-2632
	void connectionAcquisitionAfterErrorViaExplicitTXShouldWork(@Autowired Driver driver) {
		Flux
			.range(1, 5)
			.flatMap(
				i -> {
					Mono<SessionAndTx> f = Mono
						.just(driver.session(ReactiveSession.class))
						.flatMap(s -> Mono.fromDirect(s.beginTransaction()).map(tx -> new SessionAndTx(s, tx)));
					return Flux.usingWhen(f,
						h -> Flux.from(h.tx.run("MATCH (n) WHERE false = true RETURN n")).flatMap(ReactiveResult::records),
						h -> Mono.from(h.tx.commit()).then(Mono.from(h.session.close())),
						(h, e) -> Mono.from(h.tx.rollback()).then(Mono.from(h.session.close())),
						h -> Mono.from(h.tx.rollback()).then(Mono.from(h.session.close()))
					).switchIfEmpty(Mono.error(new RuntimeException()));
				}
			)
			.then()
			.as(StepVerifier::create)
			.verifyError();

		try (Session session = driver.session()) {
			long aNumber = session.run("RETURN 1").single().get(0).asLong();
			assertThat(aNumber).isOne();
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			var config = org.neo4j.driver.Config.builder()
				.withMaxConnectionPoolSize(2)
				.withConnectionAcquisitionTimeout(2, TimeUnit.SECONDS)
				.withLeakedSessionsLogging()
				.build();
			return GraphDatabase.driver(neo4jConnectionSupport.url, neo4jConnectionSupport.authToken, config);
		}
	}
}
