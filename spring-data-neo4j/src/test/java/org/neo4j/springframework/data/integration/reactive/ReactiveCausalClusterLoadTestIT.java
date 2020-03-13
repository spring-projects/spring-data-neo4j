/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.integration.reactive;

import static java.util.stream.Collectors.*;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.junit.jupiter.causal_cluster.Neo4jUri;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.ReactiveNeo4jClient;
import org.neo4j.springframework.data.integration.shared.ThingWithGeneratedId;
import org.neo4j.springframework.data.integration.shared.ThingWithSequence;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.CausalClusterIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * This tests needs a Neo4j causal cluster. We run them based on Testcontainers. It requires some resources
 * as well as acceptance of the commercial license, so this test is disabled by default.
 *
 * @author Michael J. Simons
 */
@CausalClusterIntegrationTest
class ReactiveCausalClusterLoadTestIT {

	@Neo4jUri
	private static URI neo4jUri;

	@RepeatedTest(20)
	void transactionsShouldBeSerializable(@Autowired ThingService thingService) throws InterruptedException {

		int numberOfRequests = 100;
		AtomicLong sequence = new AtomicLong(0L);
		thingService.getMaxInstance()
			.as(StepVerifier::create)
			.consumeNextWith(sequence::set)
			.verifyComplete();

		Callable<ThingWithSequence> createAndRead = () -> {
			List<ThingWithSequence> result = new ArrayList<>();
			long sequenceNumber = sequence.incrementAndGet();
			thingService.newThing(sequenceNumber)
				.then(thingService.findOneBySequenceNumber(sequenceNumber))
				.as(StepVerifier::create)
				.recordWith((() -> result))
				.expectNextMatches(t -> t.getSequenceNumber().equals(sequenceNumber))
				.verifyComplete();
			return result.get(0);
		};

		ExecutorService executor = Executors.newCachedThreadPool();
		List<Future<ThingWithSequence>> executedWrites = executor.invokeAll(IntStream.range(0, numberOfRequests)
			.mapToObj(i -> createAndRead).collect(toList()));
		try {
			executedWrites.forEach(request -> {
				try {
					request.get();
				} catch (InterruptedException e) {
				} catch (ExecutionException e) {
					Assertions.fail("At least one request failed " + e.getMessage());
				}
			});
		} finally {
			executor.shutdown();
		}
	}

	interface ThingRepository extends ReactiveNeo4jRepository<ThingWithSequence, Long> {
		Mono<ThingWithSequence> findOneBySequenceNumber(long sequenceNumber);
	}

	static class ThingService {
		private final ReactiveNeo4jClient neo4jClient;

		private final ThingRepository thingRepository;

		ThingService(ReactiveNeo4jClient neo4jClient, ThingRepository thingRepository) {
			this.neo4jClient = neo4jClient;
			this.thingRepository = thingRepository;
		}

		public Mono<Long> getMaxInstance() {
			return neo4jClient
				.query("MATCH (t:ThingWithSequence) RETURN COALESCE(MAX(t.sequenceNumber), -1) AS maxInstance")
				.fetchAs(Long.class)
				.one();
		}

		@Transactional
		public Mono<ThingWithSequence> newThing(long i) {
			return this.thingRepository.save(new ThingWithSequence(i));
		}

		@Transactional(readOnly = true)
		public Mono<ThingWithSequence> findOneBySequenceNumber(long sequenceNumber) {
			return thingRepository.findOneBySequenceNumber(sequenceNumber);
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class TestConfig extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {

			Driver driver = GraphDatabase.driver(neo4jUri, AuthTokens.basic("neo4j", "secret"),
				Config.builder().withConnectionTimeout(2, TimeUnit.MINUTES).build());
			driver.verifyConnectivity();
			return driver;
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithGeneratedId.class.getPackage().getName());
		}

		@Bean
		public ThingService thingService(ReactiveNeo4jClient neo4jClient, ThingRepository thingRepository) {
			return new ThingService(neo4jClient, thingRepository);
		}
	}
}
