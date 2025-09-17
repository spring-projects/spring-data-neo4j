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

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.SearchResults;
import org.springframework.data.domain.Vector;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.EntityWithVector;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.VectorSearch;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_VECTOR_INDEX)
class VectorSearchIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n");
			session.run("""
					CREATE VECTOR INDEX entityIndex IF NOT EXISTS
					FOR (m:EntityWithVector)
					ON m.myVector
					OPTIONS { indexConfig: {
					 `vector.dimensions`: 3,
					 `vector.similarity_function`: 'cosine'
					}}""").consume();
			session.run(
					"CREATE (e:EntityWithVector{name:'dings'}) WITH e CALL db.create.setNodeVectorProperty(e, 'myVector', [0.1, 0.1, 0.1])")
				.consume();
			session.run(
					"CREATE (e:EntityWithVector{name:'dings2'}) WITH e CALL db.create.setNodeVectorProperty(e, 'myVector', [0.7, 0.0, 0.3])")
				.consume();
			session.run("CALL db.awaitIndexes()").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@AfterEach
	void removeIndex(@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("DROP INDEX `entityIndex` IF EXISTS");
		}
	}

	@Test
	void findAllWithVectorIndex(@Autowired VectorSearchRepository repository) {
		var result = repository.findBy(Vector.of(new double[] { 0.1d, 0.1d, 0.1d }));
		assertThat(result).hasSize(2);
	}

	@Test
	void findAllAsSearchResultsWithVectorIndex(@Autowired VectorSearchRepository repository) {
		var result = repository.findAllBy(Vector.of(new double[] { 0.1d, 0.1d, 0.1d }));
		assertThat(result).hasSize(2);
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent()).allSatisfy(content -> {
			assertThat(content.getContent()).isNotNull();
			assertThat(content.getScore().getValue()).isGreaterThanOrEqualTo(0.8d);
		});
	}

	@Test
	void findSingleAsSearchResultWithVectorIndex(@Autowired VectorSearchRepository repository) {
		var result = repository.findBy(Vector.of(new double[] { 0.1d, 0.1d, 0.1d }), Score.of(0.9d));
		assertThat(result).isNotNull();
		assertThat(result.getContent()).isNotNull();
		assertThat(result.getScore().getValue()).isGreaterThanOrEqualTo(0.9d);
	}

	@Test
	void findSearchResultsOfSearchResults(@Autowired VectorSearchRepository repository) {
		var result = repository.findDistinctByName("dings", Vector.of(new double[] { 0.1d, 0.1d, 0.1d }));
		assertThat(result).isNotNull();
	}

	@Test
	void findByNameWithVectorIndex(@Autowired VectorSearchRepository repository) {
		var result = repository.findByName("dings", Vector.of(new double[] { 0.1d, 0.1d, 0.1d }));
		assertThat(result).hasSize(1);
	}

	@Test
	void findByNameWithVectorIndexAndScore(@Autowired VectorSearchRepository repository) {
		var result = repository.findByName("dings", Vector.of(new double[] { -0.7d, 0.0d, -0.7d }), Score.of(0.01d));
		assertThat(result).hasSize(1);
	}

	@Test
	void dontFindByNameWithVectorIndexAndScore(@Autowired VectorSearchRepository repository) {
		var result = repository.findByName("dings", Vector.of(new double[] { -0.7d, 0.0d, -0.7d }), Score.of(0.8d));
		assertThat(result).hasSize(0);
	}

	// tag::sdn-vector-search.usage[]
	interface VectorSearchRepository extends Neo4jRepository<EntityWithVector, String> {

		// end::sdn-vector-search.usage[]
		// tag::sdn-vector-search.usage.findall[]
		@VectorSearch(indexName = "entityIndex", numberOfNodes = 2)
		List<EntityWithVector> findBy(Vector searchVector);
		// end::sdn-vector-search.usage.findall[]

		// tag::sdn-vector-search.usage.findbyproperty[]
		@VectorSearch(indexName = "entityIndex", numberOfNodes = 1)
		List<EntityWithVector> findByName(String name, Vector searchVector);
		// end::sdn-vector-search.usage.findbyproperty[]

		@VectorSearch(indexName = "entityIndex", numberOfNodes = 1)
		List<EntityWithVector> findDistinctByName(String name, Vector searchVector);

		@VectorSearch(indexName = "entityIndex", numberOfNodes = 2)
		List<EntityWithVector> findByName(String name, Vector searchVector, Score score);

		@VectorSearch(indexName = "entityIndex", numberOfNodes = 2)
		SearchResults<EntityWithVector> findAllBy(Vector searchVector);

		@VectorSearch(indexName = "entityIndex", numberOfNodes = 2)
		SearchResult<EntityWithVector> findBy(Vector searchVector, Score score);

		// tag::sdn-vector-search.usage[]

	}
	// end::sdn-vector-search.usage[]

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

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
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {
			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
