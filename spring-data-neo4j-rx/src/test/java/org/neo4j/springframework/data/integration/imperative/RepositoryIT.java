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
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.Range.Bound.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Point;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.*;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension.Neo4jConnectionSupport;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Ján Šúr
 */
@Neo4jIntegrationTest
class RepositoryIT {
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

	static PersonWithAllConstructor personExample(String sameValue) {
		return new PersonWithAllConstructor(null, null, null, sameValue, null, null, null, null, null, null, null);
	}

	private static Neo4jConnectionSupport neo4jConnectionSupport;

	private final PersonRepository repository;
	private final ThingRepository thingRepository;
	private final RelationshipRepository relationshipRepository;
	private final PetRepository petRepository;
	private final Driver driver;
	private Long id1;
	private Long id2;
	private PersonWithAllConstructor person1;
	private PersonWithAllConstructor person2;

	@Autowired
	RepositoryIT(PersonRepository repository, ThingRepository thingRepository,
		RelationshipRepository relationshipRepository, PetRepository petRepository, Driver driver) {

		this.repository = repository;
		this.relationshipRepository = relationshipRepository;
		this.thingRepository = thingRepository;
		this.petRepository = petRepository;
		this.driver = driver;
	}

	@BeforeEach
	void setupData() {

		Transaction transaction = driver.session().beginTransaction();
		transaction.run("MATCH (n) detach delete n");

		ZonedDateTime createdAt = LocalDateTime.of(2019, 1, 1, 23, 23, 42, 0).atZone(ZoneOffset.UTC.normalized());
		id1 = transaction.run("" +
				"CREATE (n:PersonWithAllConstructor) " +
				"  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place, n.createdAt = $createdAt "
				+
				"RETURN id(n)",
			Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
				TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
				NEO4J_HQ, "createdAt", createdAt)
		).next().get(0).asLong();
		id2 = transaction.run(
			"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
			Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
				TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO)
		).next().get(0).asLong();
		transaction.run("CREATE (n:PersonWithNoConstructor) SET n.name = $name, n.first_name = $firstName",
			Values.parameters("name", TEST_PERSON1_NAME, "firstName", TEST_PERSON1_FIRST_NAME));
		transaction.run("CREATE (n:PersonWithWither) SET n.name = '" + TEST_PERSON1_NAME + "'");
		transaction.run("CREATE (n:KotlinPerson) SET n.name = '" + TEST_PERSON1_NAME + "'");
		transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");

		IntStream.rangeClosed(1, 20).forEach(i ->
			transaction.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})", Values.parameters("i", String.format("%02d", i))));

		transaction.commit();
		transaction.close();

		person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, createdAt.toInstant());
		person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
			false, 2L, TEST_PERSON2_BORN_ON, null, emptyList(), SFO, null);
	}

	@Test
	void findAll() {
		List<PersonWithAllConstructor> people = repository.findAll();
		assertThat(people).hasSize(2);
		assertThat(people).extracting("name").containsExactlyInAnyOrder(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
	}

	@Test
	void findAllWithoutResultDoesNotThrowAnException() {

		try (Session session = driver.session()) {
			session.run("MATCH (n:PersonWithAllConstructor) DETACH DELETE n;");
		}

		List<PersonWithAllConstructor> people = repository.findAll();
		assertThat(people).hasSize(0);
	}

	@Test
	void findById() {
		Optional<PersonWithAllConstructor> person = repository.findById(id1);
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void dontFindById() {
		Optional<PersonWithAllConstructor> person = repository.findById(-4711L);
		assertThat(person).isNotPresent();
	}

	@Test
	void dontFindOneByDerivedFinderMethodReturningOptional() {
		Optional<PersonWithAllConstructor> person = repository.findOneByNameAndFirstName("A", "BB");
		assertThat(person).isNotPresent();
	}

	@Test
	void dontFindOneByDerivedFinderMethodReturning() {
		PersonWithAllConstructor person = repository.findOneByName("A");
		assertThat(person).isNull();

		person = repository.findOneByName(TEST_PERSON1_NAME);
		assertThat(person).extracting(PersonWithAllConstructor::getName).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadEntityWithRelationship() {

		long personId;
		long clubId;
		long hobbyNode1Id;
		long hobbyNode2Id;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session()) {
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

		PersonWithRelationship loadedPerson = relationshipRepository.findById(personId).get();
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

	}

	@Test
	void loadEntityWithRelationshipToTheSameNode() {

		long personId;
		long hobbyNode1Id;
		long petNode1Id;

		try (Session session = driver.session()) {
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

		PersonWithRelationship loadedPerson = relationshipRepository.findById(personId).get();
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

	}

	@Test
	void loadMultipleEntitiesWithRelationship() {

		long hobbyNode1Id;
		long hobbyNode2Id;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session()) {
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

		List<PersonWithRelationship> loadedPersons = relationshipRepository.findAll();

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
	}

	@Test
	void loadEntityWithRelationshipViaQuery() {

		long personId;
		long hobbyNodeId;
		long petNode1Id;
		long petNode2Id;

		try (Session session = driver.session()) {
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

		PersonWithRelationship loadedPerson = relationshipRepository.getPersonWithRelationshipsViaQuery();
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

	}

	@Test
	void loadEntityWithRelationshipWithAssignedId() {

		long petNodeId;

		try (Session session = driver.session()) {
			Record record = session
				.run("CREATE (p:Pet{name:'Jerry'})-[:Has]->(t:Thing{theId:'t1', name:'Thing1'}) "
					+ "RETURN p, t").single();

			Node petNode = record.get("p").asNode();
			petNodeId = petNode.id();
		}

		Pet pet = petRepository.findById(petNodeId).get();
		ThingWithAssignedId relatedThing = pet.getThings().get(0);
		assertThat(relatedThing.getTheId()).isEqualTo("t1");
		assertThat(relatedThing.getName()).isEqualTo("Thing1");
	}

	@Test
	void existsById() {
		boolean exists = repository.existsById(id1);
		assertThat(exists).isTrue();
	}

	@Test
	void saveSingleEntity() {

		PersonWithAllConstructor person = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
			LocalDate.of(1946, 9, 15), null, Arrays.asList("b", "a"), null, null);
		PersonWithAllConstructor savedPerson = repository.save(person);
		try (Session session = driver.session()) {
			Record record = session.run("MATCH (n:PersonWithAllConstructor) WHERE n.first_name = $first_name RETURN n",
					Values.parameters("first_name", "Freddie")).single();

			assertThat(record.containsKey("n")).isTrue();
			Node node = record.get("n").asNode();
			assertThat(savedPerson.getId()).isEqualTo(node.id());
			assertThat(node.get("things").asList()).containsExactly("b", "a");
		}
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

		PersonWithRelationship savedPerson = relationshipRepository.save(person);
		try (Session session = driver.session()) {

			Record record = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies, "
					+ " [(n)<-[:Has]-(c:Club) | c] as clubs",
					Values.parameters("name", "Freddie")).single();

			assertThat(record.containsKey("n")).isTrue();
			Node rootNode = record.get("n").asNode();
			assertThat(savedPerson.getId()).isEqualTo(rootNode.id());
			assertThat(savedPerson.getName()).isEqualTo("Freddie");

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

		PersonWithRelationship savedPerson = relationshipRepository.save(person);
		savedPerson = relationshipRepository.save(savedPerson);
		try (Session session = driver.session()) {

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
			assertThat(savedPerson.getId()).isEqualTo(rootNode.id());
			assertThat(savedPerson.getName()).isEqualTo("Freddie");

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
		try (Session session = driver.session()) {
			hobbyId = session.run("CREATE (h:Hobby{name: 'Music'}) return id(h) as hId").single().get("hId").asLong();
		}

		PersonWithRelationship person = new PersonWithRelationship();
		person.setName("Freddie");
		Hobby hobby = new Hobby();
		hobby.setId(hobbyId);
		hobby.setName("Music");
		person.setHobbies(hobby);

		PersonWithRelationship savedPerson = relationshipRepository.save(person);
		try (Session session = driver.session()) {

			List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
					Values.parameters("name", "Freddie")).list();

			assertThat(recordList).hasSize(1);

			Record record = recordList.get(0);

			assertThat(record.containsKey("n")).isTrue();
			Node rootNode = record.get("n").asNode();
			assertThat(savedPerson.getId()).isEqualTo(rootNode.id());
			assertThat(savedPerson.getName()).isEqualTo("Freddie");

			assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
					.containsExactlyInAnyOrder("Music");

			// assert that only one hobby is stored
			recordList = session.run("MATCH (h:Hobby) RETURN h").list();
			assertThat(recordList).hasSize(1);
		}
	}

	@Test
	void saveEntityWithAlreadyExistingSourceAndTargetNode() {

		Long hobbyId;
		Long personId;

		try (Session session = driver.session()) {
			Record record = session.run(
				"CREATE (p:PersonWithRelationship{name: 'Freddie'}), (h:Hobby{name: 'Music'}) return id(h) as hId, id(p) as pId")
				.single();

			personId = record.get("pId").asLong();
			hobbyId = record.get("hId").asLong();
		}

		PersonWithRelationship person = new PersonWithRelationship();
		person.setName("Freddie");
		person.setId(personId);
		Hobby hobby = new Hobby();
		hobby.setId(hobbyId);
		hobby.setName("Music");
		person.setHobbies(hobby);

		PersonWithRelationship savedPerson = relationshipRepository.save(person);
		try (Session session = driver.session()) {

			List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)"
					+ " RETURN n,"
					+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
				Values.parameters("name", "Freddie")).list();

			assertThat(recordList).hasSize(1);

			Record record = recordList.get(0);

			assertThat(record.containsKey("n")).isTrue();
			Node rootNode = record.get("n").asNode();
			assertThat(savedPerson.getId()).isEqualTo(rootNode.id());
			assertThat(savedPerson.getName()).isEqualTo("Freddie");

			assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
				.containsExactlyInAnyOrder("Music");

			// assert that only one hobby is stored
			recordList = session.run("MATCH (h:Hobby) RETURN h").list();
			assertThat(recordList).hasSize(1);
		}
	}

	@Test
	void saveAll() {

		PersonWithAllConstructor newPerson = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
			LocalDate.of(1946, 9, 15), null, emptyList(), null, null);

		PersonWithAllConstructor existingPerson = repository.findById(id1).get();
		existingPerson.setFirstName("Updated first name");
		existingPerson.setNullable("Updated nullable field");

		assertThat(repository.count()).isEqualTo(2);

		List<Long> ids = StreamSupport.stream(repository.saveAll(Arrays.asList(existingPerson, newPerson)).spliterator(), false)
			.map(PersonWithAllConstructor::getId)
			.collect(toList());

		assertThat(repository.count()).isEqualTo(3);

		try (Session session = driver.session()) {

			Record record = session
				.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names", Values.parameters("ids", ids))
				.single();

			assertThat(record.containsKey("names")).isTrue();
			List<String> names = record.get("names").asList(Value::asString);
			assertThat(names).contains("Mercury", TEST_PERSON1_NAME);
		}
	}

	@Test
	void updateSingleEntity() {

		PersonWithAllConstructor originalPerson = repository.findById(id1).get();
		originalPerson.setFirstName("Updated first name");
		originalPerson.setNullable("Updated nullable field");
		assertThat(originalPerson.getThings()).isNotEmpty();
		originalPerson.setThings(emptyList());

		PersonWithAllConstructor savedPerson = repository.save(originalPerson);
		try (Session session = driver.session()) {
			session.readTransaction(tx -> {
				Record record = tx.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) = $id RETURN n",
					Values.parameters("id", id1)).single();

				assertThat(record.containsKey("n")).isTrue();
				Node node = record.get("n").asNode();

				assertThat(node.id()).isEqualTo(savedPerson.getId());
				assertThat(node.get("first_name").asString()).isEqualTo(savedPerson.getFirstName());
				assertThat(node.get("nullable").asString()).isEqualTo(savedPerson.getNullable());
				assertThat(node.get("things").asList()).isEmpty();

				return null;
			});
		}
	}

	@Test
	void delete() {

		repository.delete(person1);

		assertThat(repository.existsById(id1)).isFalse();
		assertThat(repository.existsById(id2)).isTrue();
	}

	@Test
	void deleteById() {

		repository.deleteById(id1);

		assertThat(repository.existsById(id1)).isFalse();
		assertThat(repository.existsById(id2)).isTrue();
	}

	@Test
	void deleteAllEntities() {

		repository.deleteAll(Arrays.asList(person1, person2));

		assertThat(repository.existsById(id1)).isFalse();
		assertThat(repository.existsById(id2)).isFalse();
	}

	@Test
	void deleteAll() {

		repository.deleteAll();
		assertThat(repository.count()).isEqualTo(0L);
	}

	@Test
	void deleteSimpleRelationship() {
		try (Session session = driver.session()) {
			session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'})");
		}

		PersonWithRelationship person = relationshipRepository.getPersonWithRelationshipsViaQuery();
		person.setHobbies(null);
		relationshipRepository.save(person);
		person = relationshipRepository.getPersonWithRelationshipsViaQuery();

		assertThat(person.getHobbies()).isNull();
	}

	@Test
	void deleteCollectionRelationship() {
		try (Session session = driver.session()) {
			session.run("CREATE (n:PersonWithRelationship{name:'Freddie'}), "
				+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'})");
		}

		PersonWithRelationship person = relationshipRepository.getPersonWithRelationshipsViaQuery();
		person.getPets().remove(0);
		relationshipRepository.save(person);
		person = relationshipRepository.getPersonWithRelationshipsViaQuery();

		assertThat(person.getPets()).hasSize(1);
	}

	@Test
	void findAllById() {
		List<PersonWithAllConstructor> persons = repository.findAllById(Arrays.asList(id1, id2));
		assertThat(persons).hasSize(2);
	}

	@Test
	void findByAssignedId() {
		Optional<ThingWithAssignedId> optionalThing = thingRepository.findById("anId");
		assertThat(optionalThing).isPresent();
		assertThat(optionalThing).map(ThingWithAssignedId::getTheId).contains("anId");
		assertThat(optionalThing).map(ThingWithAssignedId::getName).contains("Homer");

		AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
		anotherThing.setName("Bart");
		assertThat(optionalThing).map(ThingWithAssignedId::getThings)
			.contains(singletonList(anotherThing));
	}

	@Test
	void loadWithAssignedIdViaQuery() {
		ThingWithAssignedId thing = thingRepository.getViaQuery();
		assertThat(thing.getTheId()).isEqualTo("anId");
		assertThat(thing.getName()).isEqualTo("Homer");

		AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
		anotherThing.setName("Bart");
		assertThat(thing.getThings()).containsExactly(anotherThing);
	}

	@Test
	void saveWithAssignedId() {

		assertThat(thingRepository.count()).isEqualTo(21);

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
		thing.setName("That's the thing.");
		thing = thingRepository.save(thing);

		try (Session session = driver.session()) {
			Record record = session
				.run("MATCH (n:Thing) WHERE n.theId = $id RETURN n", Values.parameters("id", thing.getTheId()))
				.single();

			assertThat(record.containsKey("n")).isTrue();
			Node node = record.get("n").asNode();
			assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
			assertThat(node.get("name").asString()).isEqualTo(thing.getName());

			assertThat(thingRepository.count()).isEqualTo(22);
		}
	}

	@Test
	void saveWithAssignedIdAndRelationship() {

		assertThat(thingRepository.count()).isEqualTo(21);

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
		thing.setName("That's the thing.");
		AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
		anotherThing.setName("AnotherThing");
		thing.setThings(singletonList(anotherThing));
		thing = thingRepository.save(thing);

		try (Session session = driver.session()) {
			Record record = session
				.run("MATCH (n:Thing)-[:Has]->(t:Thing2) WHERE n.theId = $id RETURN n, t", Values.parameters("id", thing.getTheId()))
				.single();

			assertThat(record.containsKey("n")).isTrue();
			assertThat(record.containsKey("t")).isTrue();
			Node node = record.get("n").asNode();
			assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
			assertThat(node.get("name").asString()).isEqualTo(thing.getName());

			Node relatedNode = record.get("t").asNode();
			assertThat(relatedNode.get("theId").asLong()).isEqualTo(anotherThing.getTheId());
			assertThat(relatedNode.get("name").asString()).isEqualTo(anotherThing.getName());
			assertThat(thingRepository.count()).isEqualTo(22);
		}
	}

	@Test
	void saveAllWithAssignedId() {

		assertThat(thingRepository.count()).isEqualTo(21);

		ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
		newThing.setName("That's the thing.");

		ThingWithAssignedId existingThing = thingRepository.findById("anId").get();
		existingThing.setName("Updated name.");

		thingRepository.saveAll(Arrays.asList(newThing, existingThing));

		try (Session session = driver.session()) {
			Record record = session
				.run("MATCH (n:Thing) WHERE n.theId IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names", Values.parameters("ids", Arrays.asList(newThing.getTheId(), existingThing.getTheId())))
				.single();

			assertThat(record.containsKey("names")).isTrue();
			List<String> names = record.get("names").asList(Value::asString);
			assertThat(names).containsExactly(newThing.getName(), existingThing.getName());

			assertThat(thingRepository.count()).isEqualTo(22);
		}
	}

	@Test
	void saveAllWithAssignedIdAndRelationship() {

		assertThat(thingRepository.count()).isEqualTo(21);

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
		thing.setName("That's the thing.");
		AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
		anotherThing.setName("AnotherThing");
		thing.setThings(singletonList(anotherThing));
		thingRepository.saveAll(singletonList(thing));

		try (Session session = driver.session()) {
			Record record = session
				.run("MATCH (n:Thing)-[:Has]->(t:Thing2) WHERE n.theId = $id RETURN n, t", Values.parameters("id", thing.getTheId()))
				.single();

			assertThat(record.containsKey("n")).isTrue();
			assertThat(record.containsKey("t")).isTrue();
			Node node = record.get("n").asNode();
			assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
			assertThat(node.get("name").asString()).isEqualTo(thing.getName());

			Node relatedNode = record.get("t").asNode();
			assertThat(relatedNode.get("theId").asLong()).isEqualTo(anotherThing.getTheId());
			assertThat(relatedNode.get("name").asString()).isEqualTo(anotherThing.getName());
			assertThat(thingRepository.count()).isEqualTo(22);
		}
	}

	@Test
	void updateWithAssignedId() {

		assertThat(thingRepository.count()).isEqualTo(21);

		ThingWithAssignedId thing = new ThingWithAssignedId("id07");
		thing.setName("An updated thing");
		thingRepository.save(thing);

		thing = thingRepository.findById("id15").get();
		thing.setName("Another updated thing");
		thingRepository.save(thing);

		try (Session session = driver.session()) {
			Record record = session
				.run("MATCH (n:Thing) WHERE n.theId IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names",
					Values.parameters("ids", Arrays.asList("id07", "id15")))
				.single();

			assertThat(record.containsKey("names")).isTrue();
			List<String> names = record.get("names").asList(Value::asString);
			assertThat(names).containsExactly("An updated thing", "Another updated thing");

			assertThat(thingRepository.count()).isEqualTo(21);
		}
	}

	@Test
	void count() {
		assertThat(repository.count()).isEqualTo(2);
	}

	@Test
	void findAllWithSortByOrderDefault() {
		List<PersonWithAllConstructor> persons = repository.findAll(Sort.by("name"));

		assertThat(persons).containsExactly(person1, person2);
	}

	@Test
	void findAllWithSortByOrderAsc() {
		List<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.asc("name")));

		assertThat(persons).containsExactly(person1, person2);
	}

	@Test
	void findAllWithSortByOrderDesc() {
		List<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.desc("name")));

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
		Example<PersonWithAllConstructor> example = Example
			.of(person1, ExampleMatcher.matchingAll().withIgnoreNullValues());
		Optional<PersonWithAllConstructor> person = repository.findOne(example);

		assertThat(person).isPresent();
		assertThat(person.get()).isEqualTo(person1);
	}

	@Test
	void findAllByExample() {
		Example<PersonWithAllConstructor> example = Example
			.of(person1, ExampleMatcher.matchingAll().withIgnoreNullValues());
		List<PersonWithAllConstructor> persons = repository.findAll(example);

		assertThat(persons).containsExactly(person1);
	}

	@Test
	void findAllByExampleWithDifferentMatchers() {
		PersonWithAllConstructor person;
		Example<PersonWithAllConstructor> example;
		List<PersonWithAllConstructor> persons;

		person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME, TEST_PERSON2_FIRST_NAME, null, null, null, null,
			null, null, null, null);
		example = Example.of(person, ExampleMatcher.matchingAny());

		persons = repository.findAll(example);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);

		person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME.toUpperCase(), TEST_PERSON2_FIRST_NAME, null,
			null, null, null, null, null, null, null);
		example = Example.of(person, ExampleMatcher.matchingAny().withIgnoreCase("name"));

		persons = repository.findAll(example);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);

		person = new PersonWithAllConstructor(null,
			TEST_PERSON2_NAME.substring(TEST_PERSON2_NAME.length() - 2).toUpperCase(),
			TEST_PERSON2_FIRST_NAME.substring(0, 2), TEST_PERSON_SAMEVALUE.substring(3, 5), null, null, null, null,
			null, null, null);
		example = Example.of(person, ExampleMatcher
			.matchingAll()
			.withMatcher("name", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.ENDING, true))
			.withMatcher("firstName", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.STARTING))
			.withMatcher("sameValue", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.CONTAINING))
		);

		persons = repository.findAll(example);
		assertThat(persons).containsExactlyInAnyOrder(person2);

		person = new PersonWithAllConstructor(null, null, "(?i)ern.*", null, null, null, null, null, null, null, null);
		example = Example.of(person, ExampleMatcher.matchingAll().withStringMatcher(StringMatcher.REGEX));

		persons = repository.findAll(example);
		assertThat(persons).containsExactlyInAnyOrder(person1);

		example = Example
			.of(person, ExampleMatcher.matchingAll().withStringMatcher(StringMatcher.REGEX).withIncludeNullValues());

		persons = repository.findAll(example);
		assertThat(persons).isEmpty();
	}

	@Test
	void findAllByExampleWithSort() {
		Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
		List<PersonWithAllConstructor> persons = repository.findAll(example, Sort.by(Sort.Direction.DESC, "name"));

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
	void loadAllPersonsWithAllConstructorViaCustomQuery() {
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
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadOptionalPersonWithAllConstructorWithParameter() {
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaQuery(TEST_PERSON1_NAME);
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadNoPersonsWithAllConstructorViaCustomQueryWithoutException() {
		List<PersonWithAllConstructor> persons = repository.getNobodyViaQuery();
		assertThat(persons).hasSize(0);
	}

	@Test
	void loadOptionalPersonWithAllConstructorWithSpelParameters() {
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaQuery(TEST_PERSON1_NAME.substring(0, 2), TEST_PERSON1_NAME.substring(2));
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadOptionalPersonWithAllConstructorWithSpelParametersAndNamedQuery() {
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaNamedQuery(TEST_PERSON1_NAME.substring(0, 2), TEST_PERSON1_NAME.substring(2));
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
		Optional<PersonWithNoConstructor> person = repository.getOptionalPersonWithNoConstructorViaQuery();
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
		Optional<PersonWithWither> person = repository.getOptionalPersonWithWitherViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadAllKotlinPersons() {
		List<KotlinPerson> persons = repository.getAllKotlinPersonsViaQuery();
		assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
	}

	@Test
	void loadOneKotlinPerson() {
		KotlinPerson person = repository.getOneKotlinPersonViaQuery();
		assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void loadOptionalKotlinPerson() {
		Optional<KotlinPerson> person = repository.getOptionalKotlinPersonViaQuery();
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

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllBySameValue(TEST_PERSON_SAMEVALUE);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllBySameValueIgnoreCase(TEST_PERSON_SAMEVALUE.toUpperCase());
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllByBornOn(TEST_PERSON1_BORN_ON);
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findBySimplePropertyByEqualsWithNullShouldWork() {
		int emptyResultSize = 0;
		assertThat(repository.findAllBySameValue(null)).hasSize(emptyResultSize);
	}

	@Test
	void findBySimplePropertiesAnded() {

		Optional<PersonWithAllConstructor> optionalPerson;

		optionalPerson = repository.findOneByNameAndFirstName(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME);
		assertThat(optionalPerson).isPresent().contains(person1);

		optionalPerson = repository.findOneByNameAndFirstNameAllIgnoreCase(TEST_PERSON1_NAME.toUpperCase(), TEST_PERSON1_FIRST_NAME.toUpperCase());
		assertThat(optionalPerson).isPresent().contains(person1);
	}

	@Test
	void findBySimplePropertiesOred() {

		List<PersonWithAllConstructor> persons = repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
		assertThat(persons).containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findByNegatedSimpleProperty() {

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByNameNot(TEST_PERSON1_NAME);
		assertThat(persons).doesNotContain(person1);

		persons = repository.findAllByNameNotIgnoreCase(TEST_PERSON1_NAME.toUpperCase());
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

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameLike("Ern");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByFirstNameLikeIgnoreCase("eRN");
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

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameNotLike("Ern");
		assertThat(persons).doesNotContain(person1);

		persons = repository.findAllByFirstNameNotLikeIgnoreCase("eRN");
		assertThat(persons).doesNotContain(person1);
	}

	@Test
	void findByStartingWith() {

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameStartingWith("Er");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByFirstNameStartingWithIgnoreCase("eRN");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByContaining() {

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameContaining("ni");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByFirstNameContainingIgnoreCase("NI");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByNotContaining() {

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameNotContaining("ni");
		assertThat(persons)
			.hasSize(1)
			.contains(person2);

		persons = repository.findAllByFirstNameNotContainingIgnoreCase("NI");
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByEndingWith() {

		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByFirstNameEndingWith("nie");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByFirstNameEndingWithIgnoreCase("NIE");
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByLessThan() {

		List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsLessThan(2L);
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByLessThanEqual() {

		List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsLessThanEqual(2L);
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findByGreaterThanEqual() {

		List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsGreaterThanEqual(1L);
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findByGreaterThan() {

		List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsGreaterThan(1L);
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByBetweenRange() {

		List<PersonWithAllConstructor> persons;
		persons = repository.findAllByPersonNumberIsBetween(Range.from(inclusive(1L)).to(inclusive(2L)));
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllByPersonNumberIsBetween(Range.from(inclusive(1L)).to(exclusive(2L)));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByPersonNumberIsBetween(Range.from(inclusive(1L)).to(unbounded()));
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllByPersonNumberIsBetween(Range.from(exclusive(1L)).to(unbounded()));
		assertThat(persons)
			.hasSize(1)
			.contains(person2);

		persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.<Long>unbounded()).to(inclusive(2L)));
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.<Long>unbounded()).to(exclusive(2L)));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByPersonNumberIsBetween(Range.unbounded());
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);
	}

	@Test
	void findByBetween() {

		List<PersonWithAllConstructor> persons;
		persons = repository.findAllByPersonNumberIsBetween(1L, 2L);
		assertThat(persons)
			.containsExactlyInAnyOrder(person1, person2);

		persons = repository.findAllByPersonNumberIsBetween(3L, 5L);
		assertThat(persons).isEmpty();

		persons = repository.findAllByPersonNumberIsBetween(2L, 3L);
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByAfter() {
		List<PersonWithAllConstructor> persons = repository.findAllByBornOnAfter(TEST_PERSON1_BORN_ON);
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByBefore() {
		List<PersonWithAllConstructor> persons = repository.findAllByBornOnBefore(TEST_PERSON2_BORN_ON);
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByInstant() {
		List<PersonWithAllConstructor> persons = repository.findAllByCreatedAtBefore(LocalDate.of(2019, 9, 25).atStartOfDay().toInstant(ZoneOffset.UTC));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByIsNotNull() {
		List<PersonWithAllConstructor> persons = repository.findAllByNullableIsNotNull();
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByIsNull() {
		List<PersonWithAllConstructor> persons = repository.findAllByNullableIsNull();
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByIn() {
		List<PersonWithAllConstructor> persons = repository
			.findAllByFirstNameIn(Arrays.asList("a", "b", TEST_PERSON2_FIRST_NAME, "c"));
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByNotIn() {
		List<PersonWithAllConstructor> persons = repository
			.findAllByFirstNameNotIn(Arrays.asList("a", "b", TEST_PERSON2_FIRST_NAME, "c"));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByEmpty() {
		List<PersonWithAllConstructor> persons = repository.findAllByThingsIsEmpty();
		assertThat(persons)
			.hasSize(1)
			.contains(person2);
	}

	@Test
	void findByNotEmpty() {
		List<PersonWithAllConstructor> persons = repository.findAllByThingsIsNotEmpty();
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void findByExists() {
		List<PersonWithAllConstructor> persons = repository.findAllByNullableExists();
		assertThat(persons)
			.hasSize(1)
			.contains(person1);
	}

	@Test
	void shouldSupportSort() {
		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByOrderByFirstNameAscBornOnDesc();
		assertThat(persons)
			.containsExactly(person2, person1);
	}

	@Test
	void findByNear() {
		List<PersonWithAllConstructor> persons;

		persons = repository.findAllByPlaceNear(SFO);
		assertThat(persons)
			.containsExactly(person2, person1);

		persons = repository.findAllByPlaceNearAndFirstNameIn(SFO, singletonList(TEST_PERSON1_FIRST_NAME));
		assertThat(persons)
			.containsExactly(person1);

		persons = repository.findAllByPlaceNear(MINC, new Distance(200.0 / 1000.0, Metrics.KILOMETERS));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByPlaceNear(CLARION, new Distance(200.0 / 1000.0, Metrics.KILOMETERS));
		assertThat(persons).isEmpty();

		persons = repository.findAllByPlaceNear(MINC,
			Distance.between(60.0 / 1000.0, Metrics.KILOMETERS, 200.0 / 1000.0, Metrics.KILOMETERS));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByPlaceNear(MINC,
			Distance.between(100.0 / 1000.0, Metrics.KILOMETERS, 200.0 / 1000.0, Metrics.KILOMETERS));
		assertThat(persons).isEmpty();

		final Range<Distance> distanceRange = Range.of(inclusive(new Distance(100.0 / 1000.0, Metrics.KILOMETERS)), unbounded());
		persons = repository.findAllByPlaceNear(MINC, distanceRange);
		assertThat(persons)
			.hasSize(1)
			.contains(person2);

		persons = repository.findAllByPlaceNear(distanceRange, MINC);
		assertThat(persons)
			.hasSize(1)
			.contains(person2);

		persons = repository.findAllByPlaceWithin(new Circle(new org.springframework.data.geo.Point(MINC.x(), MINC.y()), new Distance(200.0 / 1000.0, Metrics.KILOMETERS)));
		assertThat(persons)
			.hasSize(1)
			.contains(person1);

		persons = repository.findAllByPlaceNear(CLARION, new Distance(200.0 / 1000.0, Metrics.KILOMETERS));
		assertThat(persons).isEmpty();
	}

	@Test
	void findBySomeCaseInsensitiveProperties() {

		List<PersonWithAllConstructor> persons;
		persons = repository.findAllByPlaceNearAndFirstNameAllIgnoreCase(SFO, TEST_PERSON1_FIRST_NAME.toUpperCase());
		assertThat(persons)
			.containsExactly(person1);
	}

	@DisabledIfEnvironmentVariable(named = "SDN_RX_NEO4J_VERSION", matches = "4\\.0\\.0-alpha(0.*|10.*)")
	@Test
	void limitClauseShouldWork() {

		List<ThingWithAssignedId> things;

		things = thingRepository.findTop5ByOrderByNameDesc();
		assertThat(things)
			.hasSize(5)
			.extracting(ThingWithAssignedId::getName)
			.containsExactlyInAnyOrder("name20", "name19", "name18", "name17", "name16");

		things = thingRepository.findFirstByOrderByNameDesc();
		assertThat(things)
			.extracting(ThingWithAssignedId::getName)
			.containsExactlyInAnyOrder("name20");
	}

	@Test
	void mapsInterfaceProjectionWithDerivedFinderMethod() {
		assertThat(repository.findByName(TEST_PERSON1_NAME).getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void mapsDtoProjectionWithDerivedFinderMethod() {
		assertThat(repository.findByFirstName(TEST_PERSON1_FIRST_NAME)).hasSize(1);
	}

	@Test
	void mapsInterfaceProjectionWithDerivedFinderMethodWithMultipleResults() {
		assertThat(repository.findBySameValue(TEST_PERSON_SAMEVALUE)).hasSize(2);
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndMapProjection() {
		assertThat(repository.findByNameWithCustomQueryAndMapProjection(TEST_PERSON1_NAME).getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndMapProjectionWithMultipleResults() {
		assertThat(repository.loadAllProjectionsWithMapProjection()).hasSize(2);
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndNodeReturn() {
		assertThat(repository.findByNameWithCustomQueryAndNodeReturn(TEST_PERSON1_NAME).getName()).isEqualTo(TEST_PERSON1_NAME);
	}

	@Test
	void mapsInterfaceProjectionWithCustomQueryAndNodeReturnWithMultipleResults() {
		assertThat(repository.loadAllProjectionsWithNodeReturn()).hasSize(2);
	}

	@Test
	void streamMethodsShouldWork() {
		assertThat(repository.findAllByNameLike("Test")).hasSize(2);
	}

	@Test
	void asyncMethodsShouldWork() {
		PersonWithAllConstructor p = repository.findOneByFirstName(TEST_PERSON1_FIRST_NAME).join();
		assertThat(p).isNotNull();
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}
	}
}
