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
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.SearchResult;
import org.springframework.data.domain.Vector;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.EntityWithVector;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.VectorSearch;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
class ReactiveVectorSearchIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) detach delete n");
			session.run("""
					CREATE VECTOR INDEX dingsIndex IF NOT EXISTS
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
			session.run("DROP INDEX `dingsIndex` IF EXISTS");
		}
	}

	@Test
	void findAllWithVectorIndex(@Autowired VectorSearchRepository repository) {
		StepVerifier.create(repository.findBy("dings", Vector.of(new double[] { 0.1d, 0.1d, 0.1d })))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void findAllAsSearchResultsWithVectorIndex(@Autowired VectorSearchRepository repository) {
		StepVerifier.create(repository.findAllBy(Vector.of(new double[] { 0.1d, 0.1d, 0.1d })))
			.assertNext(result -> assertThat(result.getContent()).isNotNull())
			.assertNext(result -> assertThat(result.getContent()).isNotNull())
			.verifyComplete();
	}

	@Test
	void findSingleAsSearchResultWithVectorIndex(@Autowired VectorSearchRepository repository) {
		StepVerifier.create(repository.findBy(Vector.of(new double[] { 0.1d, 0.1d, 0.1d }), Score.of(0.9d)))
			.assertNext(result -> {
				assertThat(result).isNotNull();
				assertThat(result.getContent()).isNotNull();
				assertThat(result.getScore().getValue()).isGreaterThanOrEqualTo(0.9d);
			})
			.verifyComplete();
	}

	@Test
	void findByNameWithVectorIndex(@Autowired VectorSearchRepository repository) {
		StepVerifier.create(repository.findByName("dings", Vector.of(new double[] { 0.1d, 0.1d, 0.1d })))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findByNameWithVectorIndexAndScore(@Autowired VectorSearchRepository repository) {
		StepVerifier
			.create(repository.findByName("dings", Vector.of(new double[] { -0.7d, 0.0d, -0.7d }), Score.of(0.01d)))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void dontFindByNameWithVectorIndexAndScore(@Autowired VectorSearchRepository repository) {
		StepVerifier
			.create(repository.findByName("dings", Vector.of(new double[] { -0.7d, 0.0d, -0.7d }), Score.of(0.8d)))
			.verifyComplete();
	}

	interface VectorSearchRepository extends ReactiveNeo4jRepository<EntityWithVector, String> {

		@VectorSearch(indexName = "dingsIndex", numberOfNodes = 2)
		Flux<EntityWithVector> findBy(String name, Vector searchVector);

		@VectorSearch(indexName = "dingsIndex", numberOfNodes = 1)
		Flux<EntityWithVector> findByName(String name, Vector searchVector);

		@VectorSearch(indexName = "dingsIndex", numberOfNodes = 2)
		Flux<EntityWithVector> findByName(String name, Vector searchVector, Score score);

		@VectorSearch(indexName = "dingsIndex", numberOfNodes = 2)
		Flux<SearchResult<EntityWithVector>> findAllBy(Vector searchVector);

		@VectorSearch(indexName = "dingsIndex", numberOfNodes = 2)
		Mono<SearchResult<EntityWithVector>> findBy(Vector searchVector, Score score);

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
				ReactiveDatabaseSelectionProvider databaseNameProvider) {
			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
