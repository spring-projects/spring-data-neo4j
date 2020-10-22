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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Polygon;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.integration.imperative.repositories.PersonRepository;
import org.springframework.data.neo4j.integration.imperative.repositories.ThingRepository;
import org.springframework.data.neo4j.integration.shared.AltHobby;
import org.springframework.data.neo4j.integration.shared.AltLikedByPersonRelationship;
import org.springframework.data.neo4j.integration.shared.AltPerson;
import org.springframework.data.neo4j.integration.shared.AnotherThingWithAssignedId;
import org.springframework.data.neo4j.integration.shared.BidirectionalEnd;
import org.springframework.data.neo4j.integration.shared.BidirectionalStart;
import org.springframework.data.neo4j.integration.shared.Club;
import org.springframework.data.neo4j.integration.shared.ClubRelationship;
import org.springframework.data.neo4j.integration.shared.DeepRelationships;
import org.springframework.data.neo4j.integration.shared.DtoPersonProjection;
import org.springframework.data.neo4j.integration.shared.DtoPersonProjectionContainingAdditionalFields;
import org.springframework.data.neo4j.integration.shared.EntityWithConvertedId;
import org.springframework.data.neo4j.integration.shared.ExtendedParentNode;
import org.springframework.data.neo4j.integration.shared.Friend;
import org.springframework.data.neo4j.integration.shared.FriendshipRelationship;
import org.springframework.data.neo4j.integration.shared.Hobby;
import org.springframework.data.neo4j.integration.shared.ImmutablePerson;
import org.springframework.data.neo4j.integration.shared.Inheritance;
import org.springframework.data.neo4j.integration.shared.KotlinPerson;
import org.springframework.data.neo4j.integration.shared.LikesHobbyRelationship;
import org.springframework.data.neo4j.integration.shared.MultipleLabels;
import org.springframework.data.neo4j.integration.shared.ParentNode;
import org.springframework.data.neo4j.integration.shared.PersonWithAllConstructor;
import org.springframework.data.neo4j.integration.shared.PersonWithNoConstructor;
import org.springframework.data.neo4j.integration.shared.PersonWithRelationship;
import org.springframework.data.neo4j.integration.shared.PersonWithRelationshipWithProperties;
import org.springframework.data.neo4j.integration.shared.PersonWithRelationshipWithProperties2;
import org.springframework.data.neo4j.integration.shared.PersonWithWither;
import org.springframework.data.neo4j.integration.shared.Pet;
import org.springframework.data.neo4j.integration.shared.SimilarThing;
import org.springframework.data.neo4j.integration.shared.ThingWithAssignedId;
import org.springframework.data.neo4j.integration.shared.ThingWithCustomTypes;
import org.springframework.data.neo4j.integration.shared.ThingWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.WorksInClubRelationship;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.BoundingBox;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.types.CartesianPoint2d;
import org.springframework.data.neo4j.types.GeographicPoint2d;
import org.springframework.data.repository.query.Param;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Ján Šúr
 * @author Philipp Tölle
 */
@ExtendWith(Neo4jExtension.class)
@SpringJUnitConfig
@DirtiesContext // We need this here as the nested tests all inherit from the integration test base but the database selection here is different
class RepositoryIT {

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
	private static final Point CLARION = Values.point(4326, 12.994243, 55.607726).asPoint();
	private static final Point MINC = Values.point(4326, 12.994039, 55.611496).asPoint();

	static PersonWithAllConstructor personExample(String sameValue) {
		return new PersonWithAllConstructor(null, null, null, sameValue, null, null, null, null, null, null, null);
	}

	Long id1;
	Long id2;
	PersonWithAllConstructor person1;
	PersonWithAllConstructor person2;

	RepositoryIT() {
		databaseSelection = DatabaseSelection.undecided();
	}

	@Nested
	class Find extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			ZonedDateTime createdAt = LocalDateTime.of(2019, 1, 1, 23, 23, 42, 0).atZone(ZoneOffset.UTC.normalized());
			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place, n.createdAt = $createdAt "
					+ "RETURN id(n)",
					Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ, "createdAt", createdAt))
					.next().get(0).asLong();
			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();
			transaction.run("CREATE (n:PersonWithNoConstructor) SET n.name = $name, n.first_name = $firstName",
					Values.parameters("name", TEST_PERSON1_NAME, "firstName", TEST_PERSON1_FIRST_NAME));
			transaction.run("CREATE (n:PersonWithWither) SET n.name = '" + TEST_PERSON1_NAME + "'");
			transaction.run("CREATE (n:KotlinPerson), "
					+ " (n)-[:WORKS_IN{since: 2019}]->(:KotlinClub{name: 'Golf club'}) SET n.name = '" + TEST_PERSON1_NAME + "'");
			transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");

			IntStream.rangeClosed(1, 20)
					.forEach(i -> transaction.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})",
							Values.parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, createdAt.toInstant());
			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void findAll(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> people = repository.findAll();
			assertThat(people).hasSize(2);
			assertThat(people).extracting("name").containsExactlyInAnyOrder(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
		}

		@Test
		void findAllWithoutResultDoesNotThrowAnException(@Autowired PersonRepository repository) {

			try (Session session = createSession()) {
				session.run("MATCH (n:PersonWithAllConstructor) DETACH DELETE n;");
			}

			List<PersonWithAllConstructor> people = repository.findAll();
			assertThat(people).hasSize(0);
		}

		@Test
		void findById(@Autowired PersonRepository repository) {
			Optional<PersonWithAllConstructor> person = repository.findById(id1);
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void dontFindById(@Autowired PersonRepository repository) {
			Optional<PersonWithAllConstructor> person = repository.findById(-4711L);
			assertThat(person).isNotPresent();
		}

		@Test
		void dontFindOneByDerivedFinderMethodReturningOptional(@Autowired PersonRepository repository) {
			Optional<PersonWithAllConstructor> person = repository.findOneByNameAndFirstName("A", "BB");
			assertThat(person).isNotPresent();
		}

		@Test
		void dontFindOneByDerivedFinderMethodReturning(@Autowired PersonRepository repository) {
			PersonWithAllConstructor person = repository.findOneByName("A");
			assertThat(person).isNull();

			person = repository.findOneByName(TEST_PERSON1_NAME);
			assertThat(person).extracting(PersonWithAllConstructor::getName).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void findAllById(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllById(Arrays.asList(id1, id2));
			assertThat(persons).hasSize(2);
		}

		@Test
		void findByAssignedId(@Autowired ThingRepository repository) {

			Optional<ThingWithAssignedId> optionalThing = repository.findById("anId");
			assertThat(optionalThing).isPresent();
			assertThat(optionalThing).map(ThingWithAssignedId::getTheId).contains("anId");
			assertThat(optionalThing).map(ThingWithAssignedId::getName).contains("Homer");

			AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
			anotherThing.setName("Bart");
			assertThat(optionalThing).map(ThingWithAssignedId::getThings).contains(Collections.singletonList(anotherThing));
		}

		@Test
		void findByConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:EntityWithConvertedId{identifyingEnum:'A'})");
			}

			Optional<EntityWithConvertedId> entity = repository.findById(EntityWithConvertedId.IdentifyingEnum.A);
			assertThat(entity).isPresent();
			assertThat(entity.get().getIdentifyingEnum()).isEqualTo(EntityWithConvertedId.IdentifyingEnum.A);
		}

		@Test
		void findAllByConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:EntityWithConvertedId{identifyingEnum:'A'})");
			}

			List<EntityWithConvertedId> entities = repository.findAllById(Collections.singleton(EntityWithConvertedId.IdentifyingEnum.A));

			assertThat(entities).hasSize(1);
			assertThat(entities.get(0).getIdentifyingEnum()).isEqualTo(EntityWithConvertedId.IdentifyingEnum.A);
		}

		@Test
		void findWithAssignedIdViaQuery(@Autowired ThingRepository repository) {

			ThingWithAssignedId thing = repository.getViaQuery();
			assertThat(thing.getTheId()).isEqualTo("anId");
			assertThat(thing.getName()).isEqualTo("Homer");

			AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
			anotherThing.setName("Bart");
			assertThat(thing.getThings()).containsExactly(anotherThing);
		}

		@Test
		void findAllWithSortByOrderDefault(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAll(Sort.by("name"));

			assertThat(persons).containsExactly(person1, person2);
		}

		@Test
		void findAllWithSortByOrderAsc(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.asc("name")));

			assertThat(persons).containsExactly(person1, person2);
		}

		@Test
		void findAllWithSortByOrderDesc(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAll(Sort.by(Sort.Order.desc("name")));

			assertThat(persons).containsExactly(person2, person1);
		}

		@Test
		void findAllWithPageable(@Autowired PersonRepository repository) {

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
		void loadAllPersonsWithAllConstructorViaCustomQuery(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.getAllPersonsViaQuery();

			assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
		}

		@Test
		void loadOnePersonWithAllConstructor(@Autowired PersonRepository repository) {

			PersonWithAllConstructor person = repository.getOnePersonViaQuery();
			assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadOptionalPersonWithAllConstructor(@Autowired PersonRepository repository) {

			Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaQuery();
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadOptionalPersonWithAllConstructorWithParameter(@Autowired PersonRepository repository) {

			Optional<PersonWithAllConstructor> person = repository.getOptionalPersonViaQuery(TEST_PERSON1_NAME);
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadNoPersonsWithAllConstructorViaCustomQueryWithoutException(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.getNobodyViaQuery();
			assertThat(persons).hasSize(0);
		}

		@Test
		void loadOptionalPersonWithAllConstructorWithSpelParameters(@Autowired PersonRepository repository) {

			Optional<PersonWithAllConstructor> person = repository
					.getOptionalPersonViaQuery(TEST_PERSON1_NAME.substring(0, 2), TEST_PERSON1_NAME.substring(2));
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadOptionalPersonWithAllConstructorWithSpelParametersAndNamedQuery(@Autowired PersonRepository repository) {

			Optional<PersonWithAllConstructor> person = repository
					.getOptionalPersonViaNamedQuery(TEST_PERSON1_NAME.substring(0, 2), TEST_PERSON1_NAME.substring(2));
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadAllPersonsWithNoConstructor(@Autowired PersonRepository repository) {

			List<PersonWithNoConstructor> persons = repository.getAllPersonsWithNoConstructorViaQuery();

			assertThat(persons).extracting(PersonWithNoConstructor::getName, PersonWithNoConstructor::getFirstName)
					.containsExactlyInAnyOrder(Tuple.tuple(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME));
		}

		@Test
		void loadOnePersonWithNoConstructor(@Autowired PersonRepository repository) {

			PersonWithNoConstructor person = repository.getOnePersonWithNoConstructorViaQuery();
			assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
			assertThat(person.getFirstName()).isEqualTo(TEST_PERSON1_FIRST_NAME);
		}

		@Test
		void loadOptionalPersonWithNoConstructor(@Autowired PersonRepository repository) {

			Optional<PersonWithNoConstructor> person = repository.getOptionalPersonWithNoConstructorViaQuery();
			assertThat(person).isPresent();
			assertThat(person).map(PersonWithNoConstructor::getName).contains(TEST_PERSON1_NAME);
			assertThat(person).map(PersonWithNoConstructor::getFirstName).contains(TEST_PERSON1_FIRST_NAME);
		}

		@Test
		void loadAllPersonsWithWither(@Autowired PersonRepository repository) {

			List<PersonWithWither> persons = repository.getAllPersonsWithWitherViaQuery();

			assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
		}

		@Test
		void loadOnePersonWithWither(@Autowired PersonRepository repository) {

			PersonWithWither person = repository.getOnePersonWithWitherViaQuery();
			assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadOptionalPersonWithWither(@Autowired PersonRepository repository) {

			Optional<PersonWithWither> person = repository.getOptionalPersonWithWitherViaQuery();
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadAllKotlinPersons(@Autowired KotlinPersonRepository repository) {

			List<KotlinPerson> persons = repository.getAllKotlinPersonsViaQuery();
			assertThat(persons).anyMatch(person -> person.getName().equals(TEST_PERSON1_NAME));
		}

		@Test
		void loadOneKotlinPerson(@Autowired KotlinPersonRepository repository) {

			KotlinPerson person = repository.getOneKotlinPersonViaQuery();
			assertThat(person.getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void loadOptionalKotlinPerson(@Autowired KotlinPersonRepository repository) {

			Optional<KotlinPerson> person = repository.getOptionalKotlinPersonViaQuery();
			assertThat(person).isPresent();
			assertThat(person.get().getName()).isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void callCustomCypher(@Autowired PersonRepository repository) {

			Long fixedLong = repository.customQuery();
			assertThat(fixedLong).isEqualTo(1L);
		}

		@Test
		void findBySimpleProperty(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllBySameValue(TEST_PERSON_SAMEVALUE);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllBySameValueIgnoreCase(TEST_PERSON_SAMEVALUE.toUpperCase());
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllByBornOn(TEST_PERSON1_BORN_ON);
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findBySimplePropertyByEqualsWithNullShouldWork(@Autowired PersonRepository repository) {

			int emptyResultSize = 0;
			assertThat(repository.findAllBySameValue(null)).hasSize(emptyResultSize);
		}

		@Test
		void findByPropertyThatNeedsConversion(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> people = repository
					.findAllByPlace(new GeographicPoint2d(NEO4J_HQ.y(), NEO4J_HQ.x()));

			assertThat(people).hasSize(1);
		}

		@Test
		void findByPropertyFailsIfNoConverterIsAvailable(@Autowired PersonRepository repository) {
			assertThatExceptionOfType(ConverterNotFoundException.class)
					.isThrownBy(() -> repository.findAllByPlace(new ThingWithGeneratedId("hello")))
					.withMessageStartingWith("No converter found capable of converting from type");
		}

		@Test
		void findBySimplePropertiesAnded(@Autowired PersonRepository repository) {

			Optional<PersonWithAllConstructor> optionalPerson;

			optionalPerson = repository.findOneByNameAndFirstName(TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME);
			assertThat(optionalPerson).isPresent().contains(person1);

			optionalPerson = repository.findOneByNameAndFirstNameAllIgnoreCase(TEST_PERSON1_NAME.toUpperCase(),
					TEST_PERSON1_FIRST_NAME.toUpperCase());
			assertThat(optionalPerson).isPresent().contains(person1);
		}

		@Test // GH-112
		void findByPropertyWithPageable(@Autowired PersonRepository repository) {

			Page<PersonWithAllConstructor> people;

			Sort sort = Sort.by("name").descending();
			people = repository.findAllByNameOrName(PageRequest.of(0, 1, sort), TEST_PERSON1_NAME, TEST_PERSON2_NAME);
			assertThat(people.get()).hasSize(1).extracting("name").containsExactly(TEST_PERSON2_NAME);
			assertThat(people.getTotalPages()).isEqualTo(2);

			people = repository.findAllByNameOrName(PageRequest.of(1, 1, sort), TEST_PERSON1_NAME, TEST_PERSON2_NAME);
			assertThat(people.get()).hasSize(1).extracting("name").containsExactly(TEST_PERSON1_NAME);
			assertThat(people.getTotalPages()).isEqualTo(2);

			people = repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME, PageRequest.of(1, 1, sort));
			assertThat(people.get()).hasSize(1).extracting("name").containsExactly(TEST_PERSON1_NAME);
			assertThat(people.getTotalPages()).isEqualTo(2);
		}

		@Test
		void findBySimplePropertiesOred(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);
		}

		@Test // DATAGRAPH-1374
		void findSliceShouldWork(@Autowired PersonRepository repository) {

			Slice<PersonWithAllConstructor> slice = repository.findSliceByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME, PageRequest.of(0, 1, Sort.by("name").descending()));
			assertThat(slice.get()).hasSize(1).extracting("name").containsExactly(TEST_PERSON2_NAME);
			assertThat(slice.hasNext()).isTrue();

			slice = repository.findSliceByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME, slice.nextPageable());
			assertThat(slice.get()).hasSize(1).extracting("name").containsExactly(TEST_PERSON1_NAME);
			assertThat(slice.hasNext()).isFalse();
		}

		@Test // DATAGRAPH-1412
		void customFindMapsDeepRelationships(@Autowired PetRepository repository) {
			long petNode1Id;
			long petNode2Id;
			long petNode3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE " + "(p1:Pet{name: 'Pet1'})-[:Has]->(p2:Pet{name: 'Pet2'}), "
						+ "(p2)-[:Has]->(p3:Pet{name: 'Pet3'}) " + "RETURN p1, p2, p3").single();

				petNode1Id = record.get("p1").asNode().id();
				petNode2Id = record.get("p2").asNode().id();
				petNode3Id = record.get("p3").asNode().id();
			}

			Pet loadedPet = repository.customQueryWithDeepRelationshipMapping(petNode1Id);

			Pet comparisonPet2 = new Pet(petNode2Id, "Pet2");
			Pet comparisonPet3 = new Pet(petNode3Id, "Pet3");
			assertThat(loadedPet.getFriends()).containsExactlyInAnyOrder(comparisonPet2);

			Pet pet2 = loadedPet.getFriends().get(loadedPet.getFriends().indexOf(comparisonPet2));
			assertThat(pet2.getFriends()).containsExactly(comparisonPet3);
		}
	}

	@Nested
	class FindWithRelationships extends IntegrationTestBase {

		@Test
		void findEntityWithRelationship(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship loadedPerson = repository.findById(personId).get();
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
		void findDeepSameLabelsAndTypeRelationships(@Autowired PetRepository repository) {

			long petNode1Id;
			long petNode2Id;
			long petNode3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE " + "(p1:Pet{name: 'Pet1'})-[:Has]->(p2:Pet{name: 'Pet2'}), "
						+ "(p2)-[:Has]->(p3:Pet{name: 'Pet3'}) " + "RETURN p1, p2, p3").single();

				petNode1Id = record.get("p1").asNode().id();
				petNode2Id = record.get("p2").asNode().id();
				petNode3Id = record.get("p3").asNode().id();
			}

			Pet loadedPet = repository.findById(petNode1Id).get();
			Pet comparisonPet2 = new Pet(petNode2Id, "Pet2");
			Pet comparisonPet3 = new Pet(petNode3Id, "Pet3");
			assertThat(loadedPet.getFriends()).containsExactlyInAnyOrder(comparisonPet2);

			Pet pet2 = loadedPet.getFriends().get(loadedPet.getFriends().indexOf(comparisonPet2));
			assertThat(pet2.getFriends()).containsExactly(comparisonPet3);

		}

		@Test
		void findLoopingDeepRelationships(@Autowired LoopingRelationshipRepository loopingRelationshipRepository) {

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

			DeepRelationships.LoopingType1 type1 = loopingRelationshipRepository.findById(type1Id).get();

			DeepRelationships.LoopingType1 iteration1 = type1.nextType.nextType.nextType;
			assertThat(iteration1).isNotNull();
			DeepRelationships.LoopingType1 iteration2 = iteration1.nextType.nextType.nextType;
			assertThat(iteration2).isNotNull();
			DeepRelationships.LoopingType1 iteration3 = iteration2.nextType.nextType.nextType;
			assertThat(iteration3).isNotNull();
			DeepRelationships.LoopingType1 iteration4 = iteration3.nextType.nextType.nextType;
			assertThat(iteration4).isNotNull();
			DeepRelationships.LoopingType1 iteration5 = iteration4.nextType.nextType.nextType;
			assertThat(iteration5).isNotNull();
			DeepRelationships.LoopingType1 iteration6 = iteration5.nextType.nextType.nextType;
			assertThat(iteration6).isNotNull();
			DeepRelationships.LoopingType1 iteration7 = iteration6.nextType.nextType.nextType;
			assertThat(iteration7).isNotNull();
			DeepRelationships.LoopingType1 iteration8 = iteration7.nextType.nextType.nextType;
			assertThat(iteration8).isNotNull();
			DeepRelationships.LoopingType1 iteration9 = iteration8.nextType.nextType.nextType;
			assertThat(iteration9).isNotNull();
			DeepRelationships.LoopingType1 iteration10 = iteration9.nextType.nextType.nextType;
			assertThat(iteration10).isNotNull();
			assertThat(iteration10.nextType).isNull();
		}

		@Test
		void findEntityWithRelationshipToTheSameNode(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship loadedPerson = repository.findById(personId).get();
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
		void findEntityWithBidirectionalRelationship(@Autowired BidirectionalStartRepository repository) {

			long startId;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}), "
								+ "(e)<-[:ANOTHER_CONNECTION]-(anotherStart:BidirectionalStart{name:'Elmo'})" + "RETURN n")
						.single();

				Node startNode = record.get("n").asNode();
				startId = startNode.id();
			}

			Optional<BidirectionalStart> entityOptional = repository.findById(startId);
			assertThat(entityOptional).isPresent();
			BidirectionalStart entity = entityOptional.get();
			assertThat(entity.getEnds()).hasSize(1);

			BidirectionalEnd end = entity.getEnds().iterator().next();
			assertThat(end.getAnotherStart()).isNotNull();
			assertThat(end.getAnotherStart().getName()).isEqualTo("Elmo");

		}

		@Test
		void findEntityWithSelfReferencesInBothDirections(@Autowired PetRepository repository) {
			long petId;
			try (Session session = createSession()) {
				petId = session.run("CREATE (luna:Pet{name:'Luna'})-[:Has]->(daphne:Pet{name:'Daphne'})"
						+ "-[:Has]->(luna2:Pet{name:'Luna'})" + "RETURN id(luna) as id").single().get("id").asLong();
			}
			Pet loadedPet = repository.findById(petId).get();

			assertThat(loadedPet.getFriends().get(0).getName()).isEqualTo("Daphne");
			assertThat(loadedPet.getFriends().get(0).getFriends().get(0).getName()).isEqualTo("Luna");

		}

		@Test
		void findEntityWithBidirectionalRelationshipFromIncomingSide(@Autowired BidirectionalEndRepository repository) {

			long endId;

			try (Session session = createSession()) {
				Record record = session.run(
						"CREATE (n:BidirectionalStart{name:'Ernie'})-[:CONNECTED]->(e:BidirectionalEnd{name:'Bert'}) " + "RETURN e")
						.single();

				Node endNode = record.get("e").asNode();
				endId = endNode.id();
			}

			Optional<BidirectionalEnd> entityOptional = repository.findById(endId);
			assertThat(entityOptional).isPresent();
			BidirectionalEnd entity = entityOptional.get();
			assertThat(entity.getStart()).isNotNull();

		}

		@Test
		void findMultipleEntitiesWithRelationship(@Autowired RelationshipRepository repository) {

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

			List<PersonWithRelationship> loadedPersons = repository.findAll();

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
		void findEntityWithRelationshipViaQuery(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship loadedPerson = repository.getPersonWithRelationshipsViaQuery();
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
		void findEntityWithRelationshipWithAssignedId(@Autowired PetRepository repository) {

			long petNodeId;

			try (Session session = createSession()) {
				Record record = session
						.run("CREATE (p:Pet{name:'Jerry'})-[:Has]->(t:Thing{theId:'t1', name:'Thing1'}) " + "RETURN p, t").single();

				Node petNode = record.get("p").asNode();
				petNodeId = petNode.id();
			}

			Pet pet = repository.findById(petNodeId).get();
			ThingWithAssignedId relatedThing = pet.getThings().get(0);
			assertThat(relatedThing.getTheId()).isEqualTo("t1");
			assertThat(relatedThing.getName()).isEqualTo("Thing1");
		}

	}

	@Nested
	class RelationshipProperties extends IntegrationTestBase {

		@Test // DATAGRAPH-1397
		void shouldBeStorableOnSets(
				@Autowired Neo4jTemplate template) {

			long personId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:PersonWithRelationshipWithProperties2{name:'Freddie'}),"
						+ " (n)-[l1:LIKES "
						+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
						+ "]->(h1:Hobby{name:'Music'}), "
						+ "(n)-[l2:LIKES "
						+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
						+ "]->(h2:Hobby{name:'Something else'})"
						+ "RETURN n, h1, h2").single();

				Node personNode = record.get("n").asNode();
				personId = personNode.id();
			}

			Optional<PersonWithRelationshipWithProperties2> optionalPerson = template.findById(personId, PersonWithRelationshipWithProperties2.class);
			assertThat(optionalPerson).hasValueSatisfying(person -> {
				assertThat(person.getName()).isEqualTo("Freddie");
				assertThat(person.getHobbies()).hasSize(2).extracting(LikesHobbyRelationship::getSince).containsExactlyInAnyOrder(1995, 2000);
			});
		}

		@Test
		void findEntityWithRelationshipWithProperties(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {

			long personId;
			long hobbyNode1Id;
			long hobbyNode2Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
						+ " (n)-[l1:LIKES "
						+ "{since: 1995, active: true, localDate: date('1995-02-26'), myEnum: 'SOMETHING', point: point({x: 0, y: 1})}"
						+ "]->(h1:Hobby{name:'Music'}), "
						+ "(n)-[l2:LIKES "
						+ "{since: 2000, active: false, localDate: date('2000-06-28'), myEnum: 'SOMETHING_DIFFERENT', point: point({x: 2, y: 3})}"
						+ "]->(h2:Hobby{name:'Something else'}), "
						+ "(n) - [:OWNS] -> (p:Pet {name: 'A Pet'}), "
						+ "(n) - [:OWNS {place: 'The place to be'}] -> (c1:Club {name: 'Berlin Mitte'}), "
						+ "(n) - [:OWNS {place: 'Whatever'}] -> (c2:Club {name: 'Schachklub'}) "
						+ "RETURN n, h1, h2").single();

				Node personNode = record.get("n").asNode();
				Node hobbyNode1 = record.get("h1").asNode();
				Node hobbyNode2 = record.get("h2").asNode();

				personId = personNode.id();
				hobbyNode1Id = hobbyNode1.id();
				hobbyNode2Id = hobbyNode2.id();
			}

			Optional<PersonWithRelationshipWithProperties> optionalPerson = repository.findById(personId);
			assertThat(optionalPerson).isPresent();
			PersonWithRelationshipWithProperties person = optionalPerson.get();
			assertThat(person.getName()).isEqualTo("Freddie");
			assertThat(person.getPets()).hasSize(1).first().extracting(Pet::getName).isEqualTo("A Pet");

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

			List<LikesHobbyRelationship> hobbies = person.getHobbies();
			assertThat(hobbies).containsExactlyInAnyOrder(rel1, rel2);
			assertThat(hobbies.get(hobbies.indexOf(rel1)).getHobby()).isEqualTo(hobby1);
			assertThat(hobbies.get(hobbies.indexOf(rel2)).getHobby()).isEqualTo(hobby2);

			assertThat(person.getClubs()).hasSize(2)
					.extracting(ClubRelationship::getPlace)
					.containsExactlyInAnyOrder("The place to be", "Whatever");
		}

		@Test
		void findEntityWithRelationshipWithPropertiesScalar(@Autowired PersonWithRelationshipWithPropertiesRepository repository) {

			long personId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:PersonWithRelationshipWithProperties{name:'Freddie'}),"
						+ " (n)-[:WORKS_IN{since: 1995}]->(:Club{name:'Blubb'}),"
						+ "(n) - [:OWNS {place: 'The place to be'}] -> (c1:Club {name: 'Berlin Mitte'}), "
						+ "(n) - [:OWNS {place: 'Whatever'}] -> (c2:Club {name: 'Schachklub'}) "
						+ "RETURN n").single();

				Node personNode = record.get("n").asNode();
				personId = personNode.id();
			}

			PersonWithRelationshipWithProperties person = repository.findById(personId).get();

			WorksInClubRelationship loadedRelationship = person.getClub();
			assertThat(loadedRelationship.getSince()).isEqualTo(1995);
			assertThat(loadedRelationship.getClub().getName()).isEqualTo("Blubb");
		}

		@Test
		void findEntityWithRelationshipWithPropertiesSameLabel(
				@Autowired FriendRepository repository) {

			long friendId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:Friend{name:'Freddie'}),"
						+ " (n)-[:KNOWS{since: 1995}]->(:Friend{name:'Frank'})"
						+ "RETURN n").single();

				Node friendNode = record.get("n").asNode();
				friendId = friendNode.id();
			}

			Friend person = repository.findById(friendId).get();

			List<FriendshipRelationship> loadedRelationship = person.getFriends();
			assertThat(loadedRelationship).allSatisfy(relationship -> {
				assertThat(relationship.getSince()).isEqualTo(1995);
				assertThat(relationship.getFriend().getName()).isEqualTo("Frank");
			});
		}

		@Test
		void saveEntityWithRelationshipWithProperties(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {
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
			rel1.setHobby(h1);

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
			rel2.setHobby(h2);

			List<LikesHobbyRelationship> hobbies = new ArrayList<>();
			hobbies.add(rel1);
			hobbies.add(rel2);

			Club club = new Club();
			club.setName("BlubbClub");
			WorksInClubRelationship worksInClub = new WorksInClubRelationship(2002, club);
			PersonWithRelationshipWithProperties person =
					new PersonWithRelationshipWithProperties("Freddie clone", hobbies, worksInClub);

			// when
			PersonWithRelationshipWithProperties shouldBeDifferentPerson = repository.save(person);

			// then
			assertThat(shouldBeDifferentPerson).isNotNull().isEqualToComparingOnlyGivenFields(person, "hobbies");

			assertThat(shouldBeDifferentPerson.getName()).isEqualToIgnoringCase("Freddie clone");

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:PersonWithRelationshipWithProperties {name:'Freddie clone'}) "
						+ "RETURN n, " + "[(n) -[:LIKES]->(h:Hobby) |h] as Hobbies, " + "[(n) -[r:LIKES]->(:Hobby) |r] as rels")
						.single();

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
			}
		}

		@Test
		void findEntityWithRelationshipWithPropertiesFromCustomQuery(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {

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

			PersonWithRelationshipWithProperties person = repository.loadFromCustomQuery(personId);
			assertThat(person.getName()).isEqualTo("Freddie");

			Hobby hobby1 = new Hobby();
			hobby1.setName("Music");
			hobby1.setId(hobbyNode1Id);
			LikesHobbyRelationship rel1 = new LikesHobbyRelationship(1995);
			rel1.setActive(true);
			rel1.setLocalDate(LocalDate.of(1995, 2, 26));
			rel1.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING);
			rel1.setPoint(new CartesianPoint2d(0d, 1d));
			rel1.setHobby(hobby1);

			Hobby hobby2 = new Hobby();
			hobby2.setName("Something else");
			hobby2.setId(hobbyNode2Id);
			LikesHobbyRelationship rel2 = new LikesHobbyRelationship(2000);
			rel2.setActive(false);
			rel2.setLocalDate(LocalDate.of(2000, 6, 28));
			rel2.setMyEnum(LikesHobbyRelationship.MyEnum.SOMETHING_DIFFERENT);
			rel2.setPoint(new CartesianPoint2d(2d, 3d));
			rel2.setHobby(hobby2);

			List<LikesHobbyRelationship> hobbies = person.getHobbies();
			assertThat(hobbies).containsExactlyInAnyOrder(rel1, rel2);
			assertThat(hobbies.get(hobbies.indexOf(rel1)).getHobby()).isEqualTo(hobby1);
			assertThat(hobbies.get(hobbies.indexOf(rel2)).getHobby()).isEqualTo(hobby2);
		}

		@Test // DATAGRAPH-1350
		void loadEntityWithRelationshipWithPropertiesFromCustomQueryIncoming(
				@Autowired HobbyWithRelationshipWithPropertiesRepository repository) {

			long personId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:AltPerson{name:'Freddie'}), (n)-[l1:LIKES {rating: 5}]->(h1:AltHobby{name:'Music'}) RETURN n, h1").single();
				personId = record.get("n").asNode().id();
			}

			AltHobby hobby = repository.loadFromCustomQuery(personId);
			assertThat(hobby.getName()).isEqualTo("Music");
			assertThat(hobby.getLikedBy()).hasSize(1);
			assertThat(hobby.getLikedBy()).first().satisfies(entry -> {
				assertThat(entry.getAltPerson().getId()).isEqualTo(personId);
				assertThat(entry.getRating()).isEqualTo(5);
			});
		}

		@Test
		void loadSameNodeWithDoubleRelationship(@Autowired HobbyWithRelationshipWithPropertiesRepository repository) {
			long personId;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n:AltPerson{name:'Freddie'})," +
						" (n)-[l1:LIKES {rating: 5}]->(h1:AltHobby{name:'Music'})," +
						" (n)-[l2:LIKES {rating: 1}]->(h1)" +
						" RETURN n, h1").single();
				personId = record.get("n").asNode().id();
			}

			AltHobby hobby = repository.loadFromCustomQuery(personId);
			assertThat(hobby.getName()).isEqualTo("Music");
			List<AltLikedByPersonRelationship> likedBy = hobby.getLikedBy();
			assertThat(likedBy).hasSize(2);

			AltPerson altPerson = new AltPerson("Freddie");
			altPerson.setId(personId);
			AltLikedByPersonRelationship rel1 = new AltLikedByPersonRelationship();
			rel1.setRating(5);
			rel1.setAltPerson(altPerson);

			AltLikedByPersonRelationship rel2 = new AltLikedByPersonRelationship();
			rel2.setRating(1);
			rel2.setAltPerson(altPerson);

			assertThat(likedBy).containsExactlyInAnyOrder(rel1, rel2);
		}
	}

	@Nested
	class Save extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			ZonedDateTime createdAt = LocalDateTime.of(2019, 1, 1, 23, 23, 42, 0).atZone(ZoneOffset.UTC.normalized());
			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place, n.createdAt = $createdAt "
					+ "RETURN id(n)",
					Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ, "createdAt", createdAt))
					.next().get(0).asLong();
			transaction.run("CREATE (a:Thing {theId: 'anId', name: 'Homer'})-[:Has]->(b:Thing2{theId: 4711, name: 'Bart'})");
			IntStream.rangeClosed(1, 20)
					.forEach(i -> transaction.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})",
							Values.parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, createdAt.toInstant());
		}

		@Test
		void saveSingleEntity(@Autowired PersonRepository repository) {

			PersonWithAllConstructor person = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
					LocalDate.of(1946, 9, 15), null, Arrays.asList("b", "a"), null, null);
			PersonWithAllConstructor savedPerson = repository.save(person);
			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:PersonWithAllConstructor) WHERE n.first_name = $first_name RETURN n",
						Values.parameters("first_name", "Freddie")).single();

				assertThat(record.containsKey("n")).isTrue();
				Node node = record.get("n").asNode();
				assertThat(savedPerson.getId()).isEqualTo(node.id());
				assertThat(node.get("things").asList()).containsExactly("b", "a");
			}
		}

		@Test
		void saveAll(@Autowired PersonRepository repository) {

			PersonWithAllConstructor newPerson = new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true,
					1509L, LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null, null);

			PersonWithAllConstructor existingPerson = repository.findById(id1).get();
			existingPerson.setFirstName("Updated first name");
			existingPerson.setNullable("Updated nullable field");

			assertThat(repository.count()).isEqualTo(1);

			List<Long> ids = StreamSupport
					.stream(repository.saveAll(Arrays.asList(existingPerson, newPerson)).spliterator(), false)
					.map(PersonWithAllConstructor::getId).collect(Collectors.toList());

			assertThat(repository.count()).isEqualTo(2);

			try (Session session = createSession()) {

				Record record = session.run(
						"MATCH (n:PersonWithAllConstructor) WHERE id(n) IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names",
						Values.parameters("ids", ids)).single();

				assertThat(record.containsKey("names")).isTrue();
				List<String> names = record.get("names").asList(Value::asString);
				assertThat(names).contains("Mercury", TEST_PERSON1_NAME);
			}
		}

		@Test
		void updateSingleEntity(@Autowired PersonRepository repository) {

			PersonWithAllConstructor originalPerson = repository.findById(id1).get();
			originalPerson.setFirstName("Updated first name");
			originalPerson.setNullable("Updated nullable field");
			assertThat(originalPerson.getThings()).isNotEmpty();
			originalPerson.setThings(Collections.emptyList());

			PersonWithAllConstructor savedPerson = repository.save(originalPerson);
			try (Session session = createSession()) {
				session.readTransaction(tx -> {
					Record record = tx
							.run("MATCH (n:PersonWithAllConstructor) WHERE id(n) = $id RETURN n", Values.parameters("id", id1))
							.single();

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
		void saveWithAssignedId(@Autowired ThingRepository repository) {

			assertThat(repository.count()).isEqualTo(21);

			ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
			thing.setName("That's the thing.");
			thing = repository.save(thing);

			try (Session session = createSession()) {
				Record record = session
						.run("MATCH (n:Thing) WHERE n.theId = $id RETURN n", Values.parameters("id", thing.getTheId())).single();

				assertThat(record.containsKey("n")).isTrue();
				Node node = record.get("n").asNode();
				assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
				assertThat(node.get("name").asString()).isEqualTo(thing.getName());

				assertThat(repository.count()).isEqualTo(22);
			}
		}

		@Test
		void saveAllWithAssignedId(@Autowired ThingRepository repository) {

			assertThat(repository.count()).isEqualTo(21);

			ThingWithAssignedId newThing = new ThingWithAssignedId("aaBB");
			newThing.setName("That's the thing.");

			ThingWithAssignedId existingThing = repository.findById("anId").get();
			existingThing.setName("Updated name.");

			repository.saveAll(Arrays.asList(newThing, existingThing));

			try (Session session = createSession()) {
				Record record = session
						.run("MATCH (n:Thing) WHERE n.theId IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names",
								Values.parameters("ids", Arrays.asList(newThing.getTheId(), existingThing.getTheId())))
						.single();

				assertThat(record.containsKey("names")).isTrue();
				List<String> names = record.get("names").asList(Value::asString);
				assertThat(names).containsExactly(newThing.getName(), existingThing.getName());

				assertThat(repository.count()).isEqualTo(22);
			}
		}

		@Test
		void updateWithAssignedId(@Autowired ThingRepository repository) {

			assertThat(repository.count()).isEqualTo(21);

			ThingWithAssignedId thing = new ThingWithAssignedId("id07");
			thing.setName("An updated thing");
			repository.save(thing);

			thing = repository.findById("id15").get();
			thing.setName("Another updated thing");
			repository.save(thing);

			try (Session session = createSession()) {
				Record record = session
						.run("MATCH (n:Thing) WHERE n.theId IN ($ids) WITH n ORDER BY n.name ASC RETURN COLLECT(n.name) as names",
								Values.parameters("ids", Arrays.asList("id07", "id15")))
						.single();

				assertThat(record.containsKey("names")).isTrue();
				List<String> names = record.get("names").asList(Value::asString);
				assertThat(names).containsExactly("An updated thing", "Another updated thing");

				assertThat(repository.count()).isEqualTo(21);
			}
		}

		@Test
		void saveWithConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			EntityWithConvertedId entity = new EntityWithConvertedId();
			entity.setIdentifyingEnum(EntityWithConvertedId.IdentifyingEnum.A);
			repository.save(entity);

			try (Session session = createSession()) {
				Record node = session.run("MATCH (e:EntityWithConvertedId) return e").next();
				assertThat(node.get("e").get("identifyingEnum").asString()).isEqualTo("A");
			}
		}

		@Test
		void saveAllWithConvertedId(@Autowired EntityWithConvertedIdRepository repository) {
			EntityWithConvertedId entity = new EntityWithConvertedId();
			entity.setIdentifyingEnum(EntityWithConvertedId.IdentifyingEnum.A);
			repository.saveAll(Collections.singleton(entity));

			try (Session session = createSession()) {
				Record node = session.run("MATCH (e:EntityWithConvertedId) return e").next();
				assertThat(node.get("e").get("identifyingEnum").asString()).isEqualTo("A");
			}
		}
	}

	@Nested
	class SaveWithRelationships extends IntegrationTestBase {

		@Test
		void saveSingleEntityWithRelationships(@Autowired RelationshipRepository repository) {

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
			pet1.setHobbies(Collections.singleton(petHobby));
			person.setPets(Arrays.asList(pet1, pet2));

			PersonWithRelationship savedPerson = repository.save(person);
			try (Session session = createSession()) {

				Record record = session.run(
						"MATCH (n:PersonWithRelationship)" + " RETURN n,"
								+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
								+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies, " + " [(n)<-[:Has]-(c:Club) | c] as clubs",
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

				assertThat(pets.keySet().stream().map(pet -> ((Node) pet).get("name").asString()).collect(
						Collectors.toList()))
						.containsExactlyInAnyOrder("Jerry", "Tom");

				assertThat(pets.values().stream()
						.flatMap(petHobbies -> petHobbies.stream().map(node -> node.get("name").asString())).collect(
								Collectors.toList()))
								.containsExactlyInAnyOrder("sleeping");

				assertThat(record.get("hobbies").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("Music");

				assertThat(record.get("clubs").asList(entry -> entry.asNode().get("name").asString()))
						.containsExactlyInAnyOrder("ClownsClub");
			}
		}

		@Test
		void saveSingleEntityWithRelationshipsTwiceDoesNotCreateMoreRelationships(
				@Autowired RelationshipRepository repository) {

			PersonWithRelationship person = new PersonWithRelationship();
			person.setName("Freddie");
			Hobby hobby = new Hobby();
			hobby.setName("Music");
			person.setHobbies(hobby);
			Pet pet1 = new Pet("Jerry");
			Pet pet2 = new Pet("Tom");
			Hobby petHobby = new Hobby();
			petHobby.setName("sleeping");
			pet1.setHobbies(Collections.singleton(petHobby));
			person.setPets(Arrays.asList(pet1, pet2));

			PersonWithRelationship savedPerson = repository.save(person);
			savedPerson = repository.save(savedPerson);
			try (Session session = createSession()) {

				List<Record> recordList = session.run("MATCH (n:PersonWithRelationship)" + " RETURN n,"
						+ " [(n)-[:Has]->(p:Pet) | [ p , [ (p)-[:Has]-(h:Hobby) | h ] ] ] as petsWithHobbies,"
						+ " [(n)-[:Has]->(h:Hobby) | h] as hobbies", Values.parameters("name", "Freddie")).list();

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

				assertThat(pets.keySet().stream().map(pet -> ((Node) pet).get("name").asString()).collect(
						Collectors.toList()))
						.containsExactlyInAnyOrder("Jerry", "Tom");

				assertThat(pets.values().stream()
						.flatMap(petHobbies -> petHobbies.stream().map(node -> node.get("name").asString())).collect(
								Collectors.toList()))
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
		void saveEntityWithAlreadyExistingTargetNode(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship savedPerson = repository.save(person);
			try (Session session = createSession()) {

				List<Record> recordList = session
						.run("MATCH (n:PersonWithRelationship)" + " RETURN n," + " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
								Values.parameters("name", "Freddie"))
						.list();

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
		void saveEntityWithAlreadyExistingSourceAndTargetNode(@Autowired RelationshipRepository repository) {

			Long hobbyId;
			Long personId;

			try (Session session = createSession()) {
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

			PersonWithRelationship savedPerson = repository.save(person);
			try (Session session = createSession()) {

				List<Record> recordList = session
						.run("MATCH (n:PersonWithRelationship)" + " RETURN n," + " [(n)-[:Has]->(h:Hobby) | h] as hobbies",
								Values.parameters("name", "Freddie"))
						.list();

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
		void saveEntityWithDeepSelfReferences(@Autowired PetRepository repository) {
			Pet rootPet = new Pet("Luna");
			Pet petOfRootPet = new Pet("Daphne");
			Pet petOfChildPet = new Pet("Mucki");
			Pet petOfGrandChildPet = new Pet("Blacky");

			rootPet.setFriends(Collections.singletonList(petOfRootPet));
			petOfRootPet.setFriends(Collections.singletonList(petOfChildPet));
			petOfChildPet.setFriends(Collections.singletonList(petOfGrandChildPet));

			repository.save(rootPet);

			try (Session session = createSession()) {
				Record record = session.run("MATCH (rootPet:Pet)-[:Has]->(petOfRootPet:Pet)-[:Has]->(petOfChildPet:Pet)"
						+ "-[:Has]->(petOfGrandChildPet:Pet) " + "RETURN rootPet, petOfRootPet, petOfChildPet, petOfGrandChildPet",
						Collections.emptyMap()).single();

				assertThat(record.get("rootPet").asNode().get("name").asString()).isEqualTo("Luna");
				assertThat(record.get("petOfRootPet").asNode().get("name").asString()).isEqualTo("Daphne");
				assertThat(record.get("petOfChildPet").asNode().get("name").asString()).isEqualTo("Mucki");
				assertThat(record.get("petOfGrandChildPet").asNode().get("name").asString()).isEqualTo("Blacky");
			}
		}

		@Test
		void saveEntityWithSelfReferencesInBothDirections(@Autowired PetRepository repository) {
			Pet luna = new Pet("Luna");
			Pet daphne = new Pet("Daphne");

			luna.setFriends(Collections.singletonList(daphne));
			daphne.setFriends(Collections.singletonList(luna));

			repository.save(luna);

			try (Session session = createSession()) {
				Record record = session.run("MATCH (luna:Pet{name:'Luna'})-[:Has]->(daphne:Pet{name:'Daphne'})"
						+ "-[:Has]->(luna2:Pet{name:'Luna'})" + "RETURN luna, daphne, luna2").single();

				assertThat(record.get("luna").asNode().get("name").asString()).isEqualTo("Luna");
				assertThat(record.get("daphne").asNode().get("name").asString()).isEqualTo("Daphne");
				assertThat(record.get("luna2").asNode().get("name").asString()).isEqualTo("Luna");
			}
		}

		@Test
		void saveEntityGraphWithSelfInverseRelationshipDefined(@Autowired SimilarThingRepository repository) {
			SimilarThing originalThing = new SimilarThing().withName("Original");
			SimilarThing similarThing = new SimilarThing().withName("Similar");

			originalThing.setSimilar(similarThing);
			similarThing.setSimilarOf(originalThing);
			repository.save(originalThing);

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
		void saveWithAssignedIdAndRelationship(@Autowired ThingRepository repository) {

			ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
			thing.setName("That's the thing.");
			AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
			anotherThing.setName("AnotherThing");
			thing.setThings(Collections.singletonList(anotherThing));
			thing = repository.save(thing);

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:Thing)-[:Has]->(t:Thing2) WHERE n.theId = $id RETURN n, t",
						Values.parameters("id", thing.getTheId())).single();

				assertThat(record.containsKey("n")).isTrue();
				assertThat(record.containsKey("t")).isTrue();
				Node node = record.get("n").asNode();
				assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
				assertThat(node.get("name").asString()).isEqualTo(thing.getName());

				Node relatedNode = record.get("t").asNode();
				assertThat(relatedNode.get("theId").asLong()).isEqualTo(anotherThing.getTheId());
				assertThat(relatedNode.get("name").asString()).isEqualTo(anotherThing.getName());
				assertThat(repository.count()).isEqualTo(1);
			}
		}

		@Test
		void saveAllWithAssignedIdAndRelationship(@Autowired ThingRepository repository) {

			ThingWithAssignedId thing = new ThingWithAssignedId("aaBB");
			thing.setName("That's the thing.");
			AnotherThingWithAssignedId anotherThing = new AnotherThingWithAssignedId(4711L);
			anotherThing.setName("AnotherThing");
			thing.setThings(Collections.singletonList(anotherThing));
			repository.saveAll(Collections.singletonList(thing));

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:Thing)-[:Has]->(t:Thing2) WHERE n.theId = $id RETURN n, t",
						Values.parameters("id", thing.getTheId())).single();

				assertThat(record.containsKey("n")).isTrue();
				assertThat(record.containsKey("t")).isTrue();
				Node node = record.get("n").asNode();
				assertThat(node.get("theId").asString()).isEqualTo(thing.getTheId());
				assertThat(node.get("name").asString()).isEqualTo(thing.getName());

				Node relatedNode = record.get("t").asNode();
				assertThat(relatedNode.get("theId").asLong()).isEqualTo(anotherThing.getTheId());
				assertThat(relatedNode.get("name").asString()).isEqualTo(anotherThing.getName());
				assertThat(repository.count()).isEqualTo(1);
			}
		}

		@Test
		void createComplexSameClassRelationshipsBeforeRootObject(
				@Autowired ImmutablePersonRepository immutablePersonRepository) {

			ImmutablePerson p1 = new ImmutablePerson("Person1", Collections.emptyList());
			ImmutablePerson p2 = new ImmutablePerson("Person2", Arrays.asList(p1));
			ImmutablePerson p3 = new ImmutablePerson("Person3", Arrays.asList(p2));
			ImmutablePerson p4 = new ImmutablePerson("Person4", Arrays.asList(p1, p3));

			immutablePersonRepository.saveAll(Arrays.asList(p4));

			List<ImmutablePerson> people = immutablePersonRepository.findAll();

			assertThat(people).hasSize(4);

		}
	}

	@Nested
	class Delete extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			id1 = transaction.run("CREATE (n:PersonWithAllConstructor) RETURN id(n)").next().get(0).asLong();
			id2 = transaction.run("CREATE (n:PersonWithAllConstructor) RETURN id(n)").next().get(0).asLong();

			person1 = new PersonWithAllConstructor(id1, null, null, null, null, null, null, null, null, null, null);
			person2 = new PersonWithAllConstructor(id2, null, null, null, null, null, null, null, null, null, null);
		}

		@Test
		void delete(@Autowired PersonRepository repository) {

			repository.delete(person1);

			assertThat(repository.existsById(id1)).isFalse();
			assertThat(repository.existsById(id2)).isTrue();
		}

		@Test
		void deleteById(@Autowired PersonRepository repository) {

			repository.deleteById(id1);

			assertThat(repository.existsById(id1)).isFalse();
			assertThat(repository.existsById(id2)).isTrue();
		}

		@Test
		void deleteAllEntities(@Autowired PersonRepository repository) {

			repository.deleteAll(Arrays.asList(person1, person2));

			assertThat(repository.existsById(id1)).isFalse();
			assertThat(repository.existsById(id2)).isFalse();
		}

		@Test
		void deleteAll(@Autowired PersonRepository repository) {

			repository.deleteAll();
			assertThat(repository.count()).isEqualTo(0L);
		}

		@Test
		void deleteSimpleRelationship(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(h1:Hobby{name:'Music'})");
			}

			PersonWithRelationship person = repository.getPersonWithRelationshipsViaQuery();
			person.setHobbies(null);
			repository.save(person);
			person = repository.getPersonWithRelationshipsViaQuery();

			assertThat(person.getHobbies()).isNull();
		}

		@Test
		void deleteCollectionRelationship(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'}), "
						+ "(n)-[:Has]->(p1:Pet{name: 'Jerry'}), (n)-[:Has]->(p2:Pet{name: 'Tom'})");
			}

			PersonWithRelationship person = repository.getPersonWithRelationshipsViaQuery();
			person.getPets().remove(0);
			repository.save(person);
			person = repository.getPersonWithRelationshipsViaQuery();

			assertThat(person.getPets()).hasSize(1);
		}

	}

	@Nested
	class ByExample extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			ZonedDateTime createdAt = LocalDateTime.of(2019, 1, 1, 23, 23, 42, 0).atZone(ZoneOffset.UTC.normalized());
			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place, n.createdAt = $createdAt "
					+ "RETURN id(n)",
					Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ, "createdAt", createdAt))
					.next().get(0).asLong();
			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, createdAt.toInstant());
			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void findOneByExample(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(person1,
					ExampleMatcher.matchingAll().withIgnoreNullValues());
			Optional<PersonWithAllConstructor> person = repository.findOne(example);

			assertThat(person).isPresent();
			assertThat(person.get()).isEqualTo(person1);
		}

		@Test
		void findAllByExample(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(person1,
					ExampleMatcher.matchingAll().withIgnoreNullValues());
			List<PersonWithAllConstructor> persons = repository.findAll(example);

			assertThat(persons).containsExactly(person1);
		}

		@Test
		void findAllByExampleWithDifferentMatchers(@Autowired PersonRepository repository) {

			PersonWithAllConstructor person;
			Example<PersonWithAllConstructor> example;
			List<PersonWithAllConstructor> persons;

			person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME, TEST_PERSON2_FIRST_NAME, null, null, null, null,
					null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAny());

			persons = repository.findAll(example);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			person = new PersonWithAllConstructor(null, TEST_PERSON1_NAME.toUpperCase(), TEST_PERSON2_FIRST_NAME, null, null,
					null, null, null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAny().withIgnoreCase("name"));

			persons = repository.findAll(example);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			person = new PersonWithAllConstructor(null,
					TEST_PERSON2_NAME.substring(TEST_PERSON2_NAME.length() - 2).toUpperCase(),
					TEST_PERSON2_FIRST_NAME.substring(0, 2), TEST_PERSON_SAMEVALUE.substring(3, 5), null, null, null, null, null,
					null, null);
			example = Example.of(person,
					ExampleMatcher.matchingAll()
							.withMatcher("name", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.ENDING, true))
							.withMatcher("firstName", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.STARTING))
							.withMatcher("sameValue", ExampleMatcher.GenericPropertyMatcher.of(StringMatcher.CONTAINING)));

			persons = repository.findAll(example);
			assertThat(persons).containsExactlyInAnyOrder(person2);

			person = new PersonWithAllConstructor(null, null, "(?i)ern.*", null, null, null, null, null, null, null, null);
			example = Example.of(person, ExampleMatcher.matchingAll().withStringMatcher(StringMatcher.REGEX));

			persons = repository.findAll(example);
			assertThat(persons).containsExactlyInAnyOrder(person1);

			example = Example.of(person,
					ExampleMatcher.matchingAll().withStringMatcher(StringMatcher.REGEX).withIncludeNullValues());

			persons = repository.findAll(example);
			assertThat(persons).isEmpty();
		}

		@Test
		void findAllByExampleWithSort(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
			List<PersonWithAllConstructor> persons = repository.findAll(example, Sort.by(Sort.Direction.DESC, "name"));

			assertThat(persons).containsExactly(person2, person1);
		}

		@Test
		void findAllByExampleWithPagination(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
			Iterable<PersonWithAllConstructor> persons = repository.findAll(example, PageRequest.of(1, 1, Sort.by("name")));

			assertThat(persons).containsExactly(person2);
		}

		@Test
		void existsByExample(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(personExample(TEST_PERSON_SAMEVALUE));
			boolean exists = repository.exists(example);

			assertThat(exists).isTrue();
		}

		@Test
		void countByExample(@Autowired PersonRepository repository) {

			Example<PersonWithAllConstructor> example = Example.of(person1);
			long count = repository.count(example);

			assertThat(count).isEqualTo(1);
		}

		@Test
		void findEntityWithRelationshipByFindOneByExample(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship probe = new PersonWithRelationship();
			probe.setName("Freddie");
			PersonWithRelationship loadedPerson = repository.findOne(Example.of(probe)).get();
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
		void findEntityWithRelationshipByFindAllByExample(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship probe = new PersonWithRelationship();
			probe.setName("Freddie");
			PersonWithRelationship loadedPerson = repository.findAll(Example.of(probe)).get(0);
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
		void findEntityWithRelationshipByFindAllByExampleWithSort(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship probe = new PersonWithRelationship();
			probe.setName("Freddie");
			PersonWithRelationship loadedPerson = repository.findAll(Example.of(probe), Sort.by("name")).get(0);
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
		void findEntityWithRelationshipByFindAllByExampleWithPageable(@Autowired RelationshipRepository repository) {

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

			PersonWithRelationship probe = new PersonWithRelationship();
			probe.setName("Freddie");
			PersonWithRelationship loadedPerson = repository.findAll(Example.of(probe),  PageRequest.of(0, 1, Sort.by("name"))).toList().get(0);
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

	}

	@Nested
	class FinderMethodKeywords extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			ZonedDateTime createdAt = LocalDateTime.of(2019, 1, 1, 23, 23, 42, 0).atZone(ZoneOffset.UTC.normalized());
			id1 = transaction.run("" + "CREATE (n:PersonWithAllConstructor) "
					+ "  SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.nullable = 'something', n.things = ['a', 'b'], n.place = $place, n.createdAt = $createdAt "
					+ "RETURN id(n)",
					Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME, "cool", true, "personNumber", 1, "bornOn", TEST_PERSON1_BORN_ON, "place",
							NEO4J_HQ, "createdAt", createdAt))
					.next().get(0).asLong();
			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName, n.cool = $cool, n.personNumber = $personNumber, n.bornOn = $bornOn, n.things = [], n.place = $place return id(n)",
					Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME, "cool", false, "personNumber", 2, "bornOn", TEST_PERSON2_BORN_ON, "place", SFO))
					.next().get(0).asLong();

			IntStream.rangeClosed(1, 20)
					.forEach(i -> transaction.run("CREATE (a:Thing {theId: 'id' + $i, name: 'name' + $i})",
							Values.parameters("i", String.format("%02d", i))));

			person1 = new PersonWithAllConstructor(id1, TEST_PERSON1_NAME, TEST_PERSON1_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					true, 1L, TEST_PERSON1_BORN_ON, "something", Arrays.asList("a", "b"), NEO4J_HQ, createdAt.toInstant());
			person2 = new PersonWithAllConstructor(id2, TEST_PERSON2_NAME, TEST_PERSON2_FIRST_NAME, TEST_PERSON_SAMEVALUE,
					false, 2L, TEST_PERSON2_BORN_ON, null, Collections.emptyList(), SFO, null);
		}

		@Test
		void findByNegatedSimpleProperty(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByNameNot(TEST_PERSON1_NAME);
			assertThat(persons).doesNotContain(person1);

			persons = repository.findAllByNameNotIgnoreCase(TEST_PERSON1_NAME.toUpperCase());
			assertThat(persons).doesNotContain(person1);
		}

		@Test
		void findByTrueAndFalse(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> coolPeople = repository.findAllByCoolTrue();
			List<PersonWithAllConstructor> theRest = repository.findAllByCoolFalse();
			assertThat(coolPeople).doesNotContain(person2);
			assertThat(theRest).doesNotContain(person1);
		}

		@Test
		void findByLike(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameLike("Ern");
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByFirstNameLikeIgnoreCase("eRN");
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByMatches(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByFirstNameMatches("(?i)ern.*");
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByNotLike(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameNotLike("Ern");
			assertThat(persons).doesNotContain(person1);

			persons = repository.findAllByFirstNameNotLikeIgnoreCase("eRN");
			assertThat(persons).doesNotContain(person1);
		}

		@Test
		void findByStartingWith(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameStartingWith("Er");
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByFirstNameStartingWithIgnoreCase("eRN");
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByContaining(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameContaining("ni");
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByFirstNameContainingIgnoreCase("NI");
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByNotContaining(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameNotContaining("ni");
			assertThat(persons).hasSize(1).contains(person2);

			persons = repository.findAllByFirstNameNotContainingIgnoreCase("NI");
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByEndingWith(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByFirstNameEndingWith("nie");
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByFirstNameEndingWithIgnoreCase("NIE");
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByLessThan(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsLessThan(2L);
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByLessThanEqual(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsLessThanEqual(2L);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);
		}

		@Test
		void findByGreaterThanEqual(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsGreaterThanEqual(1L);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);
		}

		@Test
		void findByGreaterThan(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByPersonNumberIsGreaterThan(1L);
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByBetweenRange(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;
			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.inclusive(1L)).to(Bound.inclusive(2L)));
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.inclusive(1L)).to(Bound.exclusive(2L)));
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.inclusive(1L)).to(Bound.unbounded()));
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.exclusive(1L)).to(Bound.unbounded()));
			assertThat(persons).hasSize(1).contains(person2);

			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.<Long>unbounded()).to(Bound.inclusive(2L)));
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllByPersonNumberIsBetween(Range.from(Bound.<Long>unbounded()).to(Bound.exclusive(2L)));
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByPersonNumberIsBetween(Range.unbounded());
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);
		}

		@Test
		void findByBetween(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;
			persons = repository.findAllByPersonNumberIsBetween(1L, 2L);
			assertThat(persons).containsExactlyInAnyOrder(person1, person2);

			persons = repository.findAllByPersonNumberIsBetween(3L, 5L);
			assertThat(persons).isEmpty();

			persons = repository.findAllByPersonNumberIsBetween(2L, 3L);
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByAfter(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByBornOnAfter(TEST_PERSON1_BORN_ON);
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByBefore(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByBornOnBefore(TEST_PERSON2_BORN_ON);
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByInstant(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository
					.findAllByCreatedAtBefore(LocalDate.of(2019, 9, 25).atStartOfDay().toInstant(ZoneOffset.UTC));
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByIsNotNull(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByNullableIsNotNull();
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByIsNull(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByNullableIsNull();
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByIn(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository
					.findAllByFirstNameIn(Arrays.asList("a", "b", TEST_PERSON2_FIRST_NAME, "c"));
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByNotIn(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository
					.findAllByFirstNameNotIn(Arrays.asList("a", "b", TEST_PERSON2_FIRST_NAME, "c"));
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByEmpty(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByThingsIsEmpty();
			assertThat(persons).hasSize(1).contains(person2);
		}

		@Test
		void findByNotEmpty(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByThingsIsNotEmpty();
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void findByExists(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons = repository.findAllByNullableExists();
			assertThat(persons).hasSize(1).contains(person1);
		}

		@Test
		void shouldSupportSort(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByOrderByFirstNameAscBornOnDesc();
			assertThat(persons).containsExactly(person2, person1);
		}

		@Test
		void findByNear(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;

			persons = repository.findAllByPlaceNear(SFO);
			assertThat(persons).containsExactly(person2, person1);

			persons = repository.findAllByPlaceNearAndFirstNameIn(SFO, Collections.singletonList(TEST_PERSON1_FIRST_NAME));
			assertThat(persons).containsExactly(person1);

			Distance distance = new Distance(200.0 / 1000.0, Metrics.KILOMETERS);
			persons = repository.findAllByPlaceNear(MINC, distance);
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByPlaceNear(CLARION, distance);
			assertThat(persons).isEmpty();

			persons = repository.findAllByPlaceNear(MINC,
					Distance.between(60.0 / 1000.0, Metrics.KILOMETERS, 200.0 / 1000.0, Metrics.KILOMETERS));
			assertThat(persons).hasSize(1).contains(person1);

			persons = repository.findAllByPlaceNear(MINC,
					Distance.between(100.0 / 1000.0, Metrics.KILOMETERS, 200.0 / 1000.0, Metrics.KILOMETERS));
			assertThat(persons).isEmpty();

			final Range<Distance> distanceRange = Range.of(Bound.inclusive(new Distance(100.0 / 1000.0, Metrics.KILOMETERS)),
					Bound.unbounded());
			persons = repository.findAllByPlaceNear(MINC, distanceRange);
			assertThat(persons).hasSize(1).contains(person2);

			persons = repository.findAllByPlaceNear(distanceRange, MINC);
			assertThat(persons).hasSize(1).contains(person2);

			persons = repository
					.findAllByPlaceWithin(new Circle(new org.springframework.data.geo.Point(MINC.x(), MINC.y()), distance));
			assertThat(persons).hasSize(1).contains(person1);

			Box b = new Box(
					new org.springframework.data.geo.Point(MINC.x() - distance.getValue(), MINC.y() - distance.getValue()),
					new org.springframework.data.geo.Point(MINC.x() + distance.getValue(), MINC.y() + distance.getValue()));
			persons = repository.findAllByPlaceWithin(b);
			assertThat(persons).hasSize(1).contains(person1);

			b = new Box(new org.springframework.data.geo.Point(NEO4J_HQ.x(), NEO4J_HQ.y()),
					new org.springframework.data.geo.Point(SFO.x(), SFO.y()));
			persons = repository.findAllByPlaceWithin(b);
			assertThat(persons).hasSize(2);

			Polygon p = new Polygon(new org.springframework.data.geo.Point(12.993747, 55.6122746),
					new org.springframework.data.geo.Point(12.9927492, 55.6110566),
					new org.springframework.data.geo.Point(12.9953456, 55.6106688),
					new org.springframework.data.geo.Point(12.9946482, 55.6110505),
					new org.springframework.data.geo.Point(12.9959786, 55.6112748),
					new org.springframework.data.geo.Point(12.9951847, 55.6122261),
					new org.springframework.data.geo.Point(12.9942727, 55.6122382),
					new org.springframework.data.geo.Point(12.9937685, 55.6122685),
					new org.springframework.data.geo.Point(12.993747, 55.6122746));

			persons = repository.findAllByPlaceWithin(BoundingBox.of(p));
			assertThat(persons).hasSize(1).contains(person1);

			assertThatIllegalArgumentException().isThrownBy(() -> repository.findAllByPlaceWithin(p)).withMessage(
					"The WITHIN operation does not support a class org.springframework.data.geo.Polygon. You might want to pass a bounding box instead: class org.springframework.data.neo4j.repository.query.BoundingBox.of(polygon).");

			persons = repository.findAllByPlaceNear(CLARION, distance);
			assertThat(persons).isEmpty();
		}

		@Test
		void existsById(@Autowired PersonRepository repository) {

			boolean exists = repository.existsById(id1);
			assertThat(exists).isTrue();
		}

		@Test
		void findBySomeCaseInsensitiveProperties(@Autowired PersonRepository repository) {

			List<PersonWithAllConstructor> persons;
			persons = repository.findAllByPlaceNearAndFirstNameAllIgnoreCase(SFO, TEST_PERSON1_FIRST_NAME.toUpperCase());
			assertThat(persons).containsExactly(person1);
		}

		@Test
		void limitClauseShouldWork(@Autowired ThingRepository repository) {

			List<ThingWithAssignedId> things;

			things = repository.findTop5ByOrderByNameDesc();
			assertThat(things).hasSize(5).extracting(ThingWithAssignedId::getName).containsExactlyInAnyOrder("name20",
					"name19", "name18", "name17", "name16");

			things = repository.findFirstByOrderByNameDesc();
			assertThat(things).extracting(ThingWithAssignedId::getName).containsExactlyInAnyOrder("name20");
		}

		@Test
		void count(@Autowired PersonRepository repository) {
			assertThat(repository.count()).isEqualTo(2);
		}

		@Test // GH-112
		void countBySimplePropertiesOred(@Autowired PersonRepository repository) {

			long count = repository.countAllByNameOrName(TEST_PERSON1_NAME, TEST_PERSON2_NAME);
			assertThat(count).isEqualTo(2L);
		}
	}

	@Nested
	class Projection extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			id1 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) "
							+ "SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName " + "RETURN id(n)",
					Values.parameters("name", TEST_PERSON1_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON1_FIRST_NAME))
					.next().get(0).asLong();
			id2 = transaction.run(
					"CREATE (n:PersonWithAllConstructor) "
							+ "SET n.name = $name, n.sameValue = $sameValue, n.first_name = $firstName " + "RETURN id(n)",
					Values.parameters("name", TEST_PERSON2_NAME, "sameValue", TEST_PERSON_SAMEVALUE, "firstName",
							TEST_PERSON2_FIRST_NAME))
					.next().get(0).asLong();
		}

		@Test
		void mapsInterfaceProjectionWithDerivedFinderMethod(@Autowired PersonRepository repository) {

			assertThat(repository.findByName(TEST_PERSON1_NAME)).satisfies(projection -> {
				assertThat(projection.getName()).isEqualTo(TEST_PERSON1_NAME);
				assertThat(projection.getFirstName()).isEqualTo(TEST_PERSON1_FIRST_NAME);
			});
		}

		@Test
		void mapsDtoProjectionWithDerivedFinderMethod(@Autowired PersonRepository repository) {
			assertThat(repository.findByFirstName(TEST_PERSON1_FIRST_NAME))
					.hasSize(1)
					.extracting(DtoPersonProjection::getFirstName)
					.first().isEqualTo(TEST_PERSON1_FIRST_NAME);
		}

		@Test
		void mapsInterfaceProjectionWithDerivedFinderMethodWithMultipleResults(@Autowired PersonRepository repository) {
			assertThat(repository.findBySameValue(TEST_PERSON_SAMEVALUE)).hasSize(2);
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndMapProjection(@Autowired PersonRepository repository) {
			assertThat(repository.findByNameWithCustomQueryAndMapProjection(TEST_PERSON1_NAME).getName())
					.isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndMapProjectionWithMultipleResults(
				@Autowired PersonRepository repository) {
			assertThat(repository.loadAllProjectionsWithMapProjection()).hasSize(2);
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndNodeReturn(@Autowired PersonRepository repository) {
			assertThat(repository.findByNameWithCustomQueryAndNodeReturn(TEST_PERSON1_NAME).getName())
					.isEqualTo(TEST_PERSON1_NAME);
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndNodeReturnWithMultipleResults(
				@Autowired PersonRepository repository) {
			assertThat(repository.loadAllProjectionsWithNodeReturn()).hasSize(2);
		}

		@Test
		void mapsInterfaceProjectionWithCustomQueryAndNodeReturn2(@Autowired PersonRepository repository) {

			List<DtoPersonProjectionContainingAdditionalFields> projectedPeople = repository
					.findAllDtoProjectionsWithAdditionalProperties(TEST_PERSON1_NAME);

			assertThat(projectedPeople).hasSize(1)
					.first()
					.satisfies(dto -> {
						assertThat(dto.getFirstName()).isEqualTo(TEST_PERSON1_FIRST_NAME);
						assertThat(dto.getSomeLongValue()).isEqualTo(4711L);
						assertThat(dto.getSomeDoubles()).containsExactly(21.42, 42.21);
						assertThat(dto.getOtherPeople()).hasSize(1)
								.first()
								.extracting(PersonWithAllConstructor::getFirstName)
								.isEqualTo(TEST_PERSON2_FIRST_NAME);
					});
		}
	}

	@Nested
	class ReturnTypes extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			transaction.run("CREATE (:PersonWithAllConstructor{name: '" + TEST_PERSON1_NAME + "', first_name: '"
					+ TEST_PERSON1_FIRST_NAME + "'})," + " (:PersonWithAllConstructor{name: '" + TEST_PERSON2_NAME + "'})");
		}

		@Test
		void streamMethodsShouldWork(@Autowired PersonRepository repository) {
			assertThat(repository.findAllByNameLike(TEST_PERSON1_NAME)).hasSize(2);
		}

		// commented see PersonRepository line 126
		// @Test
		// void asyncMethodsShouldWork(@Autowired PersonRepository repository) {
		// PersonWithAllConstructor p = repository.findOneByFirstName(TEST_PERSON1_FIRST_NAME).join();
		// assertThat(p).isNotNull();
		// }
	}

	@Nested
	class MultipleLabel extends IntegrationTestBase {

		@Test
		void createNodeWithMultipleLabels(@Autowired MultipleLabelRepository multipleLabelRepository) {
			multipleLabelRepository.save(new MultipleLabels.MultipleLabelsEntity());

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:A) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void createAllNodesWithMultipleLabels(@Autowired MultipleLabelRepository multipleLabelRepository) {
			multipleLabelRepository.saveAll(Collections.singletonList(new MultipleLabels.MultipleLabelsEntity()));

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:A) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void createNodeAndRelationshipWithMultipleLabels(@Autowired MultipleLabelRepository multipleLabelRepository) {
			MultipleLabels.MultipleLabelsEntity entity = new MultipleLabels.MultipleLabelsEntity();
			entity.otherMultipleLabelEntity = new MultipleLabels.MultipleLabelsEntity();

			multipleLabelRepository.save(entity);

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:A)-[:HAS]->(c:A) return n, c").single();
				Node parentNode = record.get("n").asNode();
				Node childNode = record.get("c").asNode();
				assertThat(parentNode.labels()).containsExactlyInAnyOrder("A", "B", "C");
				assertThat(childNode.labels()).containsExactlyInAnyOrder("A", "B", "C");
			}
		}

		@Test
		void findNodeWithMultipleLabels(@Autowired MultipleLabelRepository multipleLabelRepository) {
			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:A:B:C), (n2:B:C), (n3:A) return n1, n2, n3").single();
				n1Id = record.get("n1").asNode().id();
				n2Id = record.get("n2").asNode().id();
				n3Id = record.get("n3").asNode().id();
			}

			Assertions.assertThat(multipleLabelRepository.findById(n1Id)).isPresent();
			Assertions.assertThat(multipleLabelRepository.findById(n2Id)).isNotPresent();
			Assertions.assertThat(multipleLabelRepository.findById(n3Id)).isNotPresent();
		}

		@Test
		void deleteNodeWithMultipleLabels(@Autowired MultipleLabelRepository multipleLabelRepository) {
			long n1Id;
			long n2Id;
			long n3Id;

			try (Session session = createSession()) {
				Record record = session.run("CREATE (n1:A:B:C), (n2:B:C), (n3:A) return n1, n2, n3").single();
				n1Id = record.get("n1").asNode().id();
				n2Id = record.get("n2").asNode().id();
				n3Id = record.get("n3").asNode().id();
			}

			multipleLabelRepository.deleteById(n1Id);
			multipleLabelRepository.deleteById(n2Id);
			multipleLabelRepository.deleteById(n3Id);

			try (Session session = createSession()) {
				assertThat(session.run("MATCH (n:A:B:C) return n").list()).hasSize(0);
				assertThat(session.run("MATCH (n:B:C) return n").list()).hasSize(1);
				assertThat(session.run("MATCH (n:A) return n").list()).hasSize(1);
			}
		}

		@Test
		void createNodeWithMultipleLabelsAndAssignedId(
				@Autowired MultipleLabelWithAssignedIdRepository multipleLabelRepository) {
			multipleLabelRepository.save(new MultipleLabels.MultipleLabelsEntityWithAssignedId(4711L));

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:X) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void createAllNodesWithMultipleLabels(@Autowired MultipleLabelWithAssignedIdRepository multipleLabelRepository) {
			multipleLabelRepository.saveAll(Collections.singletonList(new MultipleLabels.MultipleLabelsEntityWithAssignedId(4711L)));

			try (Session session = createSession()) {
				Node node = session.run("MATCH (n:X) return n").single().get("n").asNode();
				assertThat(node.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void createNodeAndRelationshipWithMultipleLabels(
				@Autowired MultipleLabelWithAssignedIdRepository multipleLabelRepository) {
			MultipleLabels.MultipleLabelsEntityWithAssignedId entity = new MultipleLabels.MultipleLabelsEntityWithAssignedId(
					4711L);
			entity.otherMultipleLabelEntity = new MultipleLabels.MultipleLabelsEntityWithAssignedId(42L);

			multipleLabelRepository.save(entity);

			try (Session session = createSession()) {
				Record record = session.run("MATCH (n:X)-[:HAS]->(c:X) return n, c").single();
				Node parentNode = record.get("n").asNode();
				Node childNode = record.get("c").asNode();
				assertThat(parentNode.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
				assertThat(childNode.labels()).containsExactlyInAnyOrder("X", "Y", "Z");
			}
		}

		@Test
		void findNodeWithMultipleLabels(@Autowired MultipleLabelWithAssignedIdRepository multipleLabelRepository) {
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

			Assertions.assertThat(multipleLabelRepository.findById(n1Id)).isPresent();
			Assertions.assertThat(multipleLabelRepository.findById(n2Id)).isNotPresent();
			Assertions.assertThat(multipleLabelRepository.findById(n3Id)).isNotPresent();
		}

		@Test
		void deleteNodeWithMultipleLabels(@Autowired MultipleLabelWithAssignedIdRepository multipleLabelRepository) {
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

			multipleLabelRepository.deleteById(n1Id);
			multipleLabelRepository.deleteById(n2Id);
			multipleLabelRepository.deleteById(n3Id);

			try (Session session = createSession()) {
				assertThat(session.run("MATCH (n:X:Y:Z) return n").list()).hasSize(0);
				assertThat(session.run("MATCH (n:Y:Z) return n").list()).hasSize(1);
				assertThat(session.run("MATCH (n:X) return n").list()).hasSize(1);
			}
		}
	}

	@Nested
	class TypeInheritanceAndGenerics extends IntegrationTestBase {

		@Test
		void findByIdWithInheritance(@Autowired BaseClassRepository baseClassRepository) {
			String someValue = "test";
			String concreteClassName = "cc1";
			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA(concreteClassName, someValue);
			baseClassRepository.save(ccA);

			Inheritance.BaseClass loadedCcA = baseClassRepository.findById(ccA.getId()).get();
			assertThat(loadedCcA).isInstanceOfSatisfying(Inheritance.ConcreteClassA.class, o -> {
				assertThat(o.getName()).isEqualTo(concreteClassName);
				assertThat(o.getConcreteSomething()).isEqualTo(someValue);
			});
		}

		@Test
		void findAllWithInheritance(@Autowired BaseClassRepository baseClassRepository) {
			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA("cc1", "test");
			Inheritance.ConcreteClassB ccB = new Inheritance.ConcreteClassB("cc2", 42);
			baseClassRepository.save(ccA);
			baseClassRepository.save(ccB);

			List<Inheritance.BaseClass> all = baseClassRepository.findAll();

			assertThat(all).containsExactlyInAnyOrder(ccA, ccB);
		}

		@Test
		void findAllWithInheritanceAndExplicitLabeling(@Autowired BaseClassWithLabelsRepository repository) {
			String classAName = "test1";
			String classBName = "test2";
			Inheritance.ExtendingClassWithLabelsA classWithLabelsA = new Inheritance.ExtendingClassWithLabelsA(classAName);
			Inheritance.ExtendingClassWithLabelsB classWithLabelsB = new Inheritance.ExtendingClassWithLabelsB(classBName);

			repository.save(classWithLabelsA);
			repository.save(classWithLabelsB);

			List<Inheritance.BaseClassWithLabels> all = repository.findAll();

			assertThat(all).containsExactlyInAnyOrder(classWithLabelsA, classWithLabelsB);
		}

		@Test
		void findByIdWithTwoLevelInheritance(@Autowired SuperBaseClassRepository superBaseClassRepository) {
			String someValue = "test";
			String concreteClassName = "cc1";
			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA(concreteClassName, someValue);
			superBaseClassRepository.save(ccA);

			Inheritance.SuperBaseClass loadedCcA = superBaseClassRepository.findById(ccA.getId()).get();
			assertThat(loadedCcA).isInstanceOfSatisfying(Inheritance.ConcreteClassA.class, o -> {
				assertThat(o.getName()).isEqualTo(concreteClassName);
				assertThat(o.getConcreteSomething()).isEqualTo(someValue);
			});
		}

		@Test
		void findAllWithTwoLevelInheritance(@Autowired SuperBaseClassRepository superBaseClassRepository) {
			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA("cc1", "test");
			Inheritance.ConcreteClassB ccB = new Inheritance.ConcreteClassB("cc2", 42);
			superBaseClassRepository.save(ccA);
			superBaseClassRepository.save(ccB);

			List<Inheritance.SuperBaseClass> all = superBaseClassRepository.findAll();

			assertThat(all).containsExactlyInAnyOrder(ccA, ccB);
		}

		@Test
		void findAllWithTwoLevelInheritanceByCustomQuery(@Autowired SuperBaseClassRepository superBaseClassRepository) {
			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA("cc1", "test");
			Inheritance.ConcreteClassB ccB = new Inheritance.ConcreteClassB("cc2", 42);
			superBaseClassRepository.save(ccA);
			superBaseClassRepository.save(ccB);

			List<Inheritance.SuperBaseClass> all = superBaseClassRepository.getAllConcreteTypes();

			assertThat(all).containsExactlyInAnyOrder(ccA, ccB);
		}

		@Test
		void findAndInstantiateGenericRelationships(@Autowired RelationshipToAbstractClassRepository repository) {

			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA("cc1", "test");
			Inheritance.ConcreteClassB ccB = new Inheritance.ConcreteClassB("cc2", 42);

			List<Inheritance.SuperBaseClass> things = new ArrayList<>();
			things.add(ccA);
			things.add(ccB);
			Inheritance.RelationshipToAbstractClass thing = new Inheritance.RelationshipToAbstractClass();
			thing.setThings(things);

			repository.save(thing);

			List<Inheritance.RelationshipToAbstractClass> all = repository.findAll();

			assertThat(all.get(0).getThings()).containsExactlyInAnyOrder(ccA, ccB);
		}

		@Test
		void findAndInstantiateGenericRelationshipsWithCustomQuery(
				@Autowired RelationshipToAbstractClassRepository repository) {

			Inheritance.ConcreteClassA ccA = new Inheritance.ConcreteClassA("cc1", "test");
			Inheritance.ConcreteClassB ccB = new Inheritance.ConcreteClassB("cc2", 42);

			List<Inheritance.SuperBaseClass> things = new ArrayList<>();
			things.add(ccA);
			things.add(ccB);
			Inheritance.RelationshipToAbstractClass thing = new Inheritance.RelationshipToAbstractClass();
			thing.setThings(things);

			repository.save(thing);

			Inheritance.RelationshipToAbstractClass result = repository.getAllConcreteRelationships();

			assertThat(result.getThings()).containsExactlyInAnyOrder(ccA, ccB);
		}
	}

	@Nested
	class RelatedEntityQuery extends IntegrationTestBase {

		@Test
		void findByPropertyOnRelatedEntity(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})");
			}

			assertThat(repository.findByPetsName("Jerry").getName()).isEqualTo("Freddie");
		}

		@Test
		void findByPropertyOnRelatedEntitiesOr(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Tom'}),"
						+ "(n)-[:Has]->(:Hobby{name: 'Music'})");
			}

			assertThat(repository.findByHobbiesNameOrPetsName("Music", "Jerry").getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesNameOrPetsName("Sports", "Tom").getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesNameOrPetsName("Sports", "Jerry")).isNull();
		}

		@Test
		void findByPropertyOnRelatedEntitiesAnd(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (n:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Tom'}),"
						+ "(n)-[:Has]->(:Hobby{name: 'Music'})");
			}

			assertThat(repository.findByHobbiesNameAndPetsName("Music", "Tom").getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesNameAndPetsName("Sports", "Jerry")).isNull();
		}

		@Test
		void findByPropertyOnRelatedEntityOfRelatedEntity(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})"
						+ "-[:Has]->(:Hobby{name: 'Sleeping'})");
			}

			assertThat(repository.findByPetsHobbiesName("Sleeping").getName()).isEqualTo("Freddie");
			assertThat(repository.findByPetsHobbiesName("Sports")).isNull();
		}

		@Test
		void findByPropertyOnRelatedEntityOfRelatedSameEntity(@Autowired RelationshipRepository repository) {
			try (Session session = createSession()) {
				session.run("CREATE (:PersonWithRelationship{name:'Freddie'})-[:Has]->(:Pet{name: 'Jerry'})"
						+ "-[:Has]->(:Pet{name: 'Tom'})");
			}

			assertThat(repository.findByPetsFriendsName("Tom").getName()).isEqualTo("Freddie");
			assertThat(repository.findByPetsFriendsName("Jerry")).isNull();
		}

		@Test
		void findByPropertyOnRelationshipWithProperties(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020}]->(:Hobby{name: 'Bowling'})");
			}

			assertThat(repository.findByHobbiesSince(2020).getName()).isEqualTo("Freddie");
		}

		@Test
		void findByPropertyOnRelationshipWithPropertiesOr(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020, active: true}]->(:Hobby{name: 'Bowling'})");
			}

			assertThat(repository.findByHobbiesSinceOrHobbiesActive(2020, false).getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesSinceOrHobbiesActive(2019, true).getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesSinceOrHobbiesActive(2019, false)).isNull();
		}

		@Test
		void findByPropertyOnRelationshipWithPropertiesAnd(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020, active: true}]->(:Hobby{name: 'Bowling'})");
			}

			assertThat(repository.findByHobbiesSinceAndHobbiesActive(2020, true).getName()).isEqualTo("Freddie");
			assertThat(repository.findByHobbiesSinceAndHobbiesActive(2019, true)).isNull();
			assertThat(repository.findByHobbiesSinceAndHobbiesActive(2020, false)).isNull();
		}

		@Test
		void findByPropertyOnRelationshipWithPropertiesRelatedEntity(
				@Autowired PersonWithRelationshipWithPropertiesRepository repository) {
			try (Session session = createSession()) {
				session.run(
						"CREATE (:PersonWithRelationshipWithProperties{name:'Freddie'})-[:LIKES{since: 2020, active: true}]->(:Hobby{name: 'Bowling'})");
			}

			assertThat(repository.findByHobbiesHobbyName("Bowling").getName()).isEqualTo("Freddie");
		}
	}

	@Nested
	class Converter extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			transaction.run("CREATE (:CustomTypes{customType:'XYZ'})");
		}

		@Test
		void findByConvertedCustomType(@Autowired EntityWithCustomTypePropertyRepository repository) {

			assertThat(repository.findByCustomType(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
		}

		@Test
		void findByConvertedCustomTypeWithCustomQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

			assertThat(repository.findByCustomTypeCustomQuery(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
		}

		@Test
		void findByConvertedCustomTypeWithSpELPropertyAccessQuery(
				@Autowired EntityWithCustomTypePropertyRepository repository) {

			assertThat(repository.findByCustomTypeCustomSpELPropertyAccessQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
					.isNotNull();
		}

		@Test
		void findByConvertedCustomTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

			assertThat(repository.findByCustomTypeSpELObjectQuery(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
		}

		@Test
		void findByConvertedDifferentTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

			assertThat(repository.findByDifferentTypeCustomQuery(ThingWithCustomTypes.DifferentType.of("XYZ"))).isNotNull();
		}
	}

	/**
	 * The tests in this class ensure that in case of an inheritance scenario no DTO is projected but the extending class
	 * is used. If it wasn't the case, we wouldn't find the relationship nor the other attribute.
	 */
	@Nested
	class DtoVsInheritance extends IntegrationTestBase {

		@Override
		void setupData(Transaction transaction) {
			transaction.run(""
					+ "create (p:ParentNode:ExtendedParentNode {someAttribute: 'Foo', someOtherAttribute: 'Bar'})"
					+ "create (p) -[:CONNECTED_TO]-> (:PersonWithAllConstructor {name: 'Bazbar'})");
		}

		@Test
		void shouldFindExtendedNodeViaBaseAttribute(@Autowired ParentRepository repository) {

			assertThat(repository.findExtendedParentNodeBySomeAttribute("Foo")).hasValueSatisfying(ep -> {
				assertThat(ep.getSomeOtherAttribute()).isEqualTo("Bar");
				assertThat(ep.getPeople()).extracting(PersonWithAllConstructor::getName).containsExactly("Bazbar");
			});
		}

		@Test
		void shouldFindExtendedNodeViaExtendedAttribute(@Autowired ParentRepository repository) {

			assertThat(repository.findExtendedParentNodeBySomeOtherAttribute("Bar")).hasValueSatisfying(ep -> {
				assertThat(ep.getSomeAttribute()).isEqualTo("Foo");
				assertThat(ep.getPeople()).extracting(PersonWithAllConstructor::getName).containsExactly("Bazbar");
			});
		}
	}

	interface BidirectionalStartRepository extends Neo4jRepository<BidirectionalStart, Long> {}

	interface BidirectionalEndRepository extends Neo4jRepository<BidirectionalEnd, Long> {}

	interface LoopingRelationshipRepository extends Neo4jRepository<DeepRelationships.LoopingType1, Long> {}

	interface ImmutablePersonRepository extends Neo4jRepository<ImmutablePerson, String> {}

	interface MultipleLabelRepository extends Neo4jRepository<MultipleLabels.MultipleLabelsEntity, Long> {}

	interface MultipleLabelWithAssignedIdRepository
			extends Neo4jRepository<MultipleLabels.MultipleLabelsEntityWithAssignedId, Long> {}

	interface PersonWithRelationshipWithPropertiesRepository
			extends Neo4jRepository<PersonWithRelationshipWithProperties, Long> {

		@Query("MATCH (p:PersonWithRelationshipWithProperties)-[l:LIKES]->(h:Hobby) return p, collect(l), collect(h)")
		PersonWithRelationshipWithProperties loadFromCustomQuery(@Param("id") Long id);

		PersonWithRelationshipWithProperties findByHobbiesSince(int since);

		PersonWithRelationshipWithProperties findByHobbiesSinceOrHobbiesActive(int since1, boolean active);

		PersonWithRelationshipWithProperties findByHobbiesSinceAndHobbiesActive(int since1, boolean active);

		PersonWithRelationshipWithProperties findByHobbiesHobbyName(String hobbyName);
	}

	interface PetRepository extends Neo4jRepository<Pet, Long> {

		@Query("MATCH (p:Pet)-[r1:Has]->(p2:Pet)-[r2:Has]->(p3:Pet) " +
				"where id(p) = $petNode1Id return p, collect(r1), collect(p2), collect(r2), collect(p3)")
		Pet customQueryWithDeepRelationshipMapping(@Param("petNode1Id") long petNode1Id);
	}

	interface RelationshipRepository extends Neo4jRepository<PersonWithRelationship, Long> {

		@Query("MATCH (n:PersonWithRelationship{name:'Freddie'}) "
				+ "OPTIONAL MATCH (n)-[r1:Has]->(p:Pet) WITH n, collect(r1) as petRels, collect(p) as pets "
				+ "OPTIONAL MATCH (n)-[r2:Has]->(h:Hobby) "
				+ "return n, petRels, pets, collect(r2) as hobbyRels, collect(h) as hobbies")
		PersonWithRelationship getPersonWithRelationshipsViaQuery();

		PersonWithRelationship findByPetsName(String petName);

		PersonWithRelationship findByName(String name);

		PersonWithRelationship findByHobbiesNameOrPetsName(String hobbyName, String petName);

		PersonWithRelationship findByHobbiesNameAndPetsName(String hobbyName, String petName);

		PersonWithRelationship findByPetsHobbiesName(String hobbyName);

		PersonWithRelationship findByPetsFriendsName(String petName);
	}

	interface SimilarThingRepository extends Neo4jRepository<SimilarThing, Long> {}

	interface BaseClassRepository extends Neo4jRepository<Inheritance.BaseClass, Long> {}

	interface SuperBaseClassRepository extends Neo4jRepository<Inheritance.SuperBaseClass, Long> {

		@Query("MATCH (n:SuperBaseClass) return n")
		List<Inheritance.SuperBaseClass> getAllConcreteTypes();
	}

	interface RelationshipToAbstractClassRepository
			extends Neo4jRepository<Inheritance.RelationshipToAbstractClass, Long> {

		@Query("MATCH (n:RelationshipToAbstractClass)-[h:HAS]->(m:SuperBaseClass) return n, collect(h), collect(m)")
		Inheritance.RelationshipToAbstractClass getAllConcreteRelationships();
	}

	interface BaseClassWithLabelsRepository extends Neo4jRepository<Inheritance.BaseClassWithLabels, Long> {}

	interface EntityWithConvertedIdRepository
			extends Neo4jRepository<EntityWithConvertedId, EntityWithConvertedId.IdentifyingEnum> {}

	interface EntityWithCustomTypePropertyRepository extends Neo4jRepository<ThingWithCustomTypes, Long> {

		ThingWithCustomTypes findByCustomType(ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $customType return c")
		ThingWithCustomTypes findByCustomTypeCustomQuery(@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $differentType return c")
		ThingWithCustomTypes findByDifferentTypeCustomQuery(
				@Param("differentType") ThingWithCustomTypes.DifferentType differentType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType.value} return c")
		ThingWithCustomTypes findByCustomTypeCustomSpELPropertyAccessQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType} return c")
		ThingWithCustomTypes findByCustomTypeSpELObjectQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);
	}

	interface HobbyWithRelationshipWithPropertiesRepository extends Neo4jRepository<AltHobby, Long> {

		@Query("MATCH (p:AltPerson)-[l:LIKES]->(h:AltHobby) WHERE id(p) = $personId RETURN h, collect(l), collect(p)")
		AltHobby loadFromCustomQuery(@Param("personId") Long personId);
	}

	interface FriendRepository extends Neo4jRepository<Friend, Long> {}

	interface KotlinPersonRepository extends Neo4jRepository<KotlinPerson, Long> {

		@Query("MATCH (n:KotlinPerson)-[w:WORKS_IN]->(c:KotlinClub) return n, collect(w), collect(c)")
		List<KotlinPerson> getAllKotlinPersonsViaQuery();

		@Query("MATCH (n:KotlinPerson{name:'Test'})-[w:WORKS_IN]->(c:KotlinClub) return n, collect(w), collect(c)")
		KotlinPerson getOneKotlinPersonViaQuery();

		@Query("MATCH (n:KotlinPerson{name:'Test'})-[w:WORKS_IN]->(c:KotlinClub) return n, collect(w), collect(c)")
		Optional<KotlinPerson> getOptionalKotlinPersonViaQuery();
	}

	interface ParentRepository extends Neo4jRepository<ParentNode, Long> {

		/**
		 * Ensure things can be found by base attribute.
		 * @param someAttribute Base attribute
		 * @return optional entity
		 */
		Optional<ExtendedParentNode> findExtendedParentNodeBySomeAttribute(String someAttribute);

		/**
		 * Ensure things can be found by extended attribute.
		 * @param someOtherAttribute Base attribute
		 * @return optional entity
		 */
		Optional<ExtendedParentNode> findExtendedParentNodeBySomeOtherAttribute(String someOtherAttribute);
	}

	@SpringJUnitConfig(Config.class)
	static abstract class IntegrationTestBase {

		@Autowired private Driver driver;

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
		public Neo4jConversions neo4jConversions() {
			Set<GenericConverter> additionalConverters = new HashSet<>();
			additionalConverters.add(new ThingWithCustomTypes.CustomTypeConverter());
			additionalConverters.add(new ThingWithCustomTypes.DifferentTypeConverter());

			return new Neo4jConversions(additionalConverters);
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		public DatabaseSelectionProvider databaseNameProvider() {
			return () -> databaseSelection;
		}
	}
}
