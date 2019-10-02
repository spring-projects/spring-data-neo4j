/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.reactive;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.NamesOnly;
import org.neo4j.springframework.data.integration.shared.NamesOnlyDto;
import org.neo4j.springframework.data.integration.shared.Person;
import org.neo4j.springframework.data.integration.shared.PersonSummary;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveProjectionIT {

	private static final String FIRST_NAME = "Hans";
	private static final String LAST_NAME = "Mueller";
	private static final String CITY = "Braunschweig";

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired private ReactiveProjectionPersonRepository repository;
	@Autowired private Driver driver;

	@BeforeEach
	void setup() {
		Session session = driver.session();
		Transaction transaction = session.beginTransaction();

		transaction.run("MATCH (n) detach delete n");

		transaction.run("CREATE (:Person{firstName:'" + FIRST_NAME + "', lastName:'" + LAST_NAME + "'})"
			+ "-[:LIVES_AT]->"
			+ "(:Address{city:'" + CITY + "'})");

		transaction.commit();
		transaction.close();
		session.close();
	}

	@Test
	void loadNamesOnlyProjection() {
		StepVerifier.create(repository.findByLastName(LAST_NAME))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);

				String expectedFullName = FIRST_NAME + " " + LAST_NAME;
				assertThat(person.getFullName()).isEqualTo(expectedFullName);
			})
			.verifyComplete();
	}

	@Test
	void loadPersonSummaryProjection() {
		StepVerifier.create(repository.findByFirstName(FIRST_NAME))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);
				assertThat(person.getAddress()).isNotNull();

				PersonSummary.AddressSummary address = person.getAddress();
				assertThat(address.getCity()).isEqualTo(CITY);
			})
			.verifyComplete();
	}

	@Test
	void loadNamesOnlyDtoProjection() {
		StepVerifier.create(repository.findByFirstNameAndLastName(FIRST_NAME, LAST_NAME))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);
			})
			.verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnly() {
		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnly.class))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);

				String expectedFullName = FIRST_NAME + " " + LAST_NAME;
				assertThat(person.getFullName()).isEqualTo(expectedFullName);
			})
			.verifyComplete();
	}

	@Test
	void findDynamicProjectionForPersonSummary() {
		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, PersonSummary.class))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);
				assertThat(person.getAddress()).isNotNull();

				PersonSummary.AddressSummary address = person.getAddress();
				assertThat(address.getCity()).isEqualTo(CITY);
			})
			.verifyComplete();
	}

	@Test
	void findDynamicProjectionForNamesOnlyDto() {
		StepVerifier.create(repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnlyDto.class))
			.assertNext(person -> {
				assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
				assertThat(person.getLastName()).isEqualTo(LAST_NAME);
			})
			.verifyComplete();
	}

	public interface ReactiveProjectionPersonRepository extends ReactiveNeo4jRepository<Person, Long> {

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

		@Override
		protected Collection<String> getMappingBasePackages() {
			return singletonList(Person.class.getPackage().getName());
		}
	}

}
