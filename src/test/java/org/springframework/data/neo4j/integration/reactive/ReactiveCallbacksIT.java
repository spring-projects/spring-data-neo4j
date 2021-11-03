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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.callback.ReactiveBeforeBindCallback;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.reactive.repositories.ReactiveThingRepository;
import org.springframework.data.neo4j.integration.shared.common.CallbacksITBase;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAssignedId;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveCallbacksIT extends CallbacksITBase {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final ReactiveTransactionManager transactionManager;

	@Autowired
	ReactiveCallbacksIT(Driver driver, ReactiveTransactionManager transactionManager, BookmarkCapture bookmarkCapture) {

		super(driver, bookmarkCapture);
		this.transactionManager = transactionManager;
	}

	@Test
	void onBeforeBindShouldBeCalledForSingleEntity(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB", "A name");

		Mono<ThingWithAssignedId> operationUnderTest = Mono.just(thing).flatMap(repository::save);

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> savedThings)
				.expectNextMatches(t -> t.getName().equals("A name (Edited)")).verifyComplete();

		verifyDatabase(savedThings);
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntitiesUsingIterable(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1", "A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2", "Another name");
		repository.saveAll(Arrays.asList(thing1, thing2));

		Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(Arrays.asList(thing1, thing2));

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> savedThings)
				.expectNextMatches(t -> t.getName().equals("A name (Edited)"))
				.expectNextMatches(t -> t.getName().equals("Another name (Edited)")).verifyComplete();

		verifyDatabase(savedThings);
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntitiesUsingPublisher(@Autowired ReactiveThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1", "A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2", "Another name");
		repository.saveAll(Arrays.asList(thing1, thing2));

		Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(Flux.just(thing1, thing2));

		List<ThingWithAssignedId> savedThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> savedThings)
				.expectNextMatches(t -> t.getName().equals("A name (Edited)"))
				.expectNextMatches(t -> t.getName().equals("Another name (Edited)")).verifyComplete();

		verifyDatabase(savedThings);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		ReactiveBeforeBindCallback<ThingWithAssignedId> nameChanger() {
			return entity -> {
				ThingWithAssignedId updatedThing = new ThingWithAssignedId(entity.getTheId(), entity.getName() + " (Edited)");
				return Mono.just(updatedThing);
			};
		}

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
