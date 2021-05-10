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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Club;
import org.springframework.data.neo4j.integration.shared.common.ClubRelationship;
import org.springframework.data.neo4j.integration.shared.common.DynamicRelationshipsITBase;
import org.springframework.data.neo4j.integration.shared.common.Hobby;
import org.springframework.data.neo4j.integration.shared.common.HobbyRelationship;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelatives;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelatives.TypeOfClub;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelatives.TypeOfHobby;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelatives.TypeOfPet;
import org.springframework.data.neo4j.integration.shared.common.PersonWithRelatives.TypeOfRelative;
import org.springframework.data.neo4j.integration.shared.common.Pet;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class DynamicRelationshipsIT extends DynamicRelationshipsITBase<PersonWithRelatives> {

	@Autowired
	DynamicRelationshipsIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void shouldReadDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.findById(idOfExistingPerson).get();
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfRelative, Person> relatives = person.getRelatives();
		assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_WIFE, TypeOfRelative.HAS_DAUGHTER);
		assertThat(relatives.get(TypeOfRelative.HAS_WIFE).getFirstName()).isEqualTo("B");
		assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C");

		Map<TypeOfClub, ClubRelationship> clubs = person.getClubs();
		assertThat(clubs).containsOnlyKeys(TypeOfClub.FOOTBALL);
		assertThat(clubs.get(TypeOfClub.FOOTBALL).getPlace()).isEqualTo("Brunswick");
		assertThat(clubs.get(TypeOfClub.FOOTBALL).getClub().getName()).isEqualTo("BTSV");
	}

	@Test // GH-216
	void shouldReadDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.findById(idOfExistingPerson).get();
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfPet, List<Pet>> pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);
		assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield");
		assertThat(pets.get(TypeOfPet.DOGS)).extracting(Pet::getName).containsExactlyInAnyOrder("Benji", "Lassie");

		Map<TypeOfHobby, List<HobbyRelationship>> hobbies = person.getHobbies();
		assertThat(hobbies.get(TypeOfHobby.ACTIVE)).extracting(HobbyRelationship::getPerformance).containsExactly("average");
		assertThat(hobbies.get(TypeOfHobby.ACTIVE)).extracting(HobbyRelationship::getHobby).extracting(Hobby::getName).containsExactly("Biking");
	}

	@Test // DATAGRAPH-1449
	void shouldUpdateDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.findById(idOfExistingPerson).get();
		assumeThat(person).isNotNull();
		assumeThat(person.getName()).isEqualTo("A");

		Map<TypeOfRelative, Person> relatives = person.getRelatives();
		assumeThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_WIFE, TypeOfRelative.HAS_DAUGHTER);

		relatives.remove(TypeOfRelative.HAS_WIFE);
		Person d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "D");
		relatives.put(TypeOfRelative.HAS_SON, d);
		ReflectionTestUtils.setField(relatives.get(TypeOfRelative.HAS_DAUGHTER), "firstName", "C2");

		Map<TypeOfClub, ClubRelationship> clubs = person.getClubs();
		clubs.remove(TypeOfClub.FOOTBALL);
		ClubRelationship clubRelationship = new ClubRelationship("Boston");
		Club club = new Club();
		club.setName("Red Sox");
		clubRelationship.setClub(club);
		clubs.put(TypeOfClub.BASEBALL, clubRelationship);

		repository.save(person);

		person = repository.findById(idOfExistingPerson).get();

		relatives = person.getRelatives();
		assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_DAUGHTER, TypeOfRelative.HAS_SON);
		assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C2");
		assertThat(relatives.get(TypeOfRelative.HAS_SON).getFirstName()).isEqualTo("D");

		clubs = person.getClubs();
		assertThat(clubs).containsOnlyKeys(TypeOfClub.BASEBALL);
		assertThat(clubs.get(TypeOfClub.BASEBALL)).extracting(ClubRelationship::getPlace).isEqualTo("Boston");
		assertThat(clubs.get(TypeOfClub.BASEBALL)).extracting(ClubRelationship::getClub)
				.extracting(Club::getName).isEqualTo("Red Sox");
	}

	@Test // GH-216 // DATAGRAPH-1449
	void shouldUpdateDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.findById(idOfExistingPerson).get();
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfPet, List<Pet>> pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);

		pets.remove(TypeOfPet.DOGS);
		pets.get(TypeOfPet.CATS).add(new Pet("Delilah"));

		pets.put(TypeOfPet.FISH, Collections.singletonList(new Pet("Nemo")));

		Map<TypeOfHobby, List<HobbyRelationship>> hobbies = person.getHobbies();
		hobbies.remove(TypeOfHobby.ACTIVE);

		HobbyRelationship hobbyRelationship = new HobbyRelationship("average");
		Hobby hobby = new Hobby();
		hobby.setName("Football");
		hobbyRelationship.setHobby(hobby);
		hobbies.put(TypeOfHobby.WATCHING, Collections.singletonList(hobbyRelationship));

		repository.save(person);

		person = repository.findById(idOfExistingPerson).get();

		pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.FISH);
		assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield",
				"Delilah");
		assertThat(pets.get(TypeOfPet.FISH)).extracting(Pet::getName).containsExactlyInAnyOrder("Nemo");

		hobbies = person.getHobbies();
		assertThat(hobbies).containsOnlyKeys(TypeOfHobby.WATCHING);
		assertThat(hobbies.get(TypeOfHobby.WATCHING)).extracting(HobbyRelationship::getPerformance)
				.containsExactly("average");
		assertThat(hobbies.get(TypeOfHobby.WATCHING)).extracting(HobbyRelationship::getHobby)
				.extracting(Hobby::getName).containsExactly("Football");
	}

	@Test // DATAGRAPH-1447
	void shouldWriteDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Map<TypeOfRelative, Person> relatives = newPerson.getRelatives();

		Person d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R1");
		relatives.put(TypeOfRelative.RELATIVE_1, d);

		d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R2");
		relatives.put(TypeOfRelative.RELATIVE_2, d);

		Map<TypeOfClub, ClubRelationship> clubs = newPerson.getClubs();
		ClubRelationship clubRelationship = new ClubRelationship("Brunswick");
		Club club1 = new Club();
		club1.setName("BTSV");
		clubRelationship.setClub(club1);
		clubs.put(TypeOfClub.FOOTBALL, clubRelationship);

		clubRelationship = new ClubRelationship("Boston");
		Club club2 = new Club();
		club2.setName("Red Sox");
		clubRelationship.setClub(club2);
		clubs.put(TypeOfClub.BASEBALL, clubRelationship);

		newPerson = repository.findById(repository.save(newPerson).getId()).get();

		assertThat(newPerson.getRelatives()).containsOnlyKeys(TypeOfRelative.RELATIVE_1, TypeOfRelative.RELATIVE_2);
		assertThat(newPerson.getClubs()).containsOnlyKeys(TypeOfClub.BASEBALL, TypeOfClub.FOOTBALL);

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction
					.run("" + "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id " + "RETURN size((t)-->(:Person))"
							+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
					.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(2L);
			numberOfRelations = transaction
					.run("" + "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id " + "RETURN size((t)-->(:Club))"
							+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
					.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(2L);
		}
	}

	@Test // GH-216 // DATAGRAPH-1447
	void shouldWriteDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Map<TypeOfPet, List<Pet>> pets = newPerson.getPets();
		Map<TypeOfHobby, List<HobbyRelationship>> hobbies = newPerson.getHobbies();

		List<Pet> monsters = pets.computeIfAbsent(TypeOfPet.MONSTERS, s -> new ArrayList<>());
		monsters.add(new Pet("Godzilla"));
		monsters.add(new Pet("King Kong"));

		List<Pet> fish = pets.computeIfAbsent(TypeOfPet.FISH, s -> new ArrayList<>());
		fish.add(new Pet("Nemo"));

		List<HobbyRelationship> hobbyRelationships = hobbies
				.computeIfAbsent(TypeOfHobby.ACTIVE, s -> new ArrayList<>());
		HobbyRelationship hobbyRelationship = new HobbyRelationship("ok");
		Hobby hobby1 = new Hobby();
		hobby1.setName("Football");
		hobbyRelationship.setHobby(hobby1);
		hobbyRelationships.add(hobbyRelationship);

		HobbyRelationship hobbyRelationship2 = new HobbyRelationship("perfect");
		Hobby hobby2 = new Hobby();
		hobby2.setName("Music");
		hobbyRelationship2.setHobby(hobby2);

		hobbyRelationships.add(hobbyRelationship2);

		newPerson = repository.findById(repository.save(newPerson).getId()).get();

		pets = newPerson.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.MONSTERS, TypeOfPet.FISH);

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction
					.run("" + "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id " + "RETURN size((t)-->(:Pet))"
							+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
					.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(3L);
			numberOfRelations = transaction
					.run("" + "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id " + "RETURN size((t)-->(:Hobby))"
							+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
					.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(2L);
		}
	}

	@Test // DATAGRAPH-1411
	void shouldReadDynamicRelationshipsWithCustomQuery(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.byCustomQuery(idOfExistingPerson);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfRelative, Person> relatives = person.getRelatives();
		assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_WIFE, TypeOfRelative.HAS_DAUGHTER);
		assertThat(relatives.get(TypeOfRelative.HAS_WIFE).getFirstName()).isEqualTo("B");
		assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C");

		Map<TypeOfClub, ClubRelationship> clubs = person.getClubs();
		assertThat(clubs).containsOnlyKeys(TypeOfClub.FOOTBALL);
		assertThat(clubs.get(TypeOfClub.FOOTBALL).getPlace()).isEqualTo("Brunswick");
		assertThat(clubs.get(TypeOfClub.FOOTBALL).getClub().getName()).isEqualTo("BTSV");
	}

	@Test // DATAGRAPH-1411
	void shouldReadDynamicCollectionRelationshipsWithCustomQuery(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.byCustomQuery(idOfExistingPerson);
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfPet, List<Pet>> pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);
		assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield");
		assertThat(pets.get(TypeOfPet.DOGS)).extracting(Pet::getName).containsExactlyInAnyOrder("Benji", "Lassie");

		Map<TypeOfHobby, List<HobbyRelationship>> hobbies = person.getHobbies();
		assertThat(hobbies.get(TypeOfHobby.ACTIVE)).extracting(HobbyRelationship::getPerformance).containsExactly("average");
		assertThat(hobbies.get(TypeOfHobby.ACTIVE)).extracting(HobbyRelationship::getHobby).extracting(Hobby::getName).containsExactly("Biking");
	}

	interface PersonWithRelativesRepository extends CrudRepository<PersonWithRelatives, Long> {

		@Query("MATCH (p:PersonWithRelatives)-[r] -> (o) WHERE id(p) = $personId return p, collect(r), collect(o)")
		PersonWithRelatives byCustomQuery(@Param("personId") Long personId);

	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
