/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.imperative;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AggregateEntitiesWithGeneratedIds.IntermediateEntity;
import org.springframework.data.neo4j.integration.shared.common.AggregateEntitiesWithGeneratedIds.StartEntity;
import org.springframework.data.neo4j.integration.shared.common.AggregateEntitiesWithInternalIds.StartEntityInternalId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_VERSION_SUPPORTING_ELEMENT_ID)
class AggregateBoundaryIT {

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
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findAll().get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllById(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findAllById(List.of(this.startEntityInternalId)).get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindById(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findById(this.startEntityInternalId).get();
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindAll(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findAllByName("start").get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindAll(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findAllBy().get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindOne(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findByName("start");
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindOne(
			@Autowired AggregateRepositoryWithInternalId repository) {
		var startEntity = repository.findBy();
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSave(@Autowired AggregateRepositoryWithInternalId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().get(0);
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.save(startEntity);

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
	void shouldOnlyPersistUntilLimitWithSaveAll(@Autowired AggregateRepositoryWithInternalId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().get(0);
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.saveAll(List.of(startEntity));

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
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findAll().get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllByIdGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findAllById(List.of(this.startEntityInternalId)).get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindByIdGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findById(this.startEntityInternalId).get();
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindAllGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findAllByName("start").get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindAllGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findAllBy().get(0);
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindOneGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findByName("start");
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindOneGeneratedId(
			@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findBy();
		assertThatLimitingWorks(startEntity);
	}

	@Test
	void shouldOnlyPersistUntilLimitWithSaveGeneratedId(@Autowired AggregateRepositoryWithGeneratedIdId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().get(0);
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.save(startEntity);

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
	void shouldOnlyPersistUntilLimitWithSaveAllGeneratedId(@Autowired AggregateRepositoryWithGeneratedIdId repository,
			@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		var startEntity = repository.findAllBy().get(0);
		startEntity.getIntermediateEntity().getDifferentAggregateEntity().setName("different");
		repository.saveAll(List.of(startEntity));

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
	void shouldAllowWiderProjectionThanDomain(@Autowired AggregateRepositoryWithGeneratedIdId repository) {
		var startEntity = repository.findProjectionBy();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isEqualTo("some_name");
	}

	@Test
	void shouldLoadCompleteEntityWhenQueriedFromDifferentEntity(@Autowired IntermediateEntityRepository repository) {
		var intermediateEntity = repository.findAll().get(0);
		assertThat(intermediateEntity.getDifferentAggregateEntity().getName()).isEqualTo("some_name");
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

	interface AggregateRepositoryWithInternalId extends Neo4jRepository<StartEntityInternalId, String> {

		List<StartEntityInternalId> findAllBy();

		List<StartEntityInternalId> findAllByName(String name);

		StartEntityInternalId findBy();

		StartEntityInternalId findByName(String name);

	}

	interface AggregateRepositoryWithGeneratedIdId extends Neo4jRepository<StartEntity, String> {

		List<StartEntity> findAllBy();

		List<StartEntity> findAllByName(String name);

		StartEntity findBy();

		StartEntity findByName(String name);

		StartEntityProjection findProjectionBy();

	}

	interface IntermediateEntityRepository extends Neo4jRepository<IntermediateEntity, String> {

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
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(AggregateBoundaryIT.class.getPackage().getName());
		}

		@Bean
		BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
