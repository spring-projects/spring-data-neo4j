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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2572IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run("CREATE (p:GH2572Parent {id: 'GH2572Parent-1', name:'no-pets'})");
			session.run("CREATE (p:GH2572Parent {id: 'GH2572Parent-2', name:'one-pet'}) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-3', name: 'a-pet'})");
			session.run("MATCH (p:GH2572Parent {id: 'GH2572Parent-2'}) CREATE (p) <-[:IS_PET]- (:GH2572Child {id: 'GH2572Child-4', name: 'another-pet'})");
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2572
	void allShouldFetchCorrectNumberOfChildNodes(@Autowired DogRepository dogRepository) {
		List<GH2572Child> dogsForPerson = dogRepository.getDogsForPerson("GH2572Parent-2");
		assertThat(dogsForPerson).hasSize(2);
	}

	@Test // GH-2572
	void allShouldNotFailWithoutMatchingRootNodes(@Autowired DogRepository dogRepository) {
		List<GH2572Child> dogsForPerson = dogRepository.getDogsForPerson("GH2572Parent-1");
		assertThat(dogsForPerson).isEmpty();
	}

	@Test // GH-2572
	void oneShouldFetchCorrectNumberOfChildNodes(@Autowired DogRepository dogRepository) {
		Optional<GH2572Child> optionalChild = dogRepository.findOneDogForPerson("GH2572Parent-2");
		assertThat(optionalChild).map(GH2572Child::getName).hasValue("a-pet");
	}

	@Test // GH-2572
	void oneShouldNotFailWithoutMatchingRootNodes(@Autowired DogRepository dogRepository) {
		Optional<GH2572Child> optionalChild = dogRepository.findOneDogForPerson("GH2572Parent-1");
		assertThat(optionalChild).isEmpty();
	}

	@Test // GH-2572
	void getOneShouldFetchCorrectNumberOfChildNodes(@Autowired DogRepository dogRepository) {
		GH2572Child gh2572Child = dogRepository.getOneDogForPerson("GH2572Parent-2");
		assertThat(gh2572Child.getName()).isEqualTo("a-pet");
	}

	@Test // GH-2572
	void getOneShouldNotFailWithoutMatchingRootNodes(@Autowired DogRepository dogRepository) {
		GH2572Child gh2572Child = dogRepository.getOneDogForPerson("GH2572Parent-1");
		assertThat(gh2572Child).isNull();
	}

	public interface DogRepository extends Neo4jRepository<GH2572Child, String> {

		@Query("MATCH(person:GH2572Parent {id: $id}) "
			   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			   + "RETURN dog")
		List<GH2572Child> getDogsForPerson(String id);

		@Query("MATCH(person:GH2572Parent {id: $id}) "
			   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			   + "RETURN dog ORDER BY dog.name ASC LIMIT 1")
		Optional<GH2572Child> findOneDogForPerson(String id);

		@Query("MATCH(person:GH2572Parent {id: $id}) "
			   + "OPTIONAL MATCH (person)<-[:IS_PET]-(dog:GH2572Child) "
			   + "RETURN dog ORDER BY dog.name ASC LIMIT 1")
		GH2572Child getOneDogForPerson(String id);
	}


	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
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
	}
}
