/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.integration.constructors;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.assertj.core.util.DateUtil;
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
import org.springframework.data.neo4j.conversion.MetaDataDrivenConversionService;
import org.springframework.data.neo4j.integration.constructors.domain.*;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.util.IterableUtils;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Nicolas Mervaillie
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

	@Test
	@Ignore("final fields are not supported by OGM for now")
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
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.integration.constructors.domain");
		}
	}

	@Repository
	public interface PersonRepository extends Neo4jRepository<Person, String> {}

	@Repository
	public interface KotlinPersonRepository extends Neo4jRepository<KotlinPerson, String> {}

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
