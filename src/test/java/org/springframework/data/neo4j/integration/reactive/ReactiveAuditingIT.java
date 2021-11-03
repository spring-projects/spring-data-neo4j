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

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.config.EnableReactiveNeo4jAuditing;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AuditingITBase;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThing;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThingWithGeneratedId;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveAuditingIT extends AuditingITBase {

	private final ReactiveTransactionManager transactionManager;

	@Autowired
	ReactiveAuditingIT(Driver driver, BookmarkCapture bookmarkCapture, ReactiveTransactionManager transactionManager) {

		super(driver, bookmarkCapture);
		this.transactionManager = transactionManager;
	}

	@Test
	void auditingOfCreationShouldWork(@Autowired ImmutableEntityTestRepository repository) {

		List<ImmutableAuditableThing> newThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> repository.save(new ImmutableAuditableThing("A thing"))).as(StepVerifier::create)
				.recordWith(() -> newThings).expectNextCount(1L).verifyComplete();

		ImmutableAuditableThing savedThing = newThings.get(0);
		assertThat(savedThing.getCreatedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(savedThing.getCreatedBy()).isEqualTo("A user");

		assertThat(savedThing.getModifiedAt()).isNull();
		assertThat(savedThing.getModifiedBy()).isNull();

		verifyDatabase(savedThing.getId(), savedThing);
	}

	@Test
	void auditingOfModificationShouldWork(@Autowired ImmutableEntityTestRepository repository) {

		Mono<ImmutableAuditableThing> findAndUpdateAThing = repository.findById(idOfExistingThing)
				.flatMap(thing -> repository.save(thing.withName("A new name")));

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> findAndUpdateAThing).as(StepVerifier::create).consumeNextWith(savedThing -> {

			assertThat(savedThing.getCreatedAt()).isEqualTo(EXISTING_THING_CREATED_AT);
			assertThat(savedThing.getCreatedBy()).isEqualTo(EXISTING_THING_CREATED_BY);

			assertThat(savedThing.getModifiedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
			assertThat(savedThing.getModifiedBy()).isEqualTo("A user");

			assertThat(savedThing.getName()).isEqualTo("A new name");
		}).verifyComplete();

		// Need to happen outside the reactive flow, as we use the blocking session to verify the database
		verifyDatabase(idOfExistingThing, new ImmutableAuditableThing(null, EXISTING_THING_CREATED_AT,
				EXISTING_THING_CREATED_BY, DEFAULT_CREATION_AND_MODIFICATION_DATE, "A user", "A new name"));
	}

	@Test
	void auditingOfEntityWithGeneratedIdCreationShouldWork(
			@Autowired ImmutableEntityWithGeneratedIdTestRepository repository) {

		List<ImmutableAuditableThingWithGeneratedId> newThings = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> repository.save(new ImmutableAuditableThingWithGeneratedId("A thing")))
				.as(StepVerifier::create).recordWith(() -> newThings).expectNextCount(1L).verifyComplete();

		ImmutableAuditableThingWithGeneratedId savedThing = newThings.get(0);
		assertThat(savedThing.getCreatedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(savedThing.getCreatedBy()).isEqualTo("A user");

		assertThat(savedThing.getModifiedAt()).isNull();
		assertThat(savedThing.getModifiedBy()).isNull();

		verifyDatabase(savedThing.getId(), savedThing);
	}

	@Test
	void auditingOfEntityWithGeneratedIdModificationShouldWork(
			@Autowired ImmutableEntityWithGeneratedIdTestRepository repository) {

		Mono<ImmutableAuditableThingWithGeneratedId> findAndUpdateAThing = repository
				.findById(idOfExistingThingWithGeneratedId).flatMap(thing -> repository.save(thing.withName("A new name")));

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> findAndUpdateAThing).as(StepVerifier::create).consumeNextWith(savedThing -> {

			assertThat(savedThing.getCreatedAt()).isEqualTo(EXISTING_THING_CREATED_AT);
			assertThat(savedThing.getCreatedBy()).isEqualTo(EXISTING_THING_CREATED_BY);

			assertThat(savedThing.getModifiedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
			assertThat(savedThing.getModifiedBy()).isEqualTo("A user");

			assertThat(savedThing.getName()).isEqualTo("A new name");
		}).verifyComplete();

		// Need to happen outside the reactive flow, as we use the blocking session to verify the database
		verifyDatabase(idOfExistingThingWithGeneratedId,
				new ImmutableAuditableThingWithGeneratedId(null, EXISTING_THING_CREATED_AT, EXISTING_THING_CREATED_BY,
						DEFAULT_CREATION_AND_MODIFICATION_DATE, "A user", "A new name"));
	}

	interface ImmutableEntityTestRepository extends ReactiveNeo4jRepository<ImmutableAuditableThing, Long> {}

	interface ImmutableEntityWithGeneratedIdTestRepository
			extends ReactiveNeo4jRepository<ImmutableAuditableThingWithGeneratedId, String> {}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableReactiveNeo4jAuditing(modifyOnCreate = false, auditorAwareRef = "auditorProvider",
			dateTimeProviderRef = "fixedDateTimeProvider")
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public ReactiveAuditorAware<String> auditorProvider() {
			return () -> Mono.just("A user");
		}

		@Bean
		public DateTimeProvider fixedDateTimeProvider() {
			return () -> Optional.of(DEFAULT_CREATION_AND_MODIFICATION_DATE);
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
