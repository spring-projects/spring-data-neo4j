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
package org.springframework.data.neo4j.integration.imperative;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.StartEntity;
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
public class AggregateLimitingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private String startEntityId;

	@BeforeEach
	void setup(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n").consume();
			this.startEntityId = session
				.run("""
						CREATE (se:StartEntity{name:'start'})-[:CONNECTED]->(ie:IntermediateEntity)-[:CONNECTED]->(dae:DifferentAggregateEntity{name:'some_name'})
						RETURN elementId(se) as id;
						""")
				.single()
				.get("id")
				.asString();
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
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAll(@Autowired AggregateRepository repository) {
		var startEntity = repository.findAll().get(0);

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindAllById(
			@Autowired AggregateRepository repository) {
		var startEntity = repository.findAllById(List.of(this.startEntityId)).get(0);

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithGenericFindById(@Autowired AggregateRepository repository) {
		var startEntity = repository.findById(this.startEntityId).get();

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindAll(@Autowired AggregateRepository repository) {
		var startEntity = repository.findAllByName("start").get(0);

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindAll(
			@Autowired AggregateRepository repository) {
		var startEntity = repository.findAllBy().get(0);

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithPartTreeFindOne(@Autowired AggregateRepository repository) {
		var startEntity = repository.findByName("start");

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyReportIdForDifferentAggregateEntityWithEmptyParameterPartTreeFindOne(
			@Autowired AggregateRepository repository) {
		var startEntity = repository.findBy();

		assertThat(startEntity).isNotNull();
		assertThat(startEntity.getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getId()).isNotNull();
		assertThat(startEntity.getIntermediateEntity().getDifferentAggregateEntity().getName()).isNull();
	}

	@Test
	void shouldOnlyPersistUntilLimit(@Autowired AggregateRepository repository, @Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture) {
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

	interface AggregateRepository extends Neo4jRepository<StartEntity, String> {

		List<StartEntity> findAllBy();

		List<StartEntity> findAllByName(String name);

		StartEntity findBy();

		StartEntity findByName(String name);

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
			return Collections.singleton(AggregateLimitingIT.class.getPackage().getName());
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
