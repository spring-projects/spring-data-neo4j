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
package org.springframework.data.neo4j.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(Neo4jExtension.class)
@ContextConfiguration(classes = RepositoryIT.Config.class)
class RepositoryIT {
	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";
	private static final String TEST_PERSON1_FIRST_NAME = "Ernie";
	private static final String TEST_PERSON2_FIRST_NAME = "Bert";
	private static final String TEST_PERSON_SAMEVALUE = "SameValue";

	static PersonWithAllConstructor personExample(String sameValue) {
		return new PersonWithAllConstructor(null, null, null, sameValue, null);
	}

	private static Neo4jConnectionSupport neo4jConnectionSupport;

	private final PersonRepository repository;
	private final ThingRepository thingRepository;
	private final Driver driver;
	private Long id1;
	private Long id2;
	private PersonWithAllConstructor person1;
	private PersonWithAllConstructor person2;

	@Autowired RepositoryIT(PersonRepository repository, ThingRepository thingRepository, Driver driver) {

		this.repository = repository;
		this.thingRepository = thingRepository;
		this.driver = driver;
	}

	@BeforeEach
	void setupData() {

		Transaction transaction = driver.session().beginTransaction();
		transaction.run("MATCH (n) detach delete n");

		id1 = transaction.run(
			"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool return id(n)",
			Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
				TEST_PERSON1_FIRST_NAME, "cool", true)
		).next().get(0).asLong();
		id2 = transaction.run(
			"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool return id(n)",
			Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
				TEST_PERSON2_FIRST_NAME, "cool", false)
		).next().get(0).asLong();
		transaction.run("CREATE (n:PersonWithNoConstructor) SET n.name = $name, n.first_name = $firstName",
			Values.parameters("name", TEST_PERSON1_NAME, "firstName", TEST_PERSON1_FIRST_NAME));
		transaction.run("CREATE (n:PersonWithWither) SET n.name = '" + TEST_PERSON1_NAME + "'");
		transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})");
		transaction.success();
		transaction.close();

		// note that there is no id setting in the mapping right now
		person1 = new PersonWithAllConstructor(null, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			true);
		person2 = new PersonWithAllConstructor(null, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			false);
	}

	@Test
	void findAll() {
		Iterable<PersonWithAllConstructor> people = repository.findAll();
		assertThat(people).hasSize(2);
		assertThat(people).extracting("name").containsExactlyInAnyOrder(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
	}

	@Test
	void findById() {
		Optional<PersonWithAllConstructor> person = repository.findById(id1);
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void existsById() {
		boolean exists = repository.existsById(id1);
		assertThat(exists).isTrue();
	}

	@Test
	void findAllById() {
		Iterable<PersonWithAllConstructor> persons = repository.findAllById(Arrays.asList(id1, id2));
		assertThat(persons).hasSize(2);
	}

	@Test
	void findByAssignedId() {
		Optional<ThingWithAssignedId> optionalThing = thingRepository.findById("anId");
		assertThat(optionalThing).isPresent();
		assertThat(optionalThing).map(ThingWithAssignedId::getTheId).contains("anId");
		assertThat(optionalThing).map(ThingWithAssignedId::getName).contains("Homer");
	}

	@Test
	void count() {
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void findAllWithSortByOrderDefault() {
		Iterable<PersonWithAllConstructor> persons = repository.findAll(Sort.by("name"));

		assertThat(persons).containsExactly(person1, person2);
	}

	@Test
	void findAllWithSortByOrderAsc() {
		Iterable<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.asc("name")));

		assertThat(persons).containsExactly(person1, person2);
	}

	@Test
	void findAllWithSortByOrderDesc() {
		Iterable<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.desc("name")));

		assertThat(persons).containsExactly(person2, person1);
	}

	@Test
	void findAllWithPageable() {
		Sort sort = Sort.by("name");
		int page = 0;
		int limit = 1;
		Page<PersonWithAllConstructor> persons = repository.findAll(PageRequest.of(page, limit, sort));

		assertThat(persons).containsExactly(person1);

		page = 1;
		persons = repository.findAll(PageRequest.of(page, limit, sort));
		assertThat(persons).containsExactly(person2);
	}

	@Test
	void findOneByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1, ExampleMatcher.matchingAll().withIgnoreNullValues());
		Optional<PersonWithAllConstructor> person = repository.findOne(example);

		assertThat(person).isPresent();
		assertThat(person.get()).isEqualTo(person1);
	}

	@Test
	void findAllByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1, ExampleMatcher.matchingAll().withIgnoreNullValues());
		Iterable<PersonWithAllConstructor> persons = repository.findAll(example);

		assertThat(persons).containsExactly(person1);
	}

	@Test
	void findAllByExampleWithSort() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
		Iterable<PersonWithAllConstructor> persons = repository.findAll(example, Sort.by(Sort.Direction.DESC, "name"));

		assertThat(persons).containsExactly(person2, person1);
	}

	@Test
	void findAllByExampleWithPagination() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
		Iterable<PersonWithAllConstructor> persons = repository.findAll(example, PageRequest.of(1, 1, Sort.by("name")));

		assertThat(persons).containsExactly(person2);
	}

	@Test
	void existsByExample() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
		boolean exists = repository.exists(example);

		assertThat(exists).isTrue();
	}

	@Test
	void countByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1);
		long count = repository.count(example);

		assertThat(count).isEqualTo(1);
	}

	@Test
	void loadAllPersonsWithAllConstructor() {
		List<PersonWithAllConstructor> persons = repository.getAllPersonsViaQuery();

		assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
	}

	@Test
	void loadOnePersonWithAllConstructor() {
		PersonWithAllConstructor person = repository.getOnePersonViaQuery();
		assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadOptionalPersonWithAllConstructor() {
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonsViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadAllPersonsWithNoConstructor() {
		List<PersonWithNoConstructor> persons = repository.getAllPersonsWithNoConstructorViaQuery();

		assertThat(persons)
			.extracting(PersonWithNoConstructor::getName, PersonWithNoConstructor::getFirstName)
			.containsExactlyInAnyOrder(
				Tuple.tuple(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME)
			);
	}

	@Test
	void loadOnePersonWithNoConstructor() {
		PersonWithNoConstructor person = repository.getOnePersonWithNoConstructorViaQuery();
		assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
		assertThat(person.getFirstName()).isEqualTo(TEST_PERSON1_FIRST_NAME);
	}

	@Test
	void loadOptionalPersonWithNoConstructor() {
		Optional<PersonWithNoConstructor> person = repository.getOptionalPersonsWithNoConstructorViaQuery();
		assertThat(person).isPresent();
		assertThat(person).map(PersonWithNoConstructor::getName).contains(TEST_PERSON1_NAME);
		assertThat(person).map(PersonWithNoConstructor::getFirstName).contains(TEST_PERSON1_FIRST_NAME);
	}

	@Test
	void loadAllPersonsWithWither() {
		List<PersonWithWither> persons = repository.getAllPersonsWithWitherViaQuery();

		assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
	}

	@Test
	void loadOnePersonWithWither() {
		PersonWithWither person = repository.getOnePersonWithWitherViaQuery();
		assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadOptionalPersonWithWither() {
		Optional<PersonWithWither> person = repository.getOptionalPersonsWithWitherViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void callCustomCypher() {
		Long fixedLong = repository.customQuery();
		assertThat(fixedLong).isEqualTo(1L);
	}

	@Test
	void findBySimpleProperty() {

		List<PersonWithAllConstructor> persons = repository.findAllBySameValue(TEST_PERSON_SAMEVALUE);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findBySimplePropertiesAnded() {

		Optional<PersonWithAllConstructor> optionalPerson = repository.findOneByNameAndFirstName(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME);
		assertThat(optionalPerson).isPresent().contains(person1);
	}

	@Test
	void findBySimplePropertiesOred() {

		List<PersonWithAllConstructor> persons = repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findByNegatedSimpleProperty() {

		List<PersonWithAllConstructor> persons = repository.findAllByNameNot(TEST_PERSON1_NAME);
		assertThat(persons).doesNotContain(person1);
	}

	@Test
	void findByTrueAndFalse() {

		List<PersonWithAllConstructor> coolPeople = repository.findAllByCoolTrue();
		List<PersonWithAllConstructor> theRest = repository.findAllByCoolFalse();
		assertThat(coolPeople).doesNotContain(person2);
		assertThat(theRest).doesNotContain(person1);
	}

	@Test
	void findByLike() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameLike("Ern");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByMatches() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameMatches("(?i)ern.*");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByNotLike() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameNotLike("Ern");
		assertThat(persons).doesNotContain(person1);
	}

	@Test
	void findByStartingWith() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameStartingWith("Er");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByContaining() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameContaining("ni");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByNotContaining() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameNotContaining("ni");
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByEndingWith() {

		List<PersonWithAllConstructor> persons = repository.findAllByFirstNameContaining("nie");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.openConnection();
		}

		@Bean
		public NodeManagerFactory nodeManagerFactory(Driver driver) {

			return new NodeManagerFactory(driver, PersonWithAllConstructor.class, PersonWithNoConstructor.class,
				PersonWithWither.class, ThingWithAssignedId.class);
		}

		@Bean
		public PlatformTransactionManager transactionManager(Driver driver) {

			return new Neo4jTransactionManager(driver);
		}
	}
}
