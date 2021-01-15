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
package org.springframework.data.neo4j.integration.constructors;

import static java.util.stream.Collectors.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.assertj.core.util.DateUtil;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.exception.core.MappingException;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
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
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { PersistenceConstructorsTests.PersistenceConstructorsPersistenceContext.class })
public class PersistenceConstructorsTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager platformTransactionManager;

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

		List<Person> persons = IterableUtils.toList(personRepository.findAll());
		assertEquals(1, persons.size());
		assertEquals("foo", persons.get(0).getName());
	}

	@Test
	public void shouldHandleRelationshipEntityWithConstructor() {

		Person person = new Person("foo");
		Person friend = new Person("bar");
		person.addFriend(friend);
		personRepository.save(person);
		session.clear();

		List<Person> persons = IterableUtils.toList(personRepository.findAll());
		assertEquals(2, persons.size());
		Person person1 = persons.stream().filter(p -> p.equals(person)).findFirst().get();
		assertEquals("foo", person1.getName());
		assertEquals(1, person1.getFriendships().size());

		Friendship friendship = person.getFriendships().get(0);
		assertEquals(person, friendship.getPersonStartNode());
		assertEquals(friend, friendship.getPersonEndNode());
		assertTrue(DateUtil.timeDifference(friendship.getTimestamp(), new Date()) < 1000);
		assertEquals(new Point(1, 2), friendship.getLocation());
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
			assertEquals(MappingException.class, e.getCause().getClass());
		}
	}

	@Test
	public void shouldUseAnnotatedPersistenceConstructor() {

		PersonWithAnnotatedPersistenceConstructor pmc = new PersonWithAnnotatedPersistenceConstructor("foo", "bar");
		papcRepository.save(pmc);
		session.clear();

		List<PersonWithAnnotatedPersistenceConstructor> persons = IterableUtils.toList(papcRepository.findAll());
		assertEquals(1, persons.size());
		PersonWithAnnotatedPersistenceConstructor person = persons.get(0);
		assertEquals("foo", person.getFirstName());
		assertEquals("bar", person.getLastName());
	}

	// This test is not really representative because atm relations are not passed to constructor by OGM
	// they are populated in a second step, after object instantiation
	@Test
	public void shouldHandleRelationshipInCtor() {
		PersonWithManyToOneRel person = new PersonWithManyToOneRel("foo", new Group("ADMIN"));
		manyToOneRepository.save(person);
		session.clear();

		List<PersonWithManyToOneRel> persons = IterableUtils.toList(manyToOneRepository.findAll());
		assertEquals(1, persons.size());
		assertEquals("foo", persons.get(0).getName());
		assertEquals("ADMIN", persons.get(0).getGroup().getName());
	}

	@Test // GH-1763
	public void shouldHandleFinalFields() {
		PersonWithFinalName person = new PersonWithFinalName("foo");
		pfnRepository.save(person);
		session.clear();

		List<PersonWithFinalName> persons = IterableUtils.toList(pfnRepository.findAll());
		assertEquals(1, persons.size());
		assertEquals("foo", persons.get(0).getName());
	}

	@Test
	public void shouldHandleTypeConversion() {
		Date date = new Date();
		PersonWithConverter person = new PersonWithConverter("foo", date);
		pwcRepository.save(person);
		session.clear();

		List<PersonWithConverter> persons = IterableUtils.toList(pwcRepository.findAll());
		assertEquals(1, persons.size());
		assertEquals("foo", persons.get(0).getName());
		assertEquals(date, persons.get(0).getBirthDate());
	}

	@Test
	public void shouldHandleCompositeAttributes() {
		Point location = new Point(1.0, 2.0);
		PersonWithCompositeAttribute person = new PersonWithCompositeAttribute("foo", location);
		pwcaRepository.save(person);
		session.clear();

		List<PersonWithCompositeAttribute> persons = IterableUtils.toList(pwcaRepository.findAll());
		assertEquals(1, persons.size());
		assertEquals("foo", persons.get(0).getName());
		assertEquals(location, persons.get(0).getLocation());
	}

	@Test
	public void shouldSupportKotlinDataClasses() {

		KotlinPerson person = new KotlinPerson("foo", new ArrayList<>());
		kotlinRepository.save(person);
		session.clear();

		Collection<KotlinPerson> persons = session.loadAll(KotlinPerson.class);
		assertEquals(1, persons.size());
		assertEquals("foo", persons.iterator().next().getName());
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
		assertThat(person0.getId(), is(nullValue()));
		assertThat(person1.getId(), is(notNullValue()));
		assertThat(person2.getId(), is(notNullValue()));

		// Load the rest
		transactionTemplate.execute(t -> {
			List<KotlinDataPerson> loadedPersons = kotlinDataPersonRepository.findAll();
			assertThat(loadedPersons.size(), is(2));
			assertThat(loadedPersons, CoreMatchers.hasItems(person1, person2));
			return null;
		});

		transactionTemplate.execute(t -> {
			Optional<KotlinDataPerson> loadedPerson1 = kotlinDataPersonRepository.findById(person1.getId());
			Optional<KotlinDataPerson> loadedPerson2 = kotlinDataPersonRepository.findById(person2.getId());

			assertThat(loadedPerson1.isPresent(), is(true));
			assertThat(loadedPerson2.isPresent(), is(true));
			return null;
		});

		// Double check the finder by another index field
		transactionTemplate.execute(t -> {

			Optional<KotlinDataPerson> loadedPerson1 = kotlinDataPersonRepository.findByName(person1.getName());
			assertThat(loadedPerson1.isPresent(), is(true));
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
		assertEquals(1, personsWithMutualFriends.size());
		assertEquals(3L, personsWithMutualFriends.get(0).getNumberOfFriends());

		List<String> namesOfMutalFriends = personsWithMutualFriends.get(0).getMutualFriends().stream().map(Person::getName)
				.collect(toList());
		assertTrue(namesOfMutalFriends.containsAll(Arrays.asList("Willow", "Xander")));
	}

	@Before
	public void setUp() {
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = { PersonRepository.class }, considerNestedRepositories = true)
	@EnableTransactionManagement
	static class PersistenceConstructorsPersistenceContext {

		@Bean
		public ConversionService conversionService() {
			return new MetaDataDrivenConversionService(sessionFactory().metaData());
		}

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.integration.constructors.domain");
		}
	}

	@Repository
	public interface PersonRepository extends Neo4jRepository<Person, String> {
		@Query("MATCH (n:Person {name: $name}) \n"
				+ "WITH n, size((n) - [:IS_FRIEND] -> ()) AS numberOfFriends\n"
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
}
