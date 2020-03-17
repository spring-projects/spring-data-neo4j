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

import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.integration.reactive.repositories.ReactiveThingRepository;
import org.neo4j.springframework.data.integration.shared.CallbacksITBase;
import org.neo4j.springframework.data.integration.shared.ThingWithAssignedId;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.repository.event.ReactiveBeforeBindCallback;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Michael J. Simons
 */
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveCallbacksIT extends CallbacksITBase {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final ReactiveTransactionManager transactionManager;

	@Autowired ReactiveCallbacksIT(Driver driver, ReactiveTransactionManager transactionManager) {

		super(driver);
		this.transactionManager = transactionManager;
	}

	@Test
	void onBeforeBindShouldBeCalledForSingleEntity(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
		thing.setName("A name");

		Mono<ThingWithAssignedId> operationUnderTest = Mono.just(thing).flatMap(repository::save);

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> savedThings)
			.expectNextMatches(t -> t.getName().equals("A name (Edited)"))
			.verifyComplete();

		verifyDatabase(savedThings);
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntitiesUsingIterable(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1");
		thing1.setName("A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2");
		thing2.setName("Another name");
		repository.saveAll(Arrays.asList(thing1, thing2));

		Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(Arrays.asList(thing1, thing2));

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> savedThings)
			.expectNextMatches(t -> t.getName().equals("A name (Edited)"))
			.expectNextMatches(t -> t.getName().equals("Another name (Edited)"))
			.verifyComplete();

		verifyDatabase(savedThings);
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntitiesUsingPublisher(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1");
		thing1.setName("A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2");
		thing2.setName("Another name");
		repository.saveAll(Arrays.asList(thing1, thing2));

		Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(Flux.just(thing1, thing2));

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> savedThings)
			.expectNextMatches(t -> t.getName().equals("A name (Edited)"))
			.expectNextMatches(t -> t.getName().equals("Another name (Edited)"))
			.verifyComplete();

		verifyDatabase(savedThings);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		ReactiveBeforeBindCallback<ThingWithAssignedId> nameChanger() {
			return entity -> {
				ThingWithAssignedId updatedThing = new ThingWithAssignedId(entity.getTheId());
				updatedThing.setName(entity.getName() + " (Edited)");
				return Mono.just(updatedThing);
			};
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithAssignedId.class.getPackage().getName());
		}
	}
}
