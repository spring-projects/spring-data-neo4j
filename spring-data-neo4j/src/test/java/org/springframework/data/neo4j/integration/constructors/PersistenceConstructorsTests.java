/*
 * Copyright 2011-2020 the original author or authors.
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
package org.springframework.data.neo4j.integration.constructors;

import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import org.assertj.core.util.DateUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.exception.core.MappingException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.integration.constructors.domain.*;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = PersistenceConstructorsTests.PersistenceConstructorsPersistenceContext.class)
public class PersistenceConstructorsTests {

	@Autowired GraphDatabaseService graphDatabaseService;

	@Autowired Session session;

	@Autowired Neo4jMappingContext ctx;

	@Autowired PersonRepository personRepository;

	@Autowired PersonWithFinalNameRepository pfnRepository;

	@Autowired PersonMultipleConstructorsRepository pmcRepository;

	@Autowired PersonWithAnnotatedPersistenceConstructorRepository papcRepository;

	@Autowired PersonWithConverterRepository pwcRepository;

	@Autowired PersonWithCompositeAttributeRepository pwcaRepository;

	@Autowired PersonWithManyToOneRelRepository manyToOneRepository;

	@Autowired KotlinPersonRepository kotlinRepository;

	@Autowired KotlinDataPersonRepository kotlinDataPersonRepository;

	@Autowired TransactionTemplate transactionTemplate;

	@Test
	public void shouldHandleSimpleEntityWithConstructor() {

		Person person = new Person("foo");
		personRepository.save(person);
		session.clear();

		List<Person> persons = iterableToList(personRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.get(0).getName()).isEqualTo("foo");
	}

	@Test
	public void shouldHandleRelationshipEntityWithConstructor() {

		Person person = new Person("foo");
		Person friend = new Person("bar");
		person.addFriend(friend);
		personRepository.save(person);
		session.clear();

		List<Person> persons = iterableToList(personRepository.findAll());
		assertThat(persons.size()).isEqualTo(2);
		Person person1 = persons.stream().filter(p -> p.equals(person)).findFirst().get();
		assertThat(person1.getName()).isEqualTo("foo");
		assertThat(person1.getFriendships().size()).isEqualTo(1);

		Friendship friendship = person.getFriendships().get(0);
		assertThat(friendship.getPersonStartNode()).isEqualTo(person);
		assertThat(friendship.getPersonEndNode()).isEqualTo(friend);
		assertThat(DateUtil.timeDifference(friendship.getTimestamp(), new Date()) < 1000)
				.isTrue();
		assertThat(friendship.getLocation()).isEqualTo(new Point(1, 2));
	}

	@Test
	public void shouldThrowAnExplicitExceptionIfMultipleConstructors() {

		PersonMultipleConstructors pmc = new PersonMultipleConstructors("foo");
		pmcRepository.save(pmc);
		session.clear();

		try {
			pmcRepository.findAll();
			fail("Should have raised an exception");
		} catch (InvalidDataAccessApiUsageException e) {
			assertThat(e.getCause().getClass()).isEqualTo(MappingException.class);
		}
	}

	@Test
	public void shouldUseAnnotatedPersistenceConstructor() {

		PersonWithAnnotatedPersistenceConstructor pmc = new PersonWithAnnotatedPersistenceConstructor("foo", "bar");
		papcRepository.save(pmc);
		session.clear();

		List<PersonWithAnnotatedPersistenceConstructor> persons = iterableToList(papcRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		PersonWithAnnotatedPersistenceConstructor person = persons.get(0);
		assertThat(person.getFirstName()).isEqualTo("foo");
		assertThat(person.getLastName()).isEqualTo("bar");
	}

	// This test is not really representative because atm relations are not passed to constructor by OGM
	// they are populated in a second step, after object instantiation
	@Test
	public void shouldHandleRelationshipInCtor() {
		PersonWithManyToOneRel person = new PersonWithManyToOneRel("foo", new Group("ADMIN"));
		manyToOneRepository.save(person);
		session.clear();

		List<PersonWithManyToOneRel> persons = iterableToList(manyToOneRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.get(0).getName()).isEqualTo("foo");
		assertThat(persons.get(0).getGroup().getName()).isEqualTo("ADMIN");
	}

	@Test
	@Ignore("final fields are not supported by OGM for now")
	public void shouldHandleFinalFields() {
		PersonWithFinalName person = new PersonWithFinalName("foo");
		pfnRepository.save(person);
		session.clear();

		List<PersonWithFinalName> persons = iterableToList(pfnRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.get(0).getName()).isEqualTo("foo");
	}

	@Test
	public void shouldHandleTypeConversion() {
		Date date = new Date();
		PersonWithConverter person = new PersonWithConverter("foo", date);
		pwcRepository.save(person);
		session.clear();

		List<PersonWithConverter> persons = iterableToList(pwcRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.get(0).getName()).isEqualTo("foo");
		assertThat(persons.get(0).getBirthDate()).isEqualTo(date);
	}

	@Test
	public void shouldHandleCompositeAttributes() {
		Point location = new Point(1.0, 2.0);
		PersonWithCompositeAttribute person = new PersonWithCompositeAttribute("foo", location);
		pwcaRepository.save(person);
		session.clear();

		List<PersonWithCompositeAttribute> persons = iterableToList(pwcaRepository.findAll());
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.get(0).getName()).isEqualTo("foo");
		assertThat(persons.get(0).getLocation()).isEqualTo(location);
	}

	@Test
	public void shouldSupportKotlinDataClasses() {

		KotlinPerson person = new KotlinPerson("foo", new ArrayList<>());
		kotlinRepository.save(person);
		session.clear();

		Collection<KotlinPerson> persons = session.loadAll(KotlinPerson.class);
		assertThat(persons.size()).isEqualTo(1);
		assertThat(persons.iterator().next().getName()).isEqualTo("foo");
	}

	@Test // DATAGRAPH-1220
	public void shouldSupportKotlinDataClassesWithGeneratedIds() {

		KotlinDataPerson person0 = KotlinPersonKt.newDataPerson("Not to be persisted");

		// Make sure we don't interfere with OGMs session cache but also don't clean it externally

		KotlinDataPerson person1 = transactionTemplate
				.execute(t -> kotlinDataPersonRepository.save(KotlinPersonKt.newDataPerson("Data Person 1")));
		KotlinDataPerson person2 = transactionTemplate
				.execute(t -> kotlinDataPersonRepository.save(KotlinPersonKt.newDataPerson("Data Person 2")));

		// Assert that the id is actually null without database interaction
		assertThat(person0.getId()).isNull();
		assertThat(person1.getId()).isNotNull();
		assertThat(person2.getId()).isNotNull();

		// Load the rest
		transactionTemplate.execute(t -> {
			List<KotlinDataPerson> loadedPersons = kotlinDataPersonRepository.findAll();
			assertThat(loadedPersons.size()).isEqualTo(2);
			assertThat(loadedPersons).contains(person1, person2);
			return null;
		});

		transactionTemplate.execute(t -> {
			Optional<KotlinDataPerson> loadedPerson1 = kotlinDataPersonRepository.findById(person1.getId());
			Optional<KotlinDataPerson> loadedPerson2 = kotlinDataPersonRepository.findById(person2.getId());

			assertThat(loadedPerson1.isPresent()).isTrue();
			assertThat(loadedPerson2.isPresent()).isTrue();
			return null;
		});

		// Double check the finder by another index field
		transactionTemplate.execute(t -> {

			Optional<KotlinDataPerson> loadedPerson1 = kotlinDataPersonRepository.findByName(person1.getName());
			assertThat(loadedPerson1.isPresent()).isTrue();
			return null;
		});
	}

	@Test
	public void shouldSupportQueryResults() {
		session.query(
				"MERGE (buffy:Person {name: 'Buffy'}) - [:IS_FRIEND {timestamp: 1533570712595, latitude: 34.10367, longitude: -118.272562}] -> (spike:Person {name: 'Spike'})\n"
						+ "MERGE (willow:Person {name: 'Willow'})\n" + "MERGE (xander:Person {name: 'Xander'})\n"
						+ "MERGE (buffy) - [:IS_FRIEND {timestamp: 1533570712595, latitude: 34.10367, longitude: -118.272562}] -> (willow)\n"
						+ "MERGE (willow) - [:IS_FRIEND {timestamp: 1533570712595, latitude: 34.10367, longitude: -118.272562}] -> (buffy)\n"
						+ "MERGE (buffy) - [:IS_FRIEND {timestamp: 1533570712595, latitude: 34.10367, longitude: -118.272562}] -> (xander)\n"
						+ "MERGE (xander) - [:IS_FRIEND {timestamp: 1533570712595, latitude: 34.10367, longitude: -118.272562}] -> (buffy)",
				new HashMap<>());

		List<PersonProjection> personsWithMutualFriends = personRepository.findPersonWithMutualFriendsByName("Buffy");
		assertThat(personsWithMutualFriends.size()).isEqualTo(1);
		assertThat(personsWithMutualFriends.get(0).getNumberOfFriends()).isEqualTo(3L);

		List<String> namesOfMutalFriends = personsWithMutualFriends.get(0).getMutualFriends().stream().map(Person::getName)
				.collect(toList());
		assertThat(namesOfMutalFriends.containsAll(Arrays.asList("Willow", "Xander")))
				.isTrue();
	}

	@Before
	public void setUp() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Repository
	public interface PersonRepository extends Neo4jRepository<Person, String> {
		@Query("MATCH (n:Person {name: $name}) \n" + "WITH n, size((n) - [:IS_FRIEND] -> ()) AS numberOfFriends\n"
				+ "MATCH (n) - [:IS_FRIEND] -> (m:Person) - [:IS_FRIEND] -> (n)\n"
				+ "RETURN n.name AS name, numberOfFriends,  collect(m) AS mutualFriends")
		List<PersonProjection> findPersonWithMutualFriendsByName(@Param("name") String name);
	}

	@Repository
	public interface KotlinPersonRepository extends Neo4jRepository<KotlinPerson, String> {}

	@Repository
	public interface KotlinDataPersonRepository extends Neo4jRepository<KotlinDataPerson, Long> {

		List<KotlinDataPerson> findAll();

		Optional<KotlinDataPerson> findByName(String name);
	}

	@Repository
	public interface PersonMultipleConstructorsRepository extends Neo4jRepository<PersonMultipleConstructors, String> {}

	@Repository
	public interface PersonWithAnnotatedPersistenceConstructorRepository
			extends Neo4jRepository<PersonWithAnnotatedPersistenceConstructor, String> {}

	@Repository
	public interface PersonWithConverterRepository extends Neo4jRepository<PersonWithConverter, String> {}

	@Repository
	public interface PersonWithCompositeAttributeRepository
			extends Neo4jRepository<PersonWithCompositeAttribute, String> {}

	@Repository
	public interface PersonWithFinalNameRepository extends Neo4jRepository<PersonWithFinalName, String> {}

	@Repository
	public interface PersonWithManyToOneRelRepository extends Neo4jRepository<PersonWithManyToOneRel, String> {}

	private static <T> List<T> iterableToList(Iterable<T> iterable) {
		return StreamSupport.stream(iterable.spliterator(), false).collect(toList());
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.integration.constructors.domain",
			repositoryPackages = "org.springframework.data.neo4j.integration.constructors", considerNestedRepositories = true)
	static class PersistenceConstructorsPersistenceContext {

		@Bean
		public ConversionService conversionService(SessionFactory sessionFactory) {
			return new MetaDataDrivenConversionService(sessionFactory.metaData());
		}
	}
}
