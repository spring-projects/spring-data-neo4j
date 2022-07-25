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
package org.springframework.data.neo4j.integration.issues.gh2572;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2572ReactiveIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run("CREATE (p:GH2572Parent {id: 'GH2572Parent-1', name:'no-pets'})");
			session.run(
					"CREATE (p:GH2572Parent {id: 'GH2572Parent-2', name:'one-pet'}) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-3', name: 'a-pet'})");
			session.run(
					"MATCH (p:GH2572Parent {id: 'GH2572Parent-2'}) CREATE (p) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-4', name: 'another-pet'})");
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2572
	void allShouldFetchCorrectNumberOfChildNodes(@Autowired DogRepository dogRepository) {
		dogRepository.getDogsForPerson("GH2572Parent-2")
				.as(StepVerifier::create)
				.expectNextCount(2L)
				.verifyComplete();
	}

	@Test // GH-2572
	void allShouldNotFailWithoutMatchingRootNodes(@Autowired DogRepository dogRepository) {
		dogRepository.getDogsForPerson("GH2572Parent-1")
				.as(StepVerifier::create)
				.expectNextCount(0L)
				.verifyComplete();
	}

	@Test // GH-2572
	void oneShouldFetchCorrectNumberOfChildNodes(@Autowired DogRepository dogRepository) {
		dogRepository.findOneDogForPerson("GH2572Parent-2")
				.map(GH2572Child::getName)
				.as(StepVerifier::create)
				.expectNext("a-pet")
				.verifyComplete();
	}

	@Test // GH-2572
	void oneShouldNotFailWithoutMatchingRootNodes(@Autowired DogRepository dogRepository) {
		dogRepository.findOneDogForPerson("GH2572Parent-1")
				.as(StepVerifier::create)
				.expectNextCount(0L)
				.verifyComplete();
	}

	public interface DogRepository extends ReactiveNeo4jRepository<GH2572Child, String> {

		@Query("MATCH(person:GH2572Parent {id: $id}) "
			   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			   + "RETURN dog")
		Flux<GH2572Child> getDogsForPerson(String id);

		@Query("MATCH(person:GH2572Parent {id: $id}) "
			   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			   + "RETURN dog ORDER BY dog.name ASC LIMIT 1")
		Mono<GH2572Child> findOneDogForPerson(String id);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver,
				ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(GH2572BaseEntity.class.getPackage().getName());
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
