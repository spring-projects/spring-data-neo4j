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
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.driver.Values.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.AnotherThingWithAssignedId;
import org.neo4j.springframework.data.integration.shared.Club;
import org.neo4j.springframework.data.integration.shared.Hobby;
import org.neo4j.springframework.data.integration.shared.LikesHobbyRelationship;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.integration.shared.PersonWithRelationship;
import org.neo4j.springframework.data.integration.shared.PersonWithRelationshipWithProperties;
import org.neo4j.springframework.data.integration.shared.Pet;
import org.neo4j.springframework.data.integration.shared.ThingWithAssignedId;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension.*;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.neo4j.springframework.data.types.CartesianPoint2d;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Philipp Tölle
 */
@Neo4jIntegrationTest
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveRepositoryIT {

	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";
	private static final String TEST_PERSON1_FIRST_NAME = "Ernie";
	private static final String TEST_PERSON2_FIRST_NAME = "Bert";
	private static final LocalDate TEST_PERSON1_BORN_ON = LocalDate.of(2019, 1, 1);
	private static final LocalDate TEST_PERSON2_BORN_ON = LocalDate.of(2019, 2, 1);
	private static final String TEST_PERSON_SAMEVALUE = "SameValue";
	private static final Point NEO4J_HQ = Values.point(4326, 12.994823, 55.612191).asPoint();
	private static final Point SFO = Values.point(4326, -122.38681, 37.61649).asPoint();
	private static final Point CLARION = Values.point(4326, 12.994243, 55.607726).asPoint();
	private static final Point MINC = Values.point(4326, 12.994039, 55.611496).asPoint();
	private static final long NOT_EXISTING_NODE_ID = 3123131231L;
	protected static Neo4jConnectionSupport neo4jConnectionSupport;
	@Autowired private ReactivePersonRepository repository;
	@Autowired private ReactiveThingRepository thingRepository;
	@Autowired private ReactiveRelationshipRepository relationshipRepository;
	@Autowired private ReactivePetRepository petRepository;
	@Autowired private ReactivePersonWithRelationshipWithPropertiesRepository relationshipWithPropertiesRepository;
	@Autowired private BidirectionalStartRepository bidirectionalStartRepository;
	@Autowired private BidirectionalEndRepository bidirectionalEndRepository;
	@Autowired private Driver driver;
	@Autowired private ReactiveTransactionManager transactionManager;
	private long id1;
	private long id2;
	private PersonWithAllConstructor person1;
	private PersonWithAllConstructor person2;

	static PersonWithAllConstructor personExample(String sameValue) {
		return new PersonWithAllConstructor(null, null, null, sameValue, null, null, null, null, null, null, null);
	}

	@BeforeEach
	void setupData() {

		Transaction transaction = driver.session(getSessionConfig()).beginTransaction();
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
		IntStream.rangeClosed(1, 20).forEach(i ->
			transaction.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})",
				parameters("i", String.format("%02d", i))));

		transaction.commit();
		transaction.close();

		person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			true,
			1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, null);

		person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
	}

	/**
	 * Shall be configured by test making use of database selection, so that the verification queries run in the correct database.
	 *
	 * @return The session config used for verification methods.
	 */
	SessionConfig getSessionConfig() {

		return SessionConfig.defaultConfig();
	}

	@Test
	void findAll() {
		List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

		StepVerifier.create(repository.findAll()).expectNextMatches(personList::contains)
			.expectNextMatches(personList::contains).verifyComplete();
	}

	@Test
	void findById() {
		StepVerifier.create(repository.findById(id1)).expectNext(person1).verifyComplete();
	}

	@Test
	void loadEntityWithRelationship() {

		long personId;
		long clubId;
		long hobbyNode1Id;
		long hobbyNode2Id;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
					+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'}), "
					+ "(n)<-[:Has]-(c:Club{name:'ClownsClub'}), "
					+ "(p1)-[:Has]->(h2:Hobby{name:'sleeping'}), "
					+ "(p1)-[:Has]->(p2)"
					+ "RETURN n, h1, h2, p1, p2, c").single();

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

		StepVerifier.create(relationshipRepository.findById(personId))
			.assertNext(loadedPerson -> {

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
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithRelationshipToTheSameNode() {

		long personId;
		long hobbyNode1Id;
		long petNode1Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
					+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), "
					+ "(p1)-[:Has]->(h1)"
					+ "RETURN n, h1, p1").single();

			Node personNode = record.get("n").asNode();
			Node hobbyNode1 = record.get("h1").asNode();
			Node petNode1 = record.get("p1").asNode();

			personId = personNode.id();
			hobbyNode1Id = hobbyNode1.id();
			petNode1Id = petNode1.id();
		}

		StepVerifier.create(relationshipRepository.findById(personId))
			.assertNext(loadedPerson -> {

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
			})
			.verifyComplete();
	}

	@Test
	void findWithPageable() {
		Sort sort = Sort.by("name");
		int page = 0;
		int limit = 1;

		StepVerifier.create(repository.findByNameStartingWith("Test", PageRequest.of(page, limit, sort)))
			.assertNext(person -> assertThat(person).isEqualTo(person1))
			.verifyComplete();

		sort = Sort.by("name");
		page = 1;
		limit = 1;

		StepVerifier.create(repository.findByNameStartingWith("Test", PageRequest.of(page, limit, sort)))
			.assertNext(person -> assertThat(person).isEqualTo(person2))
			.verifyComplete();
	}

	@Test
	void loadEntityWithBidirectionalRelationship() {

		long startId;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}) "
					+ "RETURN n").single();

			Node startNode = record.get("n").asNode();
			startId = startNode.id();
		}

		StepVerifier.create(bidirectionalStartRepository.findById(startId))
			.assertNext(entity -> {
				assertThat(entity.getEnds()).hasSize(1);
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithBidirectionalRelationshipFromIncomingSide() {

		long endId;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}) "
					+ "RETURN e").single();

			Node endNode = record.get("e").asNode();
			endId = endNode.id();
		}

		StepVerifier.create(bidirectionalEndRepository.findById(endId))
			.assertNext(entity -> {
				assertThat(entity.getStart()).isNotNull();
			})
			.verifyComplete();
	}

	@Test
	void loadMultipleEntitiesWithRelationship() {

		long hobbyNode1Id;
		long hobbyNode2Id;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h:Hobby{name:'Music'}), "
					+ "(n)-[:Has]->(p:Pet{name: 'Jerry'}) "
					+ "RETURN n, h, p").single();

			hobbyNode1Id = record.get("h").asNode().id();
			petNode1Id = record.get("p").asNode().id();

			record = session
				.run("CREATE (n:PersonWithRelationship{name:'SomeoneElse'})-[:Has]->(h:Hobby{name:'Music2'}), "
					+ "(n)-[:Has]->(p:Pet{name: 'Jerry2'}) "
					+ "RETURN n, h, p").single();

			hobbyNode2Id = record.get("h").asNode().id();
			petNode2Id = record.get("p").asNode().id();
		}

		StepVerifier.create(relationshipRepository.findAll())
			.recordWith(ArrayList::new)
			.expectNextCount(2)
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
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithRelationshipViaQuery() {

		long personId;
		long hobbyNodeId;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'}), "
					+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'}) "
					+ "RETURN n, h1, p1, p2").single();

			Node personNode = record.get("n").asNode();
			Node hobbyNode1 = record.get("h1").asNode();
			Node petNode1 = record.get("p1").asNode();
			Node petNode2 = record.get("p2").asNode();

			personId = personNode.id();
			hobbyNodeId = hobbyNode1.id();
			petNode1Id = petNode1.id();
			petNode2Id = petNode2.id();
		}

		StepVerifier.create(relationshipRepository.getPersonWithRelationshipsViaQuery())
			.assertNext(loadedPerson -> {
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
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithRelationshipWithAssignedId() {

		long petNodeId;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (p:Pet{name:'Jerry'})-[:Has]->(t:Thing{theId:'t1', name:'Thing1'}) "
					+ "RETURN p, t").single();

			Node petNode = record.get("p").asNode();
			petNodeId = petNode.id();
		}

		StepVerifier.create(petRepository.findById(petNodeId))
			.assertNext(pet -> {
				ThingWithAssignedId relatedThing = pet.getThings().get(0);
				assertThat(relatedThing.getTheId()).isEqualTo("t1");
				assertThat(relatedThing.getName()).isEqualTo("Thing1");
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithRelationshipWithProperties() {

		long personId;
		long hobbyNode1Id;
		long hobbyNode2Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
					+ " (n)-[l1:LIKES"
					+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
					+ "]->(h1:Hobby{name:'Music'}),"
					+ " (n)-[l2:LIKES"
					+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
					+ "]->(h2:Hobby{name:'Something else'})"
					+ "RETURN n, h1, h2").single();

			Node personNode = record.get("n").asNode();
			Node hobbyNode1 = record.get("h1").asNode();
			Node hobbyNode2 = record.get("h2").asNode();

			personId = personNode.id();
			hobbyNode1Id = hobbyNode1.id();
			hobbyNode2Id = hobbyNode2.id();
		}

		StepVerifier.create(relationshipWithPropertiesRepository.findById(personId))
			.assertNext(person -> {
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
			})
			.verifyComplete();

	}
	@Test
	void saveEntityWithRelationshipWithProperties() {
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
		Mono<PersonWithRelationshipWithProperties> operationUnderTest = relationshipWithPropertiesRepository
			.save(clonePerson);

		// then
		List<PersonWithRelationshipWithProperties> shouldBeDifferentPersons = new ArrayList<>();

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> shouldBeDifferentPersons)
			.expectNextCount(1L)
			.verifyComplete();

		assertThat(shouldBeDifferentPersons).size().isEqualTo(1);

		PersonWithRelationshipWithProperties shouldBeDifferentPerson = shouldBeDifferentPersons.get(0);
		assertThat(shouldBeDifferentPerson)
			.isNotNull()
			.isEqualToComparingOnlyGivenFields(clonePerson, "hobbies");
		assertThat(shouldBeDifferentPerson.getName()).isEqualToIgnoringCase("Freddie clone");

		// check content of db
		String matchQuery =
			"MATCH (n:PersonWithRelationshipWithProperties {name:'Freddie clone'}) "
				+ "RETURN n, "
				+ "[(n) -[:LIKES]->(h:Hobby) |h] as Hobbies, "
				+ "[(n) -[r:LIKES]->(:Hobby) |r] as rels";
		Flux.usingWhen(
			Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
			s -> s.run(matchQuery).records(),
			RxSession::close
		).as(StepVerifier::create)
			.assertNext(record -> {

				assertThat(record.containsKey("n")).isTrue();
				assertThat(record.containsKey("Hobbies")).isTrue();
				assertThat(record.containsKey("rels")).isTrue();
				assertThat(record.values()).hasSize(3);
				assertThat(record.get("Hobbies").values()).hasSize(2);
				assertThat(record.get("rels").values()).hasSize(2);

				assertThat(record.get("rels").values(Value::asRelationship)).
					extracting(
						Relationship::type,
						rel -> rel.get("active"),
						rel -> rel.get("localDate"),
						rel -> rel.get("point"),
						rel -> rel.get("myEnum"),
						rel -> rel.get("since")
					)
					.containsExactlyInAnyOrder(
						tuple(
							"LIKES", Values.value(rel1Active), Values.value(rel1LocalDate),
							Values.point(rel1Point.getSrid(), rel1Point.getX(), rel1Point.getY()),
							Values.value(rel1MyEnum.name()), Values.value(rel1Since)
						),
						tuple(
							"LIKES", Values.value(rel2Active), Values.value(rel2LocalDate),
							Values.point(rel2Point.getSrid(), rel2Point.getX(), rel2Point.getY()),
							Values.value(rel2MyEnum.name()), Values.value(rel2Since)
						)
					);
			})
			.verifyComplete();
	}

	@Test
	void loadEntityWithRelationshipWithPropertiesFromCustomQuery() {

		long personId;
		long hobbyNode1Id;
		long hobbyNode2Id;

		try (Session session = driver.session(getSessionConfig())) {
			Record record = session
				.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
					+ " (n)-[l1:LIKES"
					+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
					+ "]->(h1:Hobby{name:'Music'}),"
					+ " (n)-[l2:LIKES"
					+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
					+ "]->(h2:Hobby{name:'Something else'})"
					+ "RETURN n, h1, h2").single();

			Node personNode = record.get("n").asNode();
			Node hobbyNode1 = record.get("h1").asNode();
			Node hobbyNode2 = record.get("h2").asNode();

			personId = personNode.id();
			hobbyNode1Id = hobbyNode1.id();
			hobbyNode2Id = hobbyNode2.id();
		}

		StepVerifier.create(relationshipWithPropertiesRepository.loadFromCustomQuery(personId))
			.assertNext(person -> {
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
			})
			.verifyComplete();

	}

	@Test
	void findAllByIds() {
		List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

		StepVerifier.create(repository.findAllById(Arrays.asList(id1, id2))).expectNextMatches(personList::contains)
				.expectNextMatches(personList::contains).verifyComplete();
	}

	@Test
	void findAllByIdsPublisher() {
		List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

		StepVerifier.create(repository.findAllById(Flux.just(id1, id2))).expectNextMatches(personList::contains)
				.expectNextMatches(personList::contains).verifyComplete();
	}

	@Test
	void findByIdNoMatch() {
		StepVerifier.create(repository.findById(NOT_EXISTING_NODE_ID)).verifyComplete();
	}

	@Test
	void findByIdPublisher() {
		StepVerifier.create(repository.findById(Mono.just(id1))).expectNext(person1).verifyComplete();
	}

	@Test
	void findByIdPublisherNoMatch() {
		StepVerifier.create(repository.findById(Mono.just(NOT_EXISTING_NODE_ID))).verifyComplete();
	}

	@Test
	void findAllWithSortByOrderDefault() {
		StepVerifier.create(repository.findAll(Sort.by("name"))).expectNext(person1, person2).verifyComplete();
	}

	@Test
	void findAllWithSortByOrderAsc() {
		StepVerifier.create(repository.findAll(Sort.by(Sort.Order.asc("name")))).expectNext(person1, person2)
				.verifyComplete();
	}

	@Test
	void findAllWithSortByOrderDesc() {
		StepVerifier.create(repository.findAll(Sort.by(Sort.Order.desc("name")))).expectNext(person2, person1)
				.verifyComplete();
	}

	@Test
	void findOneByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1,
				ExampleMatcher.matchingAll().withIgnoreNullValues());

		StepVerifier.create(repository.findOne(example)).expectNext(person1).verifyComplete();
	}

	@Test
	void findAllByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1,
				ExampleMatcher.matchingAll().withIgnoreNullValues());
		StepVerifier.create(repository.findAll(example)).expectNext(person1).verifyComplete();
	}

	@Test
	void findAllByExampleWithDifferentMatchers() {
		PersonWithAllConstructor person;
		Example<PersonWithAllConstructor> example;

		person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME, TEST_PERSON2_FIRST_NAME, null, null, null, null,
				null, null, null, null);
		example = Example.of(person, ExampleMatcher.matchingAny());

		StepVerifier.create(repository.findAll(example))
			.recordWith(ArrayList::new)
			.expectNextCount(2)
			.expectRecordedMatches(recordedPersons -> recordedPersons.containsAll(Arrays.asList(person1, person2)))
			.verifyComplete();

		person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME.toUpperCase(), TEST_PERSON2_FIRST_NAME, null, null,
				null, null, null, null, null, null);
		example = Example.of(person, ExampleMatcher.matchingAny().withIgnoreCase("name"));

		StepVerifier.create(repository.findAll(example))
			.recordWith(ArrayList::new)
			.expectNextCount(2)
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
	void findAllByExampleWithSort() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));

		StepVerifier.create(repository.findAll(example, Sort.by(Sort.Direction.DESC, "name"))).expectNext(person2, person1)
				.verifyComplete();
	}

	@Test
	void existsById() {
		StepVerifier.create(repository.existsById(id1)).expectNext(true).verifyComplete();
	}

	@Test
	void existsByIdNoMatch() {
		StepVerifier.create(repository.existsById(NOT_EXISTING_NODE_ID)).expectNext(false).verifyComplete();
	}

	@Test
	void existsByIdPublisher() {
		StepVerifier.create(repository.existsById(id1)).expectNext(true).verifyComplete();
	}

	@Test
	void existsByIdPublisherNoMatch() {
		StepVerifier.create(repository.existsById(NOT_EXISTING_NODE_ID)).expectNext(false).verifyComplete();
	}

	@Test
	void existsByExample() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
		StepVerifier.create(repository.exists(example)).expectNext(true).verifyComplete();

	}

	@Test
	void count() {
		StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
	}

	@Test
	void countByExample() {
		Example<PersonWithAllConstructor> example = Example.of(person1);
		StepVerifier.create(repository.count(example)).expectNext(1L).verifyComplete();
	}

	@Test
	void callCustomCypher() {
		StepVerifier.create(repository.customQuery()).expectNext(1L).verifyComplete();
	}

	@Test
	void loadAllPersonsWithAllConstructor() {
		List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

		StepVerifier.create(repository.getAllPersonsViaQuery()).expectNextMatches(personList::contains)
				.expectNextMatches(personList::contains).verifyComplete();
	}

	@Test
	void loadOnePersonWithAllConstructor() {
		StepVerifier.create(repository.getOnePersonViaQuery()).expectNext(person1).verifyComplete();
	}

	@Test
	void findBySimplePropertiesAnded() {

		StepVerifier.create(repository.findOneByNameAndFirstName(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME))
				.expectNext(person1).verifyComplete();

		StepVerifier.create(repository.findOneByNameAndFirstNameAllIgnoreCase(TEST_PERSON1_NAME.toUpperCase(),
				TEST_PERSON1_FIRST_NAME.toUpperCase())).expectNext(person1).verifyComplete();

	}

	@Test
	void findBySimplePropertiesOred() {
		repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME)
			.as(StepVerifier::create)
			.recordWith(ArrayList::new)
			.expectNextCount(2)
			.expectRecordedMatches(recordedPersons -> recordedPersons.containsAll(Arrays.asList(person1, person2)))
			.verifyComplete();
	}

	@Test // GH-112
	void countBySimplePropertiesOred() {

		repository.countAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME)
			.as(StepVerifier::create).expectNext(2L).verifyComplete();
	}

	@Test
	void findBySimpleProperty() {
		List<PersonWithAllConstructor> personList = Arrays.asList(person1, person2);

		StepVerifier.create(repository.findAllBySameValue(TEST_PERSON_SAMEVALUE)).expectNextMatches(personList::contains)
				.expectNextMatches(personList::contains).verifyComplete();
	}

	@Test
	void deleteAll() {

		repository.deleteAll()
			.then(repository.count())
			.as(StepVerifier::create)
			.expectNext(0L)
			.verifyComplete();
	}

	@Test
	void deleteById() {

		repository.deleteById(id1)
			.then(repository.existsById(id1))
			.concatWith(repository.existsById(id2))
			.as(StepVerifier::create)
			.expectNext(false, true)
			.verifyComplete();
	}

	@Test
	void deleteByIdPublisher() {

		repository.deleteById(Mono.just(id1))
			.then(repository.existsById(id1))
			.concatWith(repository.existsById(id2))
			.as(StepVerifier::create)
			.expectNext(false, true)
			.verifyComplete();
	}

	@Test
	void delete() {

		repository.delete(person1)
			.then(repository.existsById(id1))
			.concatWith(repository.existsById(id2))
			.as(StepVerifier::create)
			.expectNext(false, true)
			.verifyComplete();
	}

	@Test
	void deleteAllEntities() {

		repository.deleteAll(Arrays.asList(person1, person2))
			.then(repository.existsById(id1))
			.concatWith(repository.existsById(id2))
			.as(StepVerifier::create)
			.expectNext(false, false)
			.verifyComplete();
	}

	@Test
	void deleteAllEntitiesPublisher() {

		repository.deleteAll(Flux.just(person1, person2))
			.then(repository.existsById(id1))
			.concatWith(repository.existsById(id2))
			.as(StepVerifier::create)
			.expectNext(false, false)
			.verifyComplete();
	}

	@Test
	void saveSingleEntity() {

		PersonWithAllConstructor person = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
			LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);

		Mono<Long> operationUnderTest = repository
			.save(person)
			.map(PersonWithAllConstructor::getId);

		List<Long> ids = new ArrayList<>();

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(1L)
			.verifyComplete();

		Flux.usingWhen(
			Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
			s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n", parameters("ids", ids))
				.records(),
			RxSession::close
		).map(r -> r.get("n").asNode().get("first_name").asString())
			.as(StepVerifier::create)
			.expectNext("Freddie")
			.verifyComplete();
	}

	@Test
	void saveAll() {

		Flux<PersonWithAllConstructor> persons = repository
			.findById(id1)
			.map(existingPerson -> {
				existingPerson.setFirstName("Updated first name");
				existingPerson.setNullable("Updated nullable field");
				return existingPerson;
			})
			.concatWith(
				Mono.fromSupplier(() -> {
					PersonWithAllConstructor newPerson = new PersonWithAllConstructor(
						null, "Mercury", "Freddie", "Queen", true, 1509L,
						LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);
					return newPerson;
				}));

		Flux<Long> operationUnderTest = repository
			.saveAll(persons)
			.map(PersonWithAllConstructor::getId);

		List<Long> ids = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(2L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n ORDER BY n.name ASC",
					parameters("ids", ids))
					.records(),
				RxSession::close
			).map(r -> r.get("n").asNode().get("name").asString())
			.as(StepVerifier::create)
			.expectNext("Mercury")
			.expectNext(TEST_PERSON1_NAME)
			.verifyComplete();
	}

	@Test
	void saveAllIterable() {

		PersonWithAllConstructor newPerson = new PersonWithAllConstructor(
			null, "Mercury", "Freddie", "Queen", true, 1509L,
			LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);

		Flux<Long> operationUnderTest = repository
			.saveAll(Arrays.asList(newPerson))
			.map(PersonWithAllConstructor::getId);

		List<Long> ids = new ArrayList<>();
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(1L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) in $ids RETURN n ORDER BY n.name ASC",
					parameters("ids", ids))
					.records(),
				RxSession::close
			).map(r -> r.get("n").asNode().get("name").asString())
			.as(StepVerifier::create)
			.expectNext("Mercury")
			.verifyComplete();
	}

	@Test
	void saveSingleEntityWithRelationships() {

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
		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> relationshipRepository.save(person).map(PersonWithRelationship::getId))
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(1L)
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {

			Record record = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies, "
					+ " [(n)<-[:Has]-(c:Club) | c] as clubs",
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
	void saveSingleEntityWithRelationshipsTwiceDoesNotCreateMoreRelationships() {

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

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);

		transactionalOperator
			.execute(t -> relationshipRepository.save(person).map(PersonWithRelationship::getId))
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(1L)
			.verifyComplete();

		transactionalOperator
			.execute(t -> relationshipRepository.save(person))
			.as(StepVerifier::create)
			.expectNextCount(1L)
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {

			List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
				Values.parameters("name", "Freddie")).list();

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
	void saveEntityWithAlreadyExistingTargetNode() {

		Long hobbyId;
		try (Session session = driver.session(getSessionConfig())) {
			hobbyId = session.run("CREATE (h:Hobby{name: 'Music'}) return id(h) as hId").single().get("hId").asLong();
		}

		PersonWithRelationship person = new PersonWithRelationship();
		person.setName("Freddie");
		Hobby hobby = new Hobby();
		hobby.setId(hobbyId);
		hobby.setName("Music");
		person.setHobbies(hobby);

		List<Long> ids = new ArrayList<>();

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);

		transactionalOperator
			.execute(t -> relationshipRepository.save(person).map(PersonWithRelationship::getId))
			.as(StepVerifier::create)
			.recordWith(() -> ids)
			.expectNextCount(1L)
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {

			List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
				Values.parameters("name", "Freddie")).list();

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
	void updateSingleEntity() {

		Mono<PersonWithAllConstructor> operationUnderTest = repository.findById(id1)
			.map(originalPerson -> {
				originalPerson.setFirstName("Updated first name");
				originalPerson.setNullable("Updated nullable field");
				return originalPerson;
			})
			.flatMap(repository::save);

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.expectNextCount(1L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> {
					Value parameters = parameters("id", id1);
					return s.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) = $id RETURN n", parameters).records();
				},
				RxSession::close
			)
			.map(r -> r.get("n").asNode())
			.as(StepVerifier::create)
			.expectNextMatches(node -> node.get("first_name").asString().equals("Updated first name") &&
				node.get("nullable").asString().equals("Updated nullable field"))
			.verifyComplete();
	}

	@Test
	void deleteSimpleRelationship() {
		try (Session session = driver.session(getSessionConfig())) {
			session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'})");
		}

		Publisher<PersonWithRelationship> personLoad = relationshipRepository.getPersonWithRelationshipsViaQuery()
			.map(person -> {
				person.setHobbies(null);
				return person;
			});

		Flux<PersonWithRelationship> personSave = relationshipRepository.saveAll(personLoad);

		StepVerifier.create(personSave.then(relationshipRepository.getPersonWithRelationshipsViaQuery()))
			.assertNext(person -> {
				assertThat(person.getHobbies()).isNull();
			})
			.verifyComplete();
	}

	@Test
	void deleteCollectionRelationship() {
		try (Session session = driver.session(getSessionConfig())) {
			session.run("CREATE (n:PersonWithRelationship{name:'Freddie'}), "
				+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'})");
		}

		Publisher<PersonWithRelationship> personLoad = relationshipRepository.getPersonWithRelationshipsViaQuery()
			.map(person -> {
				person.getPets().remove(0);
				return person;
			});

		Flux<PersonWithRelationship> personSave = relationshipRepository.saveAll(personLoad);

		StepVerifier.create(personSave.then(relationshipRepository.getPersonWithRelationshipsViaQuery()))
			.assertNext(person -> {
				assertThat(person.getPets()).hasSize(1);
			})
			.verifyComplete();
	}

	@Test
	void findByAssignedId() {
		StepVerifier.create(thingRepository.findById("anId"))
			.assertNext(thing -> {

				assertThat(thing.getTheId()).isEqualTo("anId");
				assertThat(thing.getName()).isEqualTo("Homer");

				AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
				anotherThing.setName("Bart");
				assertThat(thing.getThings()).containsExactlyInAnyOrder(anotherThing);
			})
			.verifyComplete();
	}

	@Test
	void loadWithAssignedIdViaQuery() {
		StepVerifier.create(thingRepository.getViaQuery())
			.assertNext(thing -> {
				assertThat(thing.getTheId()).isEqualTo("anId");
				assertThat(thing.getName()).isEqualTo("Homer");

				AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
				anotherThing.setName("Bart");
				assertThat(thing.getThings()).containsExactly(anotherThing);
			})
			.verifyComplete();
	}

	@Test
	void saveWithAssignedId() {

		Mono<ThingWithAssignedId> operationUnderTest =
			Mono.fromSupplier(() -> {
				ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
				thing.setName("That's the thing.");
				return thing;
			}).flatMap(thingRepository::save);

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.expectNextCount(1L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> s.run("MATCH (n:Thing) WHERE n.theId = $id RETURN n", parameters("id", "aaBB")).records(),
				RxSession::close
			)
			.map(r -> r.get("n").asNode().get("name").asString())
			.as(StepVerifier::create)
			.expectNext("That's the thing.")
			.verifyComplete();

		thingRepository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
	}

	@Test
	void saveAllWithAssignedId() {

		Flux<ThingWithAssignedId> things = thingRepository
			.findById("anId")
			.map(existingThing -> {
				existingThing.setName("Updated name.");
				return existingThing;
			})
			.concatWith(
				Mono.fromSupplier(() -> {
					ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
					newThing.setName("That's the thing.");
					return newThing;
				})
			);

		Flux<ThingWithAssignedId> operationUnderTest = thingRepository
			.saveAll(things);

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.expectNextCount(2L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> {
					Value parameters = parameters("ids", Arrays.asList("anId", "aaBB"));
					return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC",
						parameters)
						.records();
				},
				RxSession::close
			)
			.map(r -> r.get("name").asString())
			.as(StepVerifier::create)
			.expectNext("That's the thing.")
			.expectNext("Updated name.")
			.verifyComplete();

		// Make sure we triggered on insert, one update
		thingRepository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
	}

	@Test
	void saveAllIterableWithAssignedId() {

		ThingWithAssignedId existingThing = new ThingWithAssignedId("anId");
		existingThing.setName("Updated name.");
		ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
		newThing.setName("That's the thing.");

		List<ThingWithAssignedId> things = Arrays.asList(existingThing, newThing);

		Flux<ThingWithAssignedId> operationUnderTest = thingRepository.saveAll(things);

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.expectNextCount(2L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> {
					Value parameters = parameters("ids", Arrays.asList("anId", "aaBB"));
					return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC",
						parameters)
						.records();
				},
				RxSession::close
			)
			.map(r -> r.get("name").asString())
			.as(StepVerifier::create)
			.expectNext("That's the thing.")
			.expectNext("Updated name.")
			.verifyComplete();

		// Make sure we triggered on insert, one update
		thingRepository.count().as(StepVerifier::create).expectNext(22L).verifyComplete();
	}

	@Test
	void updateWithAssignedId() {

		Flux<ThingWithAssignedId> operationUnderTest = Flux.concat(
			// Without prior selection
			Mono.fromSupplier(() -> {
				ThingWithAssignedId thing = new ThingWithAssignedId("id07");
				thing.setName("An updated thing");
				return thing;
			}).flatMap(thingRepository::save),

			// With prior selection
			thingRepository.findById("id15")
				.flatMap(thing -> {
					thing.setName("Another updated thing");
					return thingRepository.save(thing);
				})
		);

		TransactionalOperator transactionalOperator = TransactionalOperator.create(transactionManager);
		transactionalOperator
			.execute(t -> operationUnderTest)
			.as(StepVerifier::create)
			.expectNextCount(2L)
			.verifyComplete();

		Flux
			.usingWhen(
				Mono.fromSupplier(() -> driver.rxSession(getSessionConfig())),
				s -> {
					Value parameters = parameters("ids", Arrays.asList("id07", "id15"));
					return s.run("MATCH (n:Thing) WHERE n.theId IN ($ids) RETURN n.name as name ORDER BY n.name ASC",
						parameters).records();
				},
				RxSession::close
			)
			.map(r -> r.get("name").asString())
			.as(StepVerifier::create)
			.expectNext("An updated thing", "Another updated thing")
			.verifyComplete();

		thingRepository.count().as(StepVerifier::create).expectNext(21L).verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithDerivedFinderMethod() {
		StepVerifier.create(repository.findByName(TEST_PERSON1_NAME))
			.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
			.verifyComplete();
	}

	@Test
	void mapsDtoProjectionWithDerivedFinderMethod() {
		StepVerifier.create(repository.findByFirstName(TEST_PERSON1_FIRST_NAME))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithDerivedFinderMethodWithMultipleResults() {
		StepVerifier.create(repository.findBySameValue(TEST_PERSON_SAMEVALUE))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndMapProjection() {
		StepVerifier.create(repository.findByNameWithCustomQueryAndMapProjection(TEST_PERSON1_NAME))
			.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
			.verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndMapProjectionWithMultipleResults() {
		StepVerifier.create(repository.loadAllProjectionsWithMapProjection())
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndNodeReturn() {
		StepVerifier.create(repository.findByNameWithCustomQueryAndNodeReturn(TEST_PERSON1_NAME))
			.assertNext(personProjection -> assertThat(personProjection.getName()).isEqualTo(TEST_PERSON1_NAME))
			.verifyComplete();
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndNodeReturnWithMultipleResults() {
		StepVerifier.create(repository.loadAllProjectionsWithNodeReturn())
			.expectNextCount(2)
			.verifyComplete();
	}

	@Configuration
	@EnableReactiveNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}
	}
}
