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
package org.springframework.data.neo4j.integration.reactive;

import static java.util.Collections.*;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.driver.Values.*;
import static org.springframework.data.neo4j.test.Neo4jExtension.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.reactive.repositories.ReactivePersonRepository;
import org.springframework.data.neo4j.integration.reactive.repositories.ReactiveThingRepository;
import org.springframework.data.neo4j.integration.shared.*;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Philipp Tölle
 */
@ExtendWith(Neo4jExtension.class)
@SpringJUnitConfig
@DirtiesContext
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveRepositoryIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;
	protected static DatabaseSelection databaseSelection = DatabaseSelection.undecided();

	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";
	private static final String TEST_PERSON1_FIRST_NAME = "Ernie";
	private static final String TEST_PERSON2_FIRST_NAME = "Bert";
	private static final LocalDate TEST_PERSON1_BORN_ON = LocalDate.of(2019, 1, 1);
	private static final LocalDate TEST_PERSON2_BORN_ON = LocalDate.of(2019, 2, 1);
	private static final String TEST_PERSON_SAMEVALUE = "SameValue";
	private static final Point NEO4J_HQ = Values.point(4326, 12.994823, 55.612191).asPoint();
	private static final Point SFO = Values.point(4326, -122.38681, 37.61649).asPoint();
	private static final long NOT_EXISTING_NODE_ID = 3123131231L;

	static PersonWithAllConstructor personExample(String sameValue) {
		return new PersonWithAllConstructor(null, null, null, sameValue, null, null, null, null, null, null, null);
	}

	private long id1;
	private long id2;
	private PersonWithAllConstructor person1;
	private PersonWithAllConstructor person2;

	ReactiveRepositoryIT() {
		databaseSelection = DatabaseSelection.undecided();
	}

	@Nested
	class Find extends ReactiveIntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {

			transaction.run("MATCH (n) detach delete n");

			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place "
					+ "RETURN id(n)",
					parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ))
					.next().get(0).asLong();

			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");
			IntStream.rangeClosed(1, 20).forEach(i -> transaction
					.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})", parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, null);

			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void findAll(@Autowired ReactivePersonRepository repository) {

			List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

			StepVerifier.create(repository.findAll()).expectNextMatches(personList::contains)
					.expectNextMatches(personList::contains).verifyComplete();
		}

		@Test
		void findById(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findById(id1)).expectNext(person1).verifyComplete();
		}

		@Test
		void findWithPageable(@Autowired ReactivePersonRepository repository) {

			Sort sort = Sort.by("name");
			int page = 0;
			int limit = 1;

			StepVerifier.create(repository.findByNameStartingWith("Test", PageRequest.of(page, limit, sort)))
					.assertNext(person -> assertThat(person).isEqualTo(person1)).verifyComplete();

			sort = Sort.by("name");
			page = 1;
			limit = 1;

			StepVerifier.create(repository.findByNameStartingWith("Test", PageRequest.of(page, limit, sort)))
					.assertNext(person -> assertThat(person).isEqualTo(person2)).verifyComplete();
		}

		@Test
		void findAllByIds(@Autowired ReactivePersonRepository repository) {

			List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

			StepVerifier.create(repository.findAllById(Arrays.asList(id1, id2))).expectNextMatches(personList::contains)
					.expectNextMatches(personList::contains).verifyComplete();
		}

		@Test
		void findAllByIdsPublisher(@Autowired ReactivePersonRepository repository) {

			List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

			StepVerifier.create(repository.findAllById(Flux.just(id1, id2))).expectNextMatches(personList::contains)
					.expectNextMatches(personList::contains).verifyComplete();
		}

		@Test
		void findByIdNoMatch(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findById(NOT_EXISTING_NODE_ID)).verifyComplete();
		}

		@Test
		void findByIdPublisher(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findById(Mono.just(id1))).expectNext(person1).verifyComplete();
		}

		@Test
		void findByIdPublisherNoMatch(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findById(Mono.just(NOT_EXISTING_NODE_ID))).verifyComplete();
		}

		@Test
		void findAllWithSortByOrderDefault(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findAll(Sort.by("name"))).expectNext(person1, person2).verifyComplete();
		}

		@Test
		void findAllWithSortByOrderAsc(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findAll(Sort.by(Sort.Order.asc("name")))).expectNext(person1, person2)
					.verifyComplete();
		}

		@Test
		void findAllWithSortByOrderDesc(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.findAll(Sort.by(Sort.Order.desc("name")))).expectNext(person2, person1)
					.verifyComplete();
		}

		@Test
		void findOneByExample(@Autowired ReactivePersonRepository repository) {
			Example<PersonWithAllConstructor> example = Example.of(person1,
					ExampleMatcher.matchingAll().withIgnoreNullValues());

			StepVerifier.create(repository.findOne(example)).expectNext(person1).verifyComplete();
		}

		@Test
		void findAllByExample(@Autowired ReactivePersonRepository repository) {
			Example<PersonWithAllConstructor> example = Example.of(person1,
					ExampleMatcher.matchingAll().withIgnoreNullValues());
			StepVerifier.create(repository.findAll(example)).expectNext(person1).verifyComplete();
		}

		@Test
		void findAllByExampleWithDifferentMatchers(@Autowired ReactivePersonRepository repository) {
			PersonWithAllConstructor person;
			Example<PersonWithAllConstructor> example;

			person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME, TEST_PERSON2_FIRST_NAME, null, null, null, null,
					null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAny());

			StepVerifier.create(repository.findAll(example)).recordWith(ArrayList::new).expectNextCount(2)
					.expectRecordedMatches(recordedPersons -> recordedPersons.containsAll(Arrays.asList(person1, person2)))
					.verifyComplete();

			person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME.toUpperCase(), TEST_PERSON2_FIRST_NAME, null, null,
					null, null, null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAny().withIgnoreCase("name"));

			StepVerifier.create(repository.findAll(example)).recordWith(ArrayList::new).expectNextCount(2)
					.expectRecordedMatches(recordedPersons -> recordedPersons.containsAll(Arrays.asList(person1, person2)))
					.verifyComplete();

			person = new PersonWithAllConstructor(null,
					TEST_PERSON2_NAME.substring(TEST_PERSON2_NAME.length() - 2).toUpperCase(),
					TEST_PERSON2_FIRST_NAME.substring(0, 2), TEST_PERSON_SAMEVALUE.substring(3, 5), null, null, null, null, null,
					null, null);
			example = Example.of(person, ExampleMatcher.matchingAll()
					.withMatcher("name", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.ENDING, true))
					.withMatcher("firstName", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.STARTING))
					.withMatcher("sameValue", ExampleMatcher.GenericPropertyMatcher.of(ExampleMatcher.StringMatcher.CONTAINING)));

			StepVerifier.create(repository.findAll(example)).expectNext(person2).verifyComplete();

			person = new PersonWithAllConstructor(null, null, "(?i)ern.*", null, null, null, null, null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAll().withStringMatcher(ExampleMatcher.StringMatcher.REGEX));

			StepVerifier.create(repository.findAll(example)).expectNext(person1).verifyComplete();

			example = Example.of(person,
					ExampleMatcher.matchingAll().withStringMatcher(ExampleMatcher.StringMatcher.REGEX).withIncludeNullValues());

			StepVerifier.create(repository.findAll(example)).verifyComplete();
		}

		@Test
		void findAllByExampleWithSort(@Autowired ReactivePersonRepository repository) {
			Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));

			StepVerifier.create(repository.findAll(example, Sort.by(Sort.Direction.DESC, "name")))
					.expectNext(person2, person1).verifyComplete();
		}

		@Test
		void existsById(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.existsById(id1)).expectNext(true).verifyComplete();
		}

		@Test
		void existsByIdNoMatch(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.existsById(NOT_EXISTING_NODE_ID)).expectNext(false).verifyComplete();
		}

		@Test
		void existsByIdPublisher(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.existsById(id1)).expectNext(true).verifyComplete();
		}

		@Test
		void existsByIdPublisherNoMatch(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.existsById(NOT_EXISTING_NODE_ID)).expectNext(false).verifyComplete();
		}

		@Test
		void existsByExample(@Autowired ReactivePersonRepository repository) {
			Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
			StepVerifier.create(repository.exists(example)).expectNext(true).verifyComplete();

		}

		@Test
		void count(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
		}

		@Test
		void countByExample(@Autowired ReactivePersonRepository repository) {
			Example<PersonWithAllConstructor> example = Example.of(person1);
			StepVerifier.create(repository.count(example)).expectNext(1L).verifyComplete();
		}

		@Test
		void callCustomCypher(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.customQuery()).expectNext(1L).verifyComplete();
		}

		@Test
		void loadAllPersonsWithAllConstructor(@Autowired ReactivePersonRepository repository) {
			List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

			StepVerifier.create(repository.getAllPersonsViaQuery()).expectNextMatches(personList::contains)
					.expectNextMatches(personList::contains).verifyComplete();
		}

		@Test
		void loadOnePersonWithAllConstructor(@Autowired ReactivePersonRepository repository) {
			StepVerifier.create(repository.getOnePersonViaQuery()).expectNext(person1).verifyComplete();
		}

		@Test
		void findBySimplePropertiesAnded(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findOneByNameAndFirstName(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME))
					.expectNext(person1).verifyComplete();

			StepVerifier.create(repository.findOneByNameAndFirstNameAllIgnoreCase(TEST_PERSON1_NAME.toUpperCase(),
					TEST_PERSON1_FIRST_NAME.toUpperCase())).expectNext(person1).verifyComplete();

		}

		@Test
		void findBySimplePropertiesOred(@Autowired ReactivePersonRepository repository) {

			repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME).as(StepVerifier::create)
					.recordWith(ArrayList::new).expectNextCount(2)
					.expectRecordedMatches(recordedPersons -> recordedPersons.containsAll(Arrays.asList(person1, person2)))
					.verifyComplete();
		}

		@Test // GH-112
		void countBySimplePropertiesOred(@Autowired ReactivePersonRepository repository) {

			repository.countAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME).as(StepVerifier::create).expectNext(2L)
					.verifyComplete();
		}

		@Test
		void findBySimpleProperty(@Autowired ReactivePersonRepository repository) {
			List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

			StepVerifier.create(repository.findAllBySameValue(TEST_PERSON_SAMEVALUE)).expectNextMatches(personList::contains)
					.expectNextMatches(personList::contains).verifyComplete();
		}

		@Test
		void findByPropertyThatNeedsConversion(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findAllByPlace(new GeographicPoint2d(NEO4J_HQ.y(), NEO4J_HQ.x())))
					.expectNextCount(1).verifyComplete();
		}

		@Test
		void findByPropertyFailsIfNoConverterIsAvailable(@Autowired ReactivePersonRepository repository) {

			assertThatExceptionOfType(ConverterNotFoundException.class)
					.isThrownBy(() -> repository.findAllByPlace(new ThingWithGeneratedId("hello")))
					.withMessageStartingWith("No converter found capable of converting from type");
		}

		@Test
		void findByAssignedId(@Autowired ReactiveThingRepository repository) {

			StepVerifier.create(repository.findById("anId")).assertNext(thing -> {

				assertThat(thing.getTheId()).isEqualTo("anId");
				assertThat(thing.getName()).isEqualTo("Homer");

				AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
				anotherThing.setName("Bart");
				assertThat(thing.getThings()).containsExactlyInAnyOrder(anotherThing);
			}).verifyComplete();
		}

		@Test
		void loadWithAssignedIdViaQuery(@Autowired ReactiveThingRepository repository) {

			StepVerifier.create(repository.getViaQuery()).assertNext(thing -> {
				assertThat(thing.getTheId()).isEqualTo("anId");
				assertThat(thing.getName()).isEqualTo("Homer");

				AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
				anotherThing.setName("Bart");
				assertThat(thing.getThings()).containsExactly(anotherThing);
			}).verifyComplete();
		}

		@Test
		void findByConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:EntityWithConvertedId{identifyingEnum:'A'})");
			}

			StepVerifier.create(repository.findById(EntityWithConvertedId.IdentifyingEnum.A)).assertNext(entity -> {
				assertThat(entity).isNotNull();
				assertThat(entity.getIdentifyingEnum()).isEqualTo(EntityWithConvertedId.IdentifyingEnum.A);
			}).verifyComplete();
		}

		@Test
		void findAllByConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:EntityWithConvertedId{identifyingEnum:'A'})");
			}

			StepVerifier.create(repository.findAllById(singleton(EntityWithConvertedId.IdentifyingEnum.A)))
					.assertNext(
							entity -> assertThat(entity.getIdentifyingEnum()).isEqualTo(EntityWithConvertedId.IdentifyingEnum.A))
					.verifyComplete();
		}
	}

	@Nested
	class FindWithRelationships extends ReactiveIntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {

			transaction.run("MATCH (n) detach delete n");

			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place "
					+ "RETURN id(n)",
					parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ))
					.next().get(0).asLong();

			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");
			IntStream.rangeClosed(1, 20).forEach(i -> transaction
					.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})", parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, null);

			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void loadEntityWithRelationship(@Autowired ReactiveRelationshipRepository repository) {

			long personId;
			long clubId;
			long hobbyNode1Id;
			long hobbyNode2Id;
			long petNode1Id;
			long petNode2Id;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
								+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'}), "
								+ "(n)<-[:Has]-(c:Club{name:'ClownsClub'}), " + "(p1)-[:Has]->(h2:Hobby{name:'sleeping'}), "
								+ "(p1)-[:Has]->(p2)" + "RETURN n, h1, h2, p1, p2, c")
						.single();

				Node personNode = record.get("n").asNode();
				Node clubNode = record.get("c").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node hobbyNode2 = record.get("h2").asNode();
				Node petNode1 = record.get("p1").asNode();
				Node petNode2 = record.get("p2").asNode();

				personId = personNode.id();
				clubId = clubNode.id();
				hobbyNode1Id = hobbyNode1.id();
				hobbyNode2Id = hobbyNode2.id();
				petNode1Id = petNode1.id();
				petNode2Id = petNode2.id();
			}

			StepVerifier.create(repository.findById(personId)).assertNext(loadedPerson -> {

				assertThat(loadedPerson.getName()).isEqualTo("Freddie");
				Hobby hobby = loadedPerson.getHobbies();
				assertThat(hobby).isNotNull();
				assertThat(hobby.getId()).isEqualTo(hobbyNode1Id);
				assertThat(hobby.getName()).isEqualTo("Music");

				Club club = loadedPerson.getClub();
				assertThat(club).isNotNull();
				assertThat(club.getId()).isEqualTo(clubId);
				assertThat(club.getName()).isEqualTo("ClownsClub");

				List<Pet> pets = loadedPerson.getPets();
				Pet comparisonPet1 = new Pet(petNode1Id, "Jerry");
				Pet comparisonPet2 = new Pet(petNode2Id, "Tom");
				assertThat(pets).containsExactlyInAnyOrder(comparisonPet1, comparisonPet2);

				Pet pet1 = pets.get(pets.indexOf(comparisonPet1));
				Pet pet2 = pets.get(pets.indexOf(comparisonPet2));
				Hobby petHobby = pet1.getHobbies().iterator().next();
				assertThat(petHobby.getId()).isEqualTo(hobbyNode2Id);
				assertThat(petHobby.getName()).isEqualTo("sleeping");

				assertThat(pet1.getFriends()).containsExactly(pet2);
			}).verifyComplete();
		}

		@Test
		void loadEntityWithRelationshipToTheSameNode(@Autowired ReactiveRelationshipRepository repository) {

			long personId;
			long hobbyNode1Id;
			long petNode1Id;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
								+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), " + "(p1)-[:Has]->(h1)" + "RETURN n, h1, p1")
						.single();

				Node personNode = record.get("n").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node petNode1 = record.get("p1").asNode();

				personId = personNode.id();
				hobbyNode1Id = hobbyNode1.id();
				petNode1Id = petNode1.id();
			}

			StepVerifier.create(repository.findById(personId)).assertNext(loadedPerson -> {

				assertThat(loadedPerson.getName()).isEqualTo("Freddie");
				Hobby hobby = loadedPerson.getHobbies();
				assertThat(hobby).isNotNull();
				assertThat(hobby.getId()).isEqualTo(hobbyNode1Id);
				assertThat(hobby.getName()).isEqualTo("Music");

				List<Pet> pets = loadedPerson.getPets();
				Pet comparisonPet1 = new Pet(petNode1Id, "Jerry");
				assertThat(pets).containsExactlyInAnyOrder(comparisonPet1);

				Pet pet1 = pets.get(pets.indexOf(comparisonPet1));
				Hobby petHobby = pet1.getHobbies().iterator().next();
				assertThat(petHobby.getName()).isEqualTo("Music");

				assertThat(petHobby).isSameAs(hobby);
			}).verifyComplete();
		}

		@Test
		void loadDeepRelationships(@Autowired ReactiveDeepRelationshipRepository repository) {

			long type1Id;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE " + "(t1:Type1)-[:NEXT_TYPE]->(t2:Type2)-[:NEXT_TYPE]->(:Type3)-[:NEXT_TYPE]->(t4:Type4)-"
								+ "[:NEXT_TYPE]->(:Type5)-[:NEXT_TYPE]->(:Type6)-[:NEXT_TYPE]->(:Type7), " + "(t2)-[:SAME_TYPE]->"
								+ "(:Type2)-[:SAME_TYPE]->(:Type2)-[:SAME_TYPE]->(:Type2)-[:SAME_TYPE]->"
								+ "(:Type2)-[:SAME_TYPE]->(:Type2)-[:SAME_TYPE]->(:Type2)-[:SAME_TYPE]->" + "(:Type2) " + "RETURN t1")
						.single();

				type1Id = record.get("t1").asNode().id();
			}

			StepVerifier.create(repository.findById(type1Id)).assertNext(type1 -> {
				// ensures that the virtual limit for same relationships does not affect distinct relationships
				assertThat(type1.nextType.nextType.nextType.nextType.nextType.nextType).isNotNull();

				// assert that same type relationships not cause stack overflow
				DeepRelationships.Type2 type2 = type1.nextType;
				assertThat(type2.sameType.sameType.sameType).isNotNull();
				assertThat(type2.sameType.sameType.sameType.sameType).isNull();
			}).verifyComplete();
		}

		@Test
		void loadLoopingDeepRelationships(@Autowired ReactiveLoopingRelationshipRepository loopingRelationshipRepository) {

			long type1Id;

			try (Session session = createSession()) {
				Record record = session.run(
						"CREATE " + "(t1:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)-[:NEXT_TYPE]->(:LoopingType2)-[:NEXT_TYPE]->(:LoopingType3)-[:NEXT_TYPE]->"
								+ "(:LoopingType1)" + "RETURN t1")
						.single();

				type1Id = record.get("t1").asNode().id();
			}

			StepVerifier.create(loopingRelationshipRepository.findById(type1Id)).assertNext(type1 -> {
				DeepRelationships.LoopingType1 iteration1 = type1.nextType.nextType.nextType;
				assertThat(iteration1).isNotNull();
				DeepRelationships.LoopingType1 iteration2 = iteration1.nextType.nextType.nextType;
				assertThat(iteration2).isNotNull();
				DeepRelationships.LoopingType1 iteration3 = iteration2.nextType.nextType.nextType;
				assertThat(iteration3.nextType).isNull();
			}).verifyComplete();
		}

		@Test
		void loadEntityWithBidirectionalRelationship(@Autowired BidirectionalStartRepository repository) {

			long startId;

			try (Session session = createSession()) {
				Record record = session.run(
						"CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}) " + "RETURN n")
						.single();

				Node startNode = record.get("n").asNode();
				startId = startNode.id();
			}

			StepVerifier.create(repository.findById(startId)).assertNext(entity -> {
				assertThat(entity.getEnds()).hasSize(1);
			}).verifyComplete();
		}

		@Test
		void loadEntityWithBidirectionalRelationshipFromIncomingSide(@Autowired BidirectionalEndRepository repository) {

			long endId;

			try (Session session = createSession()) {
				Record record = session.run(
						"CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}) " + "RETURN e")
						.single();

				Node endNode = record.get("e").asNode();
				endId = endNode.id();
			}

			StepVerifier.create(repository.findById(endId)).assertNext(entity -> {
				assertThat(entity.getStart()).isNotNull();
			}).verifyComplete();
		}

		@Test
		void loadMultipleEntitiesWithRelationship(@Autowired ReactiveRelationshipRepository repository) {

			long hobbyNode1Id;
			long hobbyNode2Id;
			long petNode1Id;
			long petNode2Id;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h:Hobby{name:'Music'}), "
								+ "(n)-[:Has]->(p:Pet{name: 'Jerry'}) " + "RETURN n, h, p")
						.single();

				hobbyNode1Id = record.get("h").asNode().id();
				petNode1Id = record.get("p").asNode().id();

				record = session.run("CREATE (n:PersonWithRelationship{name:'SomeoneElse'})-[:Has]->(h:Hobby{name:'Music2'}), "
						+ "(n)-[:Has]->(p:Pet{name: 'Jerry2'}) " + "RETURN n, h, p").single();

				hobbyNode2Id = record.get("h").asNode().id();
				petNode2Id = record.get("p").asNode().id();
			}

			StepVerifier.create(repository.findAll()).recordWith(ArrayList::new).expectNextCount(2)
					.consumeRecordedWith(loadedPersons -> {

						Hobby hobby1 = new Hobby();
						hobby1.setId(hobbyNode1Id);
						hobby1.setName("Music");

						Hobby hobby2 = new Hobby();
						hobby2.setId(hobbyNode2Id);
						hobby2.setName("Music2");

						Pet pet1 = new Pet(petNode1Id, "Jerry");
						Pet pet2 = new Pet(petNode2Id, "Jerry2");

						assertThat(loadedPersons).extracting("name").containsExactlyInAnyOrder("Freddie", "SomeoneElse");
						assertThat(loadedPersons).extracting("hobbies").containsExactlyInAnyOrder(hobby1, hobby2);
						assertThat(loadedPersons).flatExtracting("pets").containsExactlyInAnyOrder(pet1, pet2);
					}).verifyComplete();
		}

		@Test
		void loadEntityWithRelationshipViaQuery(@Autowired ReactiveRelationshipRepository repository) {

			long personId;
			long hobbyNodeId;
			long petNode1Id;
			long petNode2Id;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
								+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'}) " + "RETURN n, h1, p1, p2")
						.single();

				Node personNode = record.get("n").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node petNode1 = record.get("p1").asNode();
				Node petNode2 = record.get("p2").asNode();

				personId = personNode.id();
				hobbyNodeId = hobbyNode1.id();
				petNode1Id = petNode1.id();
				petNode2Id = petNode2.id();
			}

			StepVerifier.create(repository.getPersonWithRelationshipsViaQuery()).assertNext(loadedPerson -> {
				assertThat(loadedPerson.getName()).isEqualTo("Freddie");
				assertThat(loadedPerson.getId()).isEqualTo(personId);
				Hobby hobby = loadedPerson.getHobbies();
				assertThat(hobby).isNotNull();
				assertThat(hobby.getId()).isEqualTo(hobbyNodeId);
				assertThat(hobby.getName()).isEqualTo("Music");

				List<Pet> pets = loadedPerson.getPets();
				Pet comparisonPet1 = new Pet(petNode1Id, "Jerry");
				Pet comparisonPet2 = new Pet(petNode2Id, "Tom");
				assertThat(pets).containsExactlyInAnyOrder(comparisonPet1, comparisonPet2);
			}).verifyComplete();
		}

		@Test
		void loadEntityWithRelationshipWithAssignedId(@Autowired ReactivePetRepository repository) {

			long petNodeId;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (p:Pet{name:'Jerry'})-[:Has]->(t:Thing{theId:'t1', name:'Thing1'}) " + "RETURN p, t").single();

				Node petNode = record.get("p").asNode();
				petNodeId = petNode.id();
			}

			StepVerifier.create(repository.findById(petNodeId)).assertNext(pet -> {
				ThingWithAssignedId relatedThing = pet.getThings().get(0);
				assertThat(relatedThing.getTheId()).isEqualTo("t1");
				assertThat(relatedThing.getName()).isEqualTo("Thing1");
			}).verifyComplete();
		}

		@Test
		void findEntityWithSelfReferencesInBothDirections(@Autowired ReactivePetRepository repository) {

			long petId;

			try (Session session = createSession()) {
				petId = session.run("CREATE (luna:Pet{name:'Luna'})-[:Has]->(daphne:Pet{name:'Daphne'})"
						+ "-[:Has]->(luna2:Pet{name:'Luna'})" + "RETURN id(luna) as id").single().get("id").asLong();
			}

			StepVerifier.create(repository.findById(petId)).assertNext(loadedPet -> {
				assertThat(loadedPet.getFriends().get(0).getName()).isEqualTo("Daphne");
				assertThat(loadedPet.getFriends().get(0).getFriends().get(0).getName()).isEqualTo("Luna");
			}).verifyComplete();
		}
	}

	@Nested
	class RelationshipProperties extends ReactiveIntegrationTestBase {

		@Test
		void loadEntityWithRelationshipWithProperties(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {

			long personId;
			long hobbyNode1Id;
			long hobbyNode2Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
						+ " (n)-[l1:LIKES"
						+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
						+ "]->(h1:Hobby{name:'Music'})," + " (n)-[l2:LIKES"
						+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
						+ "]->(h2:Hobby{name:'Something else'})" + "RETURN n, h1, h2").single();

				Node personNode = record.get("n").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node hobbyNode2 = record.get("h2").asNode();

				personId = personNode.id();
				hobbyNode1Id = hobbyNode1.id();
				hobbyNode2Id = hobbyNode2.id();
			}

			StepVerifier.create(repository.findById(personId)).assertNext(person -> {
				assertThat(person.getName()).isEqualTo("Freddie");

				Hobby hobby1 = new Hobby();
				hobby1.setName("Music");
				hobby1.setId(hobbyNode1Id);
				LikesHobbyRelationship rel1 = new LikesHobbyRelationship(1995);
				rel1.setActive(true);
				rel1.setLocalDate(LocalDate.of(1995, 2, 26));
				rel1.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING);
				rel1.setPoint(new CartesianPoint2d(0d, 1d));

				Hobby hobby2 = new Hobby();
				hobby2.setName("Something else");
				hobby2.setId(hobbyNode2Id);
				LikesHobbyRelationship rel2 = new LikesHobbyRelationship(2000);
				rel2.setActive(false);
				rel2.setLocalDate(LocalDate.of(2000, 6, 28));
				rel2.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING_DIFFERENT);
				rel2.setPoint(new CartesianPoint2d(2d, 3d));

				assertThat(person.getHobbies()).contains(MapEntry.entry(hobby1, rel1), MapEntry.entry(hobby2, rel2));
			}).verifyComplete();

		}

		@Test
		void saveEntityWithRelationshipWithProperties(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {
			// given
			Hobby h1 = new Hobby();
			h1.setName("Music");

			int rel1Since = 1995;
			boolean rel1Active = true;
			LocalDate rel1LocalDate = LocalDate.of(1995, 2, 26);
			LikesHobbyRelationship.MyEnum rel1MyEnum = LikesHobbyRelationship.MyEnum.SOMETHING;
			CartesianPoint2d rel1Point = new CartesianPoint2d(0.0, 1.0);

			LikesHobbyRelationship rel1 = new LikesHobbyRelationship(rel1Since);
			rel1.setActive(rel1Active);
			rel1.setLocalDate(rel1LocalDate);
			rel1.setMyEnum(rel1MyEnum);
			rel1.setPoint(rel1Point);

			Hobby h2 = new Hobby();
			h2.setName("Something else");
			int rel2Since = 2000;
			boolean rel2Active = false;
			LocalDate rel2LocalDate = LocalDate.of(2000, 6, 28);
			LikesHobbyRelationship.MyEnum rel2MyEnum = LikesHobbyRelationship.MyEnum.SOMETHING_DIFFERENT;
			CartesianPoint2d rel2Point = new CartesianPoint2d(2.0, 3.0);

			LikesHobbyRelationship rel2 = new LikesHobbyRelationship(rel2Since);
			rel2.setActive(rel2Active);
			rel2.setLocalDate(rel2LocalDate);
			rel2.setMyEnum(rel2MyEnum);
			rel2.setPoint(rel2Point);

			Map<Hobby, LikesHobbyRelationship> hobbies = new HashMap<>();
			hobbies.put(h1, rel1);
			hobbies.put(h2, rel2);
			PersonWithRelationshipWithProperties clonePerson = new PersonWithRelationshipWithProperties("Freddie clone");
			clonePerson.setHobbies(hobbies);

			// when
			Mono<PersonWithRelationshipWithProperties> operationUnderTest = repository.save(clonePerson);

			// then
			List<PersonWithRelationshipWithProperties> shouldBeDifferentPersons = new ArrayList<>();

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create)
					.recordWith(() -> shouldBeDifferentPersons).expectNextCount(1L).verifyComplete();

			assertThat(shouldBeDifferentPersons).size().isEqualTo(1);

			PersonWithRelationshipWithProperties shouldBeDifferentPerson = shouldBeDifferentPersons.get(0);
			assertThat(shouldBeDifferentPerson).isNotNull().isEqualToComparingOnlyGivenFields(clonePerson, "hobbies");
			assertThat(shouldBeDifferentPerson.getName()).isEqualToIgnoringCase("Freddie clone");

			// check content of db
			String matchQuery = "MATCH (n:PersonWithRelationshipWithProperties {name:'Freddie clone'}) " + "RETURN n, "
					+ "[(n) -[:LIKES]->(h:Hobby) |h] as Hobbies, " + "[(n) -[r:LIKES]->(:Hobby) |r] as rels";
			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()), s -> s.run(matchQuery).records(), RxSession::close)
					.as(StepVerifier::create).assertNext(record -> {

						assertThat(record.containsKey("n")).isTrue();
						assertThat(record.containsKey("Hobbies")).isTrue();
						assertThat(record.containsKey("rels")).isTrue();
						assertThat(record.values()).hasSize(3);
						assertThat(record.get("Hobbies").values()).hasSize(2);
						assertThat(record.get("rels").values()).hasSize(2);

						assertThat(record.get("rels").values(Value::asRelationship))
								.extracting(Relationship::type, rel -> rel.get("active"), rel -> rel.get("localDate"),
										rel -> rel.get("point"), rel -> rel.get("myEnum"), rel -> rel.get("since"))
								.containsExactlyInAnyOrder(
										tuple("LIKES", Values.value(rel1Active), Values.value(rel1LocalDate),
												Values.point(rel1Point.getSrid(), rel1Point.getX(), rel1Point.getY()),
												Values.value(rel1MyEnum.name()), Values.value(rel1Since)),
										tuple("LIKES", Values.value(rel2Active), Values.value(rel2LocalDate),
												Values.point(rel2Point.getSrid(), rel2Point.getX(), rel2Point.getY()),
												Values.value(rel2MyEnum.name()), Values.value(rel2Since)));
					}).verifyComplete();
		}

		@Test
		void loadEntityWithRelationshipWithPropertiesFromCustomQuery(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {

			long personId;
			long hobbyNode1Id;
			long hobbyNode2Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
						+ " (n)-[l1:LIKES"
						+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
						+ "]->(h1:Hobby{name:'Music'})," + " (n)-[l2:LIKES"
						+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
						+ "]->(h2:Hobby{name:'Something else'})" + "RETURN n, h1, h2").single();

				Node personNode = record.get("n").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node hobbyNode2 = record.get("h2").asNode();

				personId = personNode.id();
				hobbyNode1Id = hobbyNode1.id();
				hobbyNode2Id = hobbyNode2.id();
			}

			StepVerifier.create(repository.loadFromCustomQuery(personId)).assertNext(person -> {
				assertThat(person.getName()).isEqualTo("Freddie");

				Hobby hobby1 = new Hobby();
				hobby1.setName("Music");
				hobby1.setId(hobbyNode1Id);
				LikesHobbyRelationship rel1 = new LikesHobbyRelationship(1995);
				rel1.setActive(true);
				rel1.setLocalDate(LocalDate.of(1995, 2, 26));
				rel1.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING);
				rel1.setPoint(new CartesianPoint2d(0d, 1d));

				Hobby hobby2 = new Hobby();
				hobby2.setName("Something else");
				hobby2.setId(hobbyNode2Id);
				LikesHobbyRelationship rel2 = new LikesHobbyRelationship(2000);
				rel2.setActive(false);
				rel2.setLocalDate(LocalDate.of(2000, 6, 28));
				rel2.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING_DIFFERENT);
				rel2.setPoint(new CartesianPoint2d(2d, 3d));

				assertThat(person.getHobbies()).contains(MapEntry.entry(hobby1, rel1), MapEntry.entry(hobby2, rel2));
			}).verifyComplete();

		}

		@Test // DATAGRAPH-1350
		void loadEntityWithRelationshipWithPropertiesFromCustomQueryIncoming(
				@Autowired ReactiveHobbyithRelationshipWithPropertiesRepository repository) {

			long personId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:AltPerson{name:'Freddie'}), (n)-[l1:LIKES {rating: 5}]->(h1:AltHobby{name:'Music'}) RETURN n, h1").single();
				personId = record.get("n").asNode().id();
			}

			StepVerifier.create(repository.loadFromCustomQuery(personId)).assertNext(hobby -> {
				assertThat(hobby.getName()).isEqualTo("Music");
				assertThat(hobby.getLikedBy()).hasSize(1);
				assertThat(hobby.getLikedBy().entrySet()).first().satisfies(entry -> {
					assertThat(entry.getKey().getId()).isEqualTo(personId);
					assertThat(entry.getValue().getRating()).isEqualTo(5);
				});
			}).verifyComplete();
		}
	}

	@Nested
	class RelatedEntityQuery extends ReactiveIntegrationTestBase {

		@Test
		void findByPropertyOnRelatedEntity(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})");
			}

			StepVerifier.create(repository.findByPetsName("Jerry"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelatedEntitiesOr(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Tom'}),"
						+ "(n)-[:Has]->(:Hobby{name: 'Music'})");
			}

			StepVerifier.create(repository.findByHobbiesNameOrPetsName("Music", "Jerry"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();
			StepVerifier.create(repository.findByHobbiesNameOrPetsName("Sports", "Tom"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByHobbiesNameOrPetsName("Sports", "Jerry")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelatedEntitiesAnd(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Tom'}),"
						+ "(n)-[:Has]->(:Hobby{name: 'Music'})");
			}

			StepVerifier.create(repository.findByHobbiesNameAndPetsName("Music", "Tom"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByHobbiesNameAndPetsName("Sports", "Jerry")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelatedEntityOfRelatedEntity(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})"
						+ "-[:Has]->(:Hobby{name: 'Sleeping'})");
			}

			StepVerifier.create(repository.findByPetsHobbiesName("Sleeping"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByPetsHobbiesName("Sports")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelatedEntityOfRelatedSameEntity(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})"
						+ "-[:Has]->(:Pet{name: 'Tom'})");
			}

			StepVerifier.create(repository.findByPetsFriendsName("Tom"))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByPetsFriendsName("Jerry")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelationshipWithProperties(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020}]->(:Hobby{name: 'Bowling'})");
			}

			StepVerifier.create(repository.findByHobbiesSince(2020))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();
		}

		@Test
		void findByPropertyOnRelationshipWithPropertiesOr(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020, active: true}]->(:Hobby{name: 'Bowling'})");
			}

			StepVerifier.create(repository.findByHobbiesSinceOrHobbiesActive(2020, false))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByHobbiesSinceOrHobbiesActive(2019, true))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByHobbiesSinceOrHobbiesActive(2019, false)).verifyComplete();
		}

		@Test
		void findByPropertyOnRelationshipWithPropertiesAnd(
				@Autowired ReactivePersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020, active: true}]->(:Hobby{name: 'Bowling'})");
			}

			StepVerifier.create(repository.findByHobbiesSinceAndHobbiesActive(2020, true))
					.assertNext(person -> assertThat(person.getName()).isEqualTo("Freddie")).verifyComplete();

			StepVerifier.create(repository.findByHobbiesSinceAndHobbiesActive(2019, true)).verifyComplete();

			StepVerifier.create(repository.findByHobbiesSinceAndHobbiesActive(2020, false)).verifyComplete();
		}
	}

	@Nested
	class Save extends ReactiveIntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {

			transaction.run("MATCH (n) detach delete n");

			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place "
					+ "RETURN id(n)",
					parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ))
					.next().get(0).asLong();

			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");
			IntStream.rangeClosed(1, 20).forEach(i -> transaction
					.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})", parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, null);

			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void saveSingleEntity(@Autowired ReactivePersonRepository repository) {

			PersonWithAllConstructor person = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
					LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);

			Mono<Long> operationUnderTest = repository.save(person).map(PersonWithAllConstructor::getId);

			List<Long> ids = new ArrayList<>();

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> ids)
					.expectNextCount(1L).verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()),
					s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n", parameters("ids", ids))
							.records(),
					RxSession::close).map(r -> r.get("n").asNode().get("first_name").asString()).as(StepVerifier::create)
					.expectNext("Freddie").verifyComplete();
		}

		@Test
		void saveAll(@Autowired ReactivePersonRepository repository) {

			Flux<PersonWithAllConstructor> persons = repository.findById(id1).map(existingPerson -> {
				existingPerson.setFirstName("Updated first name");
				existingPerson.setNullable("Updated nullable field");
				return existingPerson;
			}).concatWith(Mono.fromSupplier(() -> {
				PersonWithAllConstructor newPerson = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true,
						1509L, LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);
				return newPerson;
			}));

			Flux<Long> operationUnderTest = repository.saveAll(persons).map(PersonWithAllConstructor::getId);

			List<Long> ids = new ArrayList<>();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> ids)
					.expectNextCount(2L).verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()),
					s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n ORDER BY n.name ASC",
							parameters("ids", ids)).records(),
					RxSession::close).map(r -> r.get("n").asNode().get("name").asString()).as(StepVerifier::create)
					.expectNext("Mercury").expectNext(TEST_PERSON1_NAME).verifyComplete();
		}

		@Test
		void saveAllIterable(@Autowired ReactivePersonRepository repository) {

			PersonWithAllConstructor newPerson = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true,
					1509L, LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);

			Flux<Long> operationUnderTest = repository.saveAll(Arrays.asList(newPerson)).map(PersonWithAllConstructor::getId);

			List<Long> ids = new ArrayList<>();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).recordWith(() -> ids)
					.expectNextCount(1L).verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()),
					s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n ORDER BY n.name ASC",
							parameters("ids", ids)).records(),
					RxSession::close).map(r -> r.get("n").asNode().get("name").asString()).as(StepVerifier::create)
					.expectNext("Mercury").verifyComplete();
		}

		@Test
		void updateSingleEntity(@Autowired ReactivePersonRepository repository) {

			Mono<PersonWithAllConstructor> operationUnderTest = repository.findById(id1).map(originalPerson -> {
				originalPerson.setFirstName("Updated first name");
				originalPerson.setNullable("Updated nullable field");
				return originalPerson;
			}).flatMap(repository::save);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).expectNextCount(1L)
					.verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()), s -> {
				Value parameters = parameters("id", id1);
				return s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) = $id RETURN n", parameters).records();
			}, RxSession::close).map(r -> r.get("n").asNode()).as(StepVerifier::create)
					.expectNextMatches(node -> node.get("first_name").asString().equals("Updated first name")
							&& node.get("nullable").asString().equals("Updated nullable field"))
					.verifyComplete();
		}

		@Test
		void saveWithAssignedId(@Autowired ReactiveThingRepository repository) {

			Mono<ThingWithAssignedId> operationUnderTest = Mono.fromSupplier(() -> {
				ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
				thing.setName("That's the thing.");
				return thing;
			}).flatMap(repository::save);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).expectNextCount(1L)
					.verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()),
					s -> s.run("MATCH (n:Thing) WHERE n.theId = $id RETURN n", parameters("id", "aaBB")).records(),
					RxSession::close).map(r -> r.get("n").asNode().get("name").asString()).as(StepVerifier::create)
					.expectNext("That's the thing.").verifyComplete();

			repository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
		}

		@Test
		void saveAllWithAssignedId(@Autowired ReactiveThingRepository repository) {

			Flux<ThingWithAssignedId> things = repository.findById("anId").map(existingThing -> {
				existingThing.setName("Updated name.");
				return existingThing;
			}).concatWith(Mono.fromSupplier(() -> {
				ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
				newThing.setName("That's the thing.");
				return newThing;
			}));

			Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(things);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).expectNextCount(2L)
					.verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()), s -> {
				Value parameters = parameters("ids", Arrays.asList("anId", "aaBB"));
				return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC", parameters)
						.records();
			}, RxSession::close).map(r -> r.get("name").asString()).as(StepVerifier::create).expectNext("That's the thing.")
					.expectNext("Updated name.").verifyComplete();

			// Make sure we triggered on insert, one update
			repository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
		}

		@Test
		void saveAllIterableWithAssignedId(@Autowired ReactiveThingRepository repository) {

			ThingWithAssignedId existingThing = new ThingWithAssignedId("anId");
			existingThing.setName("Updated name.");
			ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
			newThing.setName("That's the thing.");

			List<ThingWithAssignedId> things = Arrays.asList(existingThing, newThing);

			Flux<ThingWithAssignedId> operationUnderTest = repository.saveAll(things);

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).expectNextCount(2L)
					.verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()), s -> {
				Value parameters = parameters("ids", Arrays.asList("anId", "aaBB"));
				return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC", parameters)
						.records();
			}, RxSession::close).map(r -> r.get("name").asString()).as(StepVerifier::create).expectNext("That's the thing.")
					.expectNext("Updated name.").verifyComplete();

			// Make sure we triggered on insert, one update
			repository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
		}

		@Test
		void updateWithAssignedId(@Autowired ReactiveThingRepository repository) {

			Flux<ThingWithAssignedId> operationUnderTest = Flux.concat(
					// Without prior selection
					Mono.fromSupplier(() -> {
						ThingWithAssignedId thing = new ThingWithAssignedId("id07");
						thing.setName("An updated thing");
						return thing;
					}).flatMap(repository::save),

					// With prior selection
					repository.findById("id15").flatMap(thing -> {
						thing.setName("Another updated thing");
						return repository.save(thing);
					}));

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> operationUnderTest).as(StepVerifier::create).expectNextCount(2L)
					.verifyComplete();

			Flux.usingWhen(Mono.fromSupplier(() -> createRxSession()), s -> {
				Value parameters = parameters("ids", Arrays.asList("id07", "id15"));
				return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC", parameters)
						.records();
			}, RxSession::close).map(r -> r.get("name").asString()).as(StepVerifier::create)
					.expectNext("An updated thing", "Another updated thing").verifyComplete();

			repository.count().as(StepVerifier::create).expectNext(21L).verifyComplete();
		}

		@Test
		void saveWithConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			EntityWithConvertedId entity = new EntityWithConvertedId();
			entity.setIdentifyingEnum(EntityWithConvertedId.IdentifyingEnum.A);
			repository.save(entity).block();

			try (Session session = createSession()) {
				Record node = session.run("MATCH (e:EntityWithConvertedId) return e").next();
				assertThat(node.get("e").get("identifyingEnum").asString()).isEqualTo("A");
			}
		}

		@Test
		void saveAllWithConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			EntityWithConvertedId entity = new EntityWithConvertedId();
			entity.setIdentifyingEnum(EntityWithConvertedId.IdentifyingEnum.A);
			repository.saveAll(Collections.singleton(entity)).collectList().block();

			try (Session session = createSession()) {
				Record node = session.run("MATCH (e:EntityWithConvertedId) return e").next();
				assertThat(node.get("e").get("identifyingEnum").asString()).isEqualTo("A");
			}
		}
	}

	@Nested
	class SaveWithRelationships extends ReactiveIntegrationTestBase {

		@Test
		void saveSingleEntityWithRelationships(@Autowired ReactiveRelationshipRepository repository) {

			PersonWithRelationship person = new PersonWithRelationship();
			person.setName("Freddie");
			Hobby hobby = new Hobby();
			hobby.setName("Music");
			person.setHobbies(hobby);
			Club club = new Club();
			club.setName("ClownsClub");
			person.setClub(club);
			Pet pet1 = new Pet("Jerry");
			Pet pet2 = new Pet("Tom");
			Hobby petHobby = new Hobby();
			petHobby.setName("sleeping");
			pet1.setHobbies(singleton(petHobby));
			person.setPets(Arrays.asList(pet1, pet2));

			List<Long> ids = new ArrayList<>();
			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());
			transactionalOperator.execute(t -> repository.save(person).map(PersonWithRelationship::getId))
					.as(StepVerifier::create).recordWith(() -> ids).expectNextCount(1L).verifyComplete();

			try (Session session = createSession()) {

				Record record = session.run(
						"MATCH (n:PersonWithRelationship)" + " RETURN n,"
								+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
								+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies, " + " [(n)<-[:Has]-(c:Club) | c] as clubs",
						Values.parameters("name", "Freddie")).single();

				assertThat(record.containsKey("n")).isTrue();
				Node rootNode = record.get("n").asNode();
				assertThat(ids.get(0)).isEqualTo(rootNode.id());
				assertThat(rootNode.get("name").asString()).isEqualTo("Freddie");

				List<List<Object>> petsWithHobbies = record.get("petsWithHobbies").asList(Value::asList);

				Map<Object, List<Node>> pets = new HashMap<>();
				for (List<Object> petWithHobbies : petsWithHobbies) {
					pets.put(petWithHobbies.get(0), ((List<Node>) petWithHobbies.get(1)));
				}

				assertThat(pets.keySet().stream().map(pet -> ((Node) pet).get("name").asString()).collect(toList()))
						.containsExactlyInAnyOrder("Jerry", "Tom");

				assertThat(pets.values().stream()
						.flatMap(petHobbies -> petHobbies.stream().map(node -> node.get("name").asString())).collect(toList()))
								.containsExactlyInAnyOrder("sleeping");

				assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("Music");

				assertThat(record.get("clubs").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("ClownsClub");
			}
		}

		@Test
		void saveSingleEntityWithRelationshipsTwiceDoesNotCreateMoreRelationships(
				@Autowired ReactiveRelationshipRepository repository) {

			PersonWithRelationship person = new PersonWithRelationship();
			person.setName("Freddie");
			Hobby hobby = new Hobby();
			hobby.setName("Music");
			person.setHobbies(hobby);
			Pet pet1 = new Pet("Jerry");
			Pet pet2 = new Pet("Tom");
			Hobby petHobby = new Hobby();
			petHobby.setName("sleeping");
			pet1.setHobbies(singleton(petHobby));
			person.setPets(Arrays.asList(pet1, pet2));

			List<Long> ids = new ArrayList<>();

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());

			transactionalOperator.execute(t -> repository.save(person).map(PersonWithRelationship::getId))
					.as(StepVerifier::create).recordWith(() -> ids).expectNextCount(1L).verifyComplete();

			transactionalOperator.execute(t -> repository.save(person)).as(StepVerifier::create).expectNextCount(1L)
					.verifyComplete();

			try (Session session = createSession()) {

				List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)" + " RETURN n,"
						+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
						+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies", Values.parameters("name", "Freddie")).list();

				// assert that there is only one record in the returned list
				assertThat(recordList).hasSize(1);

				Record record = recordList.get(0);

				assertThat(record.containsKey("n")).isTrue();
				Node rootNode = record.get("n").asNode();
				assertThat(ids.get(0)).isEqualTo(rootNode.id());
				assertThat(rootNode.get("name").asString()).isEqualTo("Freddie");

				List<List<Object>> petsWithHobbies = record.get("petsWithHobbies").asList(Value::asList);

				Map<Object, List<Node>> pets = new HashMap<>();
				for (List<Object> petWithHobbies : petsWithHobbies) {
					pets.put(petWithHobbies.get(0), ((List<Node>) petWithHobbies.get(1)));
				}

				assertThat(pets.keySet().stream().map(pet -> ((Node) pet).get("name").asString()).collect(toList()))
						.containsExactlyInAnyOrder("Jerry", "Tom");

				assertThat(pets.values().stream()
						.flatMap(petHobbies -> petHobbies.stream().map(node -> node.get("name").asString())).collect(toList()))
								.containsExactlyInAnyOrder("sleeping");

				assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("Music");

				// assert that only two hobbies is stored
				recordList = session.run("MATCH (h:Hobby) RETURN h").list();
				assertThat(recordList).hasSize(2);

				// assert that only two pets is stored
				recordList = session.run("MATCH (p:Pet) RETURN p").list();
				assertThat(recordList).hasSize(2);
			}
		}

		@Test
		void saveEntityWithAlreadyExistingTargetNode(@Autowired ReactiveRelationshipRepository repository) {

			Long hobbyId;
			try (Session session = createSession()) {
				hobbyId = session.run("CREATE (h:Hobby{name: 'Music'}) return id(h) as hId").single().get("hId").asLong();
			}

			PersonWithRelationship person = new PersonWithRelationship();
			person.setName("Freddie");
			Hobby hobby = new Hobby();
			hobby.setId(hobbyId);
			hobby.setName("Music");
			person.setHobbies(hobby);

			List<Long> ids = new ArrayList<>();

			TransactionalOperator transactionalOperator = TransactionalOperator.create(getTransactionManager());

			transactionalOperator.execute(t -> repository.save(person).map(PersonWithRelationship::getId))
					.as(StepVerifier::create).recordWith(() -> ids).expectNextCount(1L).verifyComplete();

			try (Session session = createSession()) {

				List<Record> recordList = session
						.run("MATCH (n:PersonWithRelationship)" + " RETURN n," + " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
								Values.parameters("name", "Freddie"))
						.list();

				Record record = recordList.get(0);

				assertThat(record.containsKey("n")).isTrue();
				Node rootNode = record.get("n").asNode();
				assertThat(ids.get(0)).isEqualTo(rootNode.id());
				assertThat(rootNode.get("name").asString()).isEqualTo("Freddie");

				assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("Music");

				// assert that only one hobby is stored
				recordList = session.run("MATCH (h:Hobby) RETURN h").list();
				assertThat(recordList).hasSize(1);
			}
		}

		@Test
		void saveEntityWithDeepSelfReferences(@Autowired ReactivePetRepository repository) {
			Pet rootPet = new Pet("Luna");
			Pet petOfRootPet = new Pet("Daphne");
			Pet petOfChildPet = new Pet("Mucki");
			Pet petOfGrandChildPet = new Pet("Blacky");

			rootPet.setFriends(singletonList(petOfRootPet));
			petOfRootPet.setFriends(singletonList(petOfChildPet));
			petOfChildPet.setFriends(singletonList(petOfGrandChildPet));

			StepVerifier.create(repository.save(rootPet)).expectNextCount(1).verifyComplete();

			try (Session session = createSession()) {
				Record record = session.run("MATCH (rootPet:Pet)-[:Has]->(petOfRootPet:Pet)-[:Has]->(petOfChildPet:Pet)"
						+ "-[:Has]->(petOfGrandChildPet:Pet) " + "RETURN rootPet, petOfRootPet, petOfChildPet, petOfGrandChildPet",
						emptyMap()).single();

				assertThat(record.get("rootPet").asNode().get("name").asString()).isEqualTo("Luna");
				assertThat(record.get("petOfRootPet").asNode().get("name").asString()).isEqualTo("Daphne");
				assertThat(record.get("petOfChildPet").asNode().get("name").asString()).isEqualTo("Mucki");
				assertThat(record.get("petOfGrandChildPet").asNode().get("name").asString()).isEqualTo("Blacky");
			}
		}

		@Test
		void saveEntityGraphWithSelfInverseRelationshipDefined(@Autowired ReactiveSimilarThingRepository repository) {
			SimilarThing originalThing = new SimilarThing().withName("Original");
			SimilarThing similarThing = new SimilarThing().withName("Similar");

			originalThing.setSimilar(similarThing);
			similarThing.setSimilarOf(originalThing);
			StepVerifier.create(repository.save(originalThing)).expectNextCount(1).verifyComplete();

			try (Session session = createSession()) {
				Record record = session.run(
						"MATCH (ot:SimilarThing{name:'Original'})-[r:SimilarTo]->(st:SimilarThing {name:'Similar'})" + " RETURN r")
						.single();

				assertThat(record.keys()).isNotEmpty();
				assertThat(record.containsKey("r")).isTrue();
				assertThat(record.get("r").asRelationship().type()).isEqualToIgnoringCase("SimilarTo");
			}
		}

		@Test
		void createComplexSameClassRelationshipsBeforeRootObject(@Autowired ImmutablePersonRepository repository) {

			ImmutablePerson p1 = new ImmutablePerson("Person1", Collections.emptyList());
			ImmutablePerson p2 = new ImmutablePerson("Person2", Arrays.asList(p1));
			ImmutablePerson p3 = new ImmutablePerson("Person3", Arrays.asList(p2));
			ImmutablePerson p4 = new ImmutablePerson("Person4", Arrays.asList(p1, p3));

			ImmutablePerson savedImmutablePerson = repository.save(p4).block();

			StepVerifier.create(repository.findAll()).expectNextCount(4).verifyComplete();
		}

		@Test
		void saveEntityWithSelfReferencesInBothDirections(@Autowired ReactivePetRepository repository) {

			Pet luna = new Pet("Luna");
			Pet daphne = new Pet("Daphne");

			luna.setFriends(singletonList(daphne));
			daphne.setFriends(singletonList(luna));

			StepVerifier.create(repository.save(luna)).expectNextCount(1).verifyComplete();

			try (Session session = createSession()) {
				Record record = session.run("MATCH (luna:Pet{name:'Luna'})-[:Has]->(daphne:Pet{name:'Daphne'})"
						+ "-[:Has]->(luna2:Pet{name:'Luna'})" + "RETURN luna, daphne, luna2").single();

				assertThat(record.get("luna").asNode().get("name").asString()).isEqualTo("Luna");
				assertThat(record.get("daphne").asNode().get("name").asString()).isEqualTo("Daphne");
				assertThat(record.get("luna2").asNode().get("name").asString()).isEqualTo("Luna");
			}
		}
	}

	@Nested
	class Delete extends ReactiveIntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {

			transaction.run("MATCH (n) detach delete n");

			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place "
					+ "RETURN id(n)",
					parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ))
					.next().get(0).asLong();

			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, null);

			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void deleteAll(@Autowired ReactivePersonRepository repository) {

			repository.deleteAll().then(repository.count()).as(StepVerifier::create).expectNext(0L).verifyComplete();
		}

		@Test
		void deleteById(@Autowired ReactivePersonRepository repository) {

			repository.deleteById(id1).then(repository.existsById(id1)).concatWith(repository.existsById(id2))
					.as(StepVerifier::create).expectNext(false, true).verifyComplete();
		}

		@Test
		void deleteByIdPublisher(@Autowired ReactivePersonRepository repository) {

			repository.deleteById(Mono.just(id1)).then(repository.existsById(id1)).concatWith(repository.existsById(id2))
					.as(StepVerifier::create).expectNext(false, true).verifyComplete();
		}

		@Test
		void delete(@Autowired ReactivePersonRepository repository) {

			repository.delete(person1).then(repository.existsById(id1)).concatWith(repository.existsById(id2))
					.as(StepVerifier::create).expectNext(false, true).verifyComplete();
		}

		@Test
		void deleteAllEntities(@Autowired ReactivePersonRepository repository) {

			repository.deleteAll(Arrays.asList(person1, person2)).then(repository.existsById(id1))
					.concatWith(repository.existsById(id2)).as(StepVerifier::create).expectNext(false, false).verifyComplete();
		}

		@Test
		void deleteAllEntitiesPublisher(@Autowired ReactivePersonRepository repository) {

			repository.deleteAll(Flux.just(person1, person2)).then(repository.existsById(id1))
					.concatWith(repository.existsById(id2)).as(StepVerifier::create).expectNext(false, false).verifyComplete();
		}

		@Test
		void deleteSimpleRelationship(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'})");
			}

			Publisher<PersonWithRelationship> personLoad = repository.getPersonWithRelationshipsViaQuery().map(person -> {
				person.setHobbies(null);
				return person;
			});

			Flux<PersonWithRelationship> personSave = repository.saveAll(personLoad);

			StepVerifier.create(personSave.then(repository.getPersonWithRelationshipsViaQuery())).assertNext(person -> {
				assertThat(person.getHobbies()).isNull();
			}).verifyComplete();
		}

		@Test
		void deleteCollectionRelationship(@Autowired ReactiveRelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'}), "
						+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'})");
			}

			Publisher<PersonWithRelationship> personLoad = repository.getPersonWithRelationshipsViaQuery().map(person -> {
				person.getPets().remove(0);
				return person;
			});

			Flux<PersonWithRelationship> personSave = repository.saveAll(personLoad);

			StepVerifier.create(personSave.then(repository.getPersonWithRelationshipsViaQuery())).assertNext(person -> {
				assertThat(person.getPets()).hasSize(1);
			}).verifyComplete();
		}
	}

	@Nested
	class Projection extends ReactiveIntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {

			transaction.run("MATCH (n) detach delete n");

			transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place "
					+ "RETURN id(n)",
					parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ))
					.next().get(0).asLong();

			transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();
		}

		@Test
		void mapsInterfaceProjectionWithDerivedFinderMethod(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findByName(TEST_PERSON1_NAME))
					.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
					.verifyComplete();
		}

		@Test
		void mapsDtoProjectionWithDerivedFinderMethod(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findByFirstName(TEST_PERSON1_FIRST_NAME)).expectNextCount(1).verifyComplete();
		}

		@Test
		void mapsInterfaceProjectionWithDerivedFinderMethodWithMultipleResults(
				@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findBySameValue(TEST_PERSON_SAMEVALUE)).expectNextCount(2).verifyComplete();
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndMapProjection(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findByNameWithCustomQueryAndMapProjection(TEST_PERSON1_NAME))
					.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
					.verifyComplete();
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndMapProjectionWithMultipleResults(
				@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.loadAllProjectionsWithMapProjection()).expectNextCount(2).verifyComplete();
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndNodeReturn(@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.findByNameWithCustomQueryAndNodeReturn(TEST_PERSON1_NAME))
					.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
					.verifyComplete();
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndNodeReturnWithMultipleResults(
				@Autowired ReactivePersonRepository repository) {

			StepVerifier.create(repository.loadAllProjectionsWithNodeReturn()).expectNextCount(2).verifyComplete();
		}

	}

	@Nested
	class MultipleLabel extends ReactiveIntegrationTestBase {

		@Test
		void createNodeWithMultipleLabels(@Autowired ReactiveMultipleLabelRepository repository) {
			repository.save(new MultipleLabels.MultipleLabelsEntity()).block();

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:A) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void createAllNodesWithMultipleLabels(@Autowired ReactiveMultipleLabelRepository repository) {
			repository.saveAll(singletonList(new MultipleLabels.MultipleLabelsEntity())).collectList().block();

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:A) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void createNodeAndRelationshipWithMultipleLabels(@Autowired ReactiveMultipleLabelRepository labelRepository) {
			MultipleLabels.MultipleLabelsEntity entity = new MultipleLabels.MultipleLabelsEntity();
			entity.otherMultipleLabelEntity = new MultipleLabels.MultipleLabelsEntity();

			labelRepository.save(entity).block();

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:A)-[:HAS]->(c:A) return n, c").single();
				Node parentNode = record.get("n").asNode();
				Node childNode = record.get("c").asNode();
				assertThat(parentNode.labels()).containsExactlyInAnyOrder("A", "B", "C");
				assertThat(childNode.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void findNodeWithMultipleLabels(@Autowired ReactiveMultipleLabelRepository repository) {
			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:A:B:C), (n2:B:C), (n3:A) return n1, n2, n3").single();
				n1Id = record.get("n1").asNode().id();
				n2Id = record.get("n2").asNode().id();
				n3Id = record.get("n3").asNode().id();
			}

			StepVerifier.create(repository.findById(n1Id)).expectNextCount(1).verifyComplete();
			StepVerifier.create(repository.findById(n2Id)).verifyComplete();
			StepVerifier.create(repository.findById(n3Id)).verifyComplete();
		}

		@Test
		void deleteNodeWithMultipleLabels(@Autowired ReactiveMultipleLabelRepository repository) {

			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:A:B:C), (n2:B:C), (n3:A) return n1, n2, n3").single();
				n1Id = record.get("n1").asNode().id();
				n2Id = record.get("n2").asNode().id();
				n3Id = record.get("n3").asNode().id();
			}

			repository.deleteById(n1Id).block();
			repository.deleteById(n2Id).block();
			repository.deleteById(n3Id).block();

			try (Session session = createSession()) {
				assertThat(session.run("MATCH (n:A:B:C) return n").list()).hasSize(0);
				assertThat(session.run("MATCH (n:B:C) return n").list()).hasSize(1);
				assertThat(session.run("MATCH (n:A) return n").list()).hasSize(1);
			}
		}

		@Test
		void createNodeWithMultipleLabelsAndAssignedId(
				@Autowired ReactiveMultipleLabelWithAssignedIdRepository repository) {

			repository.save(new MultipleLabels.MultipleLabelsEntityWithAssignedId(4711L)).block();

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:X) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void createAllNodesWithMultipleLabels(@Autowired ReactiveMultipleLabelWithAssignedIdRepository repository) {

			repository.saveAll(singletonList(new MultipleLabels.MultipleLabelsEntityWithAssignedId(4711L))).collectList()
					.block();

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:X) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void createNodeAndRelationshipWithMultipleLabels(
				@Autowired ReactiveMultipleLabelWithAssignedIdRepository repository) {

			MultipleLabels.MultipleLabelsEntityWithAssignedId entity = new MultipleLabels.MultipleLabelsEntityWithAssignedId(
					4711L);
			entity.otherMultipleLabelEntity = new MultipleLabels.MultipleLabelsEntityWithAssignedId(42L);

			repository.save(entity).block();

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:X)-[:HAS]->(c:X) return n, c").single();
				Node parentNode = record.get("n").asNode();
				Node childNode = record.get("c").asNode();
				assertThat(parentNode.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
				assertThat(childNode.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void findNodeWithMultipleLabels(@Autowired ReactiveMultipleLabelWithAssignedIdRepository repository) {

			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:X:Y:Z{id:4711}), (n2:Y:Z{id:42}), (n3:X{id:23}) return n1, n2, n3")
						.single();
				n1Id = record.get("n1").asNode().get("id").asLong();
				n2Id = record.get("n2").asNode().get("id").asLong();
				n3Id = record.get("n3").asNode().get("id").asLong();
			}

			StepVerifier.create(repository.findById(n1Id)).expectNextCount(1).verifyComplete();
			StepVerifier.create(repository.findById(n2Id)).verifyComplete();
			StepVerifier.create(repository.findById(n3Id)).verifyComplete();
		}

		@Test
		void deleteNodeWithMultipleLabels(@Autowired ReactiveMultipleLabelWithAssignedIdRepository repository) {

			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:X:Y:Z{id:4711}), (n2:Y:Z{id:42}), (n3:X{id:23}) return n1, n2, n3")
						.single();
				n1Id = record.get("n1").asNode().get("id").asLong();
				n2Id = record.get("n2").asNode().get("id").asLong();
				n3Id = record.get("n3").asNode().get("id").asLong();
			}

			repository.deleteById(n1Id).block();
			repository.deleteById(n2Id).block();
			repository.deleteById(n3Id).block();

			try (Session session = createSession()) {
				assertThat(session.run("MATCH (n:X:Y:Z) return n").list()).hasSize(0);
				assertThat(session.run("MATCH (n:Y:Z) return n").list()).hasSize(1);
				assertThat(session.run("MATCH (n:X) return n").list()).hasSize(1);
			}
		}

	}

	@Nested
	class Converter extends ReactiveIntegrationTestBase {

		@Test
		void findByConvertedCustomType(@Autowired EntityWithCustomTypePropertyRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:CustomTypes{customType:'XYZ'})");
			}

			StepVerifier.create(repository.findByCustomType(ThingWithCustomTypes.CustomType.of("XYZ"))).expectNextCount(1)
					.verifyComplete();
		}

		@Test
		void findByConvertedCustomTypeWithCustomQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:CustomTypes{customType:'XYZ'})");
			}

			StepVerifier.create(repository.findByCustomTypeCustomQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
					.expectNextCount(1).verifyComplete();
		}

		@Test
		void findByConvertedCustomTypeWithSpELPropertyAccessQuery(
				@Autowired EntityWithCustomTypePropertyRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:CustomTypes{customType:'XYZ'})");
			}

			StepVerifier
					.create(repository.findByCustomTypeCustomSpELPropertyAccessQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
					.expectNextCount(1).verifyComplete();
		}

		@Test
		void findByConvertedCustomTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:CustomTypes{customType:'XYZ'})");
			}

			StepVerifier.create(repository.findByCustomTypeSpELObjectQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
					.expectNextCount(1).verifyComplete();
		}
	}

	interface BidirectionalStartRepository extends ReactiveNeo4jRepository<BidirectionalStart, Long> {}

	interface BidirectionalEndRepository extends ReactiveNeo4jRepository<BidirectionalEnd, Long> {}

	interface ImmutablePersonRepository extends ReactiveNeo4jRepository<ImmutablePerson, String> {}

	interface ReactiveDeepRelationshipRepository extends ReactiveNeo4jRepository<DeepRelationships.Type1, Long> {}

	interface ReactiveLoopingRelationshipRepository
			extends ReactiveNeo4jRepository<DeepRelationships.LoopingType1, Long> {}

	interface ReactiveMultipleLabelRepository
			extends ReactiveNeo4jRepository<MultipleLabels.MultipleLabelsEntity, Long> {}

	interface ReactiveMultipleLabelWithAssignedIdRepository
			extends ReactiveNeo4jRepository<MultipleLabels.MultipleLabelsEntityWithAssignedId, Long> {}

	interface ReactivePersonWithRelationshipWithPropertiesRepository
			extends ReactiveNeo4jRepository<PersonWithRelationshipWithProperties, Long> {

		@Query("MATCH (p:PersonWithRelationshipWithProperties)-[l:LIKES]->(h:Hobby) return p, collect(l), collect(h)")
		Mono<PersonWithRelationshipWithProperties> loadFromCustomQuery(@Param("id") Long id);

		Mono<PersonWithRelationshipWithProperties> findByHobbiesSince(int since);

		Mono<PersonWithRelationshipWithProperties> findByHobbiesSinceOrHobbiesActive(int since1, boolean active);

		Mono<PersonWithRelationshipWithProperties> findByHobbiesSinceAndHobbiesActive(int since1, boolean active);
	}

	interface ReactiveHobbyithRelationshipWithPropertiesRepository
			extends ReactiveNeo4jRepository<AltHobby, Long> {

		@Query("MATCH (p:AltPerson)-[l:LIKES]->(h:AltHobby) WHERE id(p) = $personId RETURN h, collect(l), collect(p)")
		Flux<AltHobby> loadFromCustomQuery(@Param("personId") Long personId);
	}

	interface ReactivePetRepository extends ReactiveNeo4jRepository<Pet, Long> {}

	interface ReactiveRelationshipRepository extends ReactiveNeo4jRepository<PersonWithRelationship, Long> {

		@Query("MATCH (n:PersonWithRelationship{name:'Freddie'}) "
				+ "OPTIONAL MATCH (n)-[r1:Has]->(p:Pet) WITH n, collect(r1) as petRels, collect(p) as pets "
				+ "OPTIONAL MATCH (n)-[r2:Has]->(h:Hobby) "
				+ "return n, petRels, pets, collect(r2) as hobbyRels, collect(h) as hobbies")
		Mono<PersonWithRelationship> getPersonWithRelationshipsViaQuery();

		Mono<PersonWithRelationship> findByPetsName(String petName);

		Mono<PersonWithRelationship> findByHobbiesNameOrPetsName(String hobbyName, String petName);

		Mono<PersonWithRelationship> findByHobbiesNameAndPetsName(String hobbyName, String petName);

		Mono<PersonWithRelationship> findByPetsHobbiesName(String hobbyName);

		Mono<PersonWithRelationship> findByPetsFriendsName(String petName);
	}

	interface ReactiveSimilarThingRepository extends ReactiveCrudRepository<SimilarThing, Long> {}

	interface EntityWithConvertedIdRepository
			extends ReactiveNeo4jRepository<EntityWithConvertedId, EntityWithConvertedId.IdentifyingEnum> {}

	interface EntityWithCustomTypePropertyRepository extends ReactiveNeo4jRepository<ThingWithCustomTypes, Long> {

		Mono<ThingWithCustomTypes> findByCustomType(ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $customType return c")
		Mono<ThingWithCustomTypes> findByCustomTypeCustomQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType.value} return c")
		Mono<ThingWithCustomTypes> findByCustomTypeCustomSpELPropertyAccessQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType} return c")
		Mono<ThingWithCustomTypes> findByCustomTypeSpELObjectQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);
	}

	@SpringJUnitConfig(ReactiveRepositoryIT.Config.class)
	static abstract class ReactiveIntegrationTestBase {

		@Autowired private Driver driver;

		@Autowired private ReactiveTransactionManager transactionManager;

		void setupData(Transaction transaction) {

		}

		@BeforeEach
		void before() {
			Session session = createSession();
			session.writeTransaction(tx -> {
				tx.run("MATCH (n) detach delete n").consume();
				setupData(tx);
				return null;
			});
			session.close();
		}

		Session createSession() {
			return driver.session(Optional.ofNullable(databaseSelection.getValue()).map(SessionConfig::forDatabase)
					.orElseGet(SessionConfig::defaultConfig));
		}

		RxSession createRxSession() {
			return driver.rxSession(Optional.ofNullable(databaseSelection.getValue()).map(SessionConfig::forDatabase)
					.orElseGet(SessionConfig::defaultConfig));
		}

		ReactiveTransactionManager getTransactionManager() {
			return transactionManager;
		}
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
		public Neo4jConversions neo4jConversions() {
			Set<GenericConverter> additionalConverters = new HashSet<>();
			additionalConverters.add(new ThingWithCustomTypes.CustomTypeConverter());

			return new Neo4jConversions(additionalConverters);
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Override
		@Bean
		protected ReactiveDatabaseSelectionProvider reactiveNeo4jDatabaseNameProvider() {
			return Optional.ofNullable(databaseSelection.getValue())
					.map(ReactiveDatabaseSelectionProvider::createStaticDatabaseSelectionProvider)
					.orElse(ReactiveDatabaseSelectionProvider.getDefaultSelectionProvider());
		}

	}
}
