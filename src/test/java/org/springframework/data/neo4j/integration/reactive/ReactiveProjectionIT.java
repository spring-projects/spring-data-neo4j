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

import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.NamesOnly;
import org.springframework.data.neo4j.integration.shared.common.NamesOnlyDto;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonSummary;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveProjectionIT {

	private static final String FIRST_NAME = "Hans";
	private static final String LAST_NAME = "Mueller";
	private static final String CITY = "Braunschweig";

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	@Autowired
	ReactiveProjectionIT(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void setup(@Autowired BookmarkCapture bookmarkCapture) {
		Session session = driver.session(bookmarkCapture.createSessionConfig());
		Transaction transaction = session.beginTransaction();

		transaction.run("MATCH (n) detach delete n");

		transaction.run("CREATE (:Person{firstName:'" + FIRST_NAME + "', lastName:'" + LAST_NAME + "'})" + "-[:LIVES_AT]->"
				+ "(:Address{city:'" + CITY + "'})");

		transaction.commit();
		transaction.close();
		bookmarkCapture.seedWith(session.lastBookmark());
		session.close();
	}

	@Test
	void loadNamesOnlyProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastName(LAST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);

			String expectedFullName = FIRST_NAME + " " + LAST_NAME;
			assertThat(person.getFullName()).isEqualTo(expectedFullName);
		}).verifyComplete();
	}

	@Test
	void loadPersonSummaryProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByFirstName(FIRST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);
			assertThat(person.getAddress()).isNotNull();

			PersonSummary.AddressSummary address = person.getAddress();
			assertThat(address.getCity()).isEqualTo(CITY);
		}).verifyComplete();
	}

	@Test
	void loadNamesOnlyDtoProjection(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByFirstNameAndLastName(FIRST_NAME, LAST_NAME)).assertNext(person -> {
			assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
			assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnly(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnly.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);

					String expectedFullName = FIRST_NAME + " " + LAST_NAME;
					assertThat(person.getFullName()).isEqualTo(expectedFullName);
				}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForPersonSummary(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, PersonSummary.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);
					assertThat(person.getAddress()).isNotNull();

					PersonSummary.AddressSummary address = person.getAddress();
					assertThat(address.getCity()).isEqualTo(CITY);
				}).verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnlyDto(@Autowired ReactiveProjectionPersonRepository repository) {

		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnlyDto.class))
				.assertNext(person -> {
					assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
					assertThat(person.getLastName()).isEqualTo(LAST_NAME);
				}).verifyComplete();
	}

	interface ReactiveProjectionPersonRepository extends ReactiveNeo4jRepository<Person, Long> {

		Flux<NamesOnly> findByLastName(String lastName);

		Flux<PersonSummary> findByFirstName(String firstName);

		Flux<NamesOnlyDto> findByFirstNameAndLastName(String firstName, String lastName);

		<T> Flux<T> findByLastNameAndFirstName(String lastName, String firstName, Class<T> projectionClass);
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

	}

}
