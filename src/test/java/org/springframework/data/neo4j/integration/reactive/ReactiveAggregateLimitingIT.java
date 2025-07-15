/*
 * Copyright 2011-2025 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AggregateEntitiesWithGeneratedIds.StartEntity;
import org.springframework.data.neo4j.integration.shared.common.AggregateEntitiesWithInternalIds.StartEntityInternalId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveAggregateLimitingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final String startEntityUuid = "1476db91-10e2-4202-a63f-524be2dcb7fe";

	private String startEntityInternalId;

	@BeforeEach
	void setup(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n").consume();
			this.startEntityInternalId = session
				.run("""
						CREATE (se:StartEntityInternalId{name:'start'})-[:CONNECTED]->(ie:IntermediateEntityInternalId)-[:CONNECTED]->(dae:DifferentAggregateEntityInternalId{name:'some_name'})
						RETURN elementId(se) as id;
						""")
				.single()
				.get("id")
				.asString();
			session
				.run("""
						CREATE (se:StartEntity{name:'start'})-[:CONNECTED]->(ie:IntermediateEntity)-[:CONNECTED]->(dae:DifferentAggregateEntity{name:'some_name'})
						SET se.id = $uuid1, ie.id = $uuid2, dae.id = $uuid3;
						""",
						Map.of("uuid1", this.startEntityInternalId, "uuid2", UUID.randomUUID().toString(), "uuid3",
								UUID.randomUUID().toString()))
				.consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@AfterEach
	void tearDown(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAll(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findAll()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllById(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findAllById(List.of(this.startEntityInternalId))).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindById(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findById(this.startEntityInternalId)).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindAll(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findAllByName("start")).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindAll(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findAllBy()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindOne(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findByName("start")).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindOne(
			@Autowired ReactiveAggregateRepositoryWithInternalId repository) {
		StepVerifier.create(repository.findBy()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSave(@Autowired ReactiveAggregateRepositoryWithInternalId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().blockLast();
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.save(startEntity).block();

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var name = session.executeRead(tx -> tx.run(
					"MATCH (:StartEntityInternalId)-[:CONNECTED]->(:IntermediateEntityInternalId)-[:CONNECTED]->(dae:DifferentAggregateEntityInternalId) return dae.name as name")
				.single()
				.get("name")
				.asString());
			bookmarkCapture.seedWith(session.lastBookmarks());
			assertThat(name).isEqualTo("some_name");
		}
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSaveAll(@Autowired ReactiveAggregateRepositoryWithInternalId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().blockLast();
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.saveAll(List.of(startEntity)).blockLast();

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var name = session.executeRead(tx -> tx.run(
					"MATCH (:StartEntityInternalId)-[:CONNECTED]->(:IntermediateEntityInternalId)-[:CONNECTED]->(dae:DifferentAggregateEntityInternalId) return dae.name as name")
				.single()
				.get("name")
				.asString());
			bookmarkCapture.seedWith(session.lastBookmarks());
			assertThat(name).isEqualTo("some_name");
		}
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findAll()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllByIdGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findAllById(List.of(this.startEntityInternalId))).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindByIdGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findById(this.startEntityInternalId)).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindAllGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findAllByName("start")).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindAllGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findAllBy()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindOneGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findByName("start")).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindOneGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findBy()).assertNext(startEntity -> {
			assertThatLimitingWorks(startEntity);
		}).verifyComplete();
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSaveGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository, @Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().blockFirst();
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.save(startEntity).block();

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var name = session.executeRead(tx -> tx.run(
					"MATCH (:StartEntity)-[:CONNECTED]->(:IntermediateEntity)-[:CONNECTED]->(dae:DifferentAggregateEntity) return dae.name as name")
				.single()
				.get("name")
				.asString());
			bookmarkCapture.seedWith(session.lastBookmarks());
			assertThat(name).isEqualTo("some_name");
		}
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSaveAllGeneratedId(
			@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository, @Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().blockFirst();
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.saveAll(List.of(startEntity)).blockLast();

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var name = session.executeRead(tx -> tx.run(
					"MATCH (:StartEntity)-[:CONNECTED]->(:IntermediateEntity)-[:CONNECTED]->(dae:DifferentAggregateEntity) return dae.name as name")
				.single()
				.get("name")
				.asString());
			bookmarkCapture.seedWith(session.lastBookmarks());
			assertThat(name).isEqualTo("some_name");
		}
	}

	@Test
	void shouldAllowWiderProjectionThanDomain(@Autowired ReactiveAggregateRepositoryWithGeneratedIdId repository) {
		StepVerifier.create(repository.findProjectionBy())
			.assertNext(startEntity -> assertThat(
					startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName())
				.isEqualTo("some_name"))
			.verifyComplete();
	}

	private void assertThatLimitingWorks(StartEntity startEntity) {
		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	private void assertThatLimitingWorks(StartEntityInternalId startEntity) {
		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	interface ReactiveAggregateRepositoryWithInternalId extends ReactiveNeo4jRepository<StartEntityInternalId, String> {

		Flux<StartEntityInternalId> findAllBy();

		Flux<StartEntityInternalId> findAllByName(String name);

		Mono<StartEntityInternalId> findBy();

		Mono<StartEntityInternalId> findByName(String name);

	}

	interface ReactiveAggregateRepositoryWithGeneratedIdId extends ReactiveNeo4jRepository<StartEntity, String> {

		Flux<StartEntity> findAllBy();

		Flux<StartEntity> findAllByName(String name);

		Mono<StartEntity> findBy();

		Mono<StartEntity> findByName(String name);

		Mono<StartEntityProjection> findProjectionBy();

	}

	interface StartEntityProjection {

		String getName();

		IntermediateEntityProjection getIntermediateEntity();

	}

	interface IntermediateEntityProjection {

		DifferentAggregateEntityProjection getDifferentAggregateEntity();

	}

	interface DifferentAggregateEntityProjection {

		String getName();

	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture()));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
