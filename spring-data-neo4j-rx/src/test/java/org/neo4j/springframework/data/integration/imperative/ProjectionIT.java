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
package org.neo4j.springframework.data.integration.imperative;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.NamesOnly;
import org.neo4j.springframework.data.integration.shared.NamesOnlyDto;
import org.neo4j.springframework.data.integration.shared.Person;
import org.neo4j.springframework.data.integration.shared.PersonSummary;
import org.neo4j.springframework.data.repository.Neo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
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
class ProjectionIT {

	private static final String FIRST_NAME = "Hans";
	private static final String LAST_NAME = "Mueller";
	private static final String CITY = "Braunschweig";

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired private ProjectionPersonRepository repository;
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
		Collection<NamesOnly> people = repository.findByLastName(LAST_NAME);
		assertThat(people).hasSize(1);

		NamesOnly person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

		String expectedFullName = FIRST_NAME + " " + LAST_NAME;
		assertThat(person.getFullName()).isEqualTo(expectedFullName);

	}

	@Test
	void loadPersonSummaryProjection() {
		Collection<PersonSummary> people = repository.findByFirstName(FIRST_NAME);
		assertThat(people).hasSize(1);

		PersonSummary person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		assertThat(person.getAddress()).isNotNull();

		PersonSummary.AddressSummary address = person.getAddress();
		assertThat(address.getCity()).isEqualTo(CITY);

	}

	@Test
	void loadNamesOnlyDtoProjection() {
		Collection<NamesOnlyDto> people = repository.findByFirstNameAndLastName(FIRST_NAME, LAST_NAME);
		assertThat(people).hasSize(1);

		NamesOnlyDto person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

	}

	@Test
	void findDynamicProjectionForNamesOnly() {
		Collection<NamesOnly> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnly.class);
		assertThat(people).hasSize(1);

		NamesOnly person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

		String expectedFullName = FIRST_NAME + " " + LAST_NAME;
		assertThat(person.getFullName()).isEqualTo(expectedFullName);

	}

	@Test
	void findDynamicProjectionForPersonSummary() {
		Collection<PersonSummary> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, PersonSummary.class);
		assertThat(people).hasSize(1);

		PersonSummary person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);
		assertThat(person.getAddress()).isNotNull();

		PersonSummary.AddressSummary address = person.getAddress();
		assertThat(address.getCity()).isEqualTo(CITY);

	}

	@Test
	void findDynamicProjectionForNamesOnlyDto() {
		Collection<NamesOnlyDto> people = repository.findByLastNameAndFirstName(LAST_NAME, FIRST_NAME, NamesOnlyDto.class);
		assertThat(people).hasSize(1);

		NamesOnlyDto person = people.iterator().next();
		assertThat(person.getFirstName()).isEqualTo(FIRST_NAME);
		assertThat(person.getLastName()).isEqualTo(LAST_NAME);

	}

	public interface ProjectionPersonRepository extends Neo4jRepository<Person, Long> {

		Collection<NamesOnly> findByLastName(String lastName);

		Collection<PersonSummary> findByFirstName(String firstName);

		Collection<NamesOnlyDto> findByFirstNameAndLastName(String firstName, String lastName);

		<T> Collection<T> findByLastNameAndFirstName(String lastName, String firstName, Class<T> projectionClass);
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

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
