/*
 * Copyright (c) 2019-2020 "Neo4j,"
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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.DynamicRelationshipsITBase;
import org.neo4j.springframework.data.integration.shared.Person;
import org.neo4j.springframework.data.integration.shared.PersonWithRelatives;
import org.neo4j.springframework.data.integration.shared.PersonWithRelatives.TypeOfPet;
import org.neo4j.springframework.data.integration.shared.PersonWithRelatives.TypeOfRelative;
import org.neo4j.springframework.data.integration.shared.Pet;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class DynamicRelationshipsIT extends DynamicRelationshipsITBase<PersonWithRelatives> {

	@Autowired DynamicRelationshipsIT(Driver driver) {
		super(driver);
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
	}

	@Test
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

		person = repository.save(person);
		relatives = person.getRelatives();
		assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_DAUGHTER, TypeOfRelative.HAS_SON);
		assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C2");
		assertThat(relatives.get(TypeOfRelative.HAS_SON).getFirstName()).isEqualTo("D");
	}

	@Test // GH-216
	void shouldUpdateDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives person = repository.findById(idOfExistingPerson).get();
		assertThat(person).isNotNull();
		assertThat(person.getName()).isEqualTo("A");

		Map<TypeOfPet, List<Pet>> pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);

		pets.remove(TypeOfPet.DOGS);
		pets.get(TypeOfPet.CATS).add(new Pet("Delilah"));

		pets.put(TypeOfPet.FISH, Collections.singletonList(new Pet("Nemo")));

		person = repository.save(person);
		pets = person.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.FISH);
		assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName)
			.containsExactlyInAnyOrder("Tom", "Garfield", "Delilah");
		assertThat(pets.get(TypeOfPet.FISH)).extracting(Pet::getName).containsExactlyInAnyOrder("Nemo");
	}

	@Test
	void shouldWriteDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Map<TypeOfRelative, Person> relatives = newPerson.getRelatives();

		Person d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R1");
		relatives.put(TypeOfRelative.RELATIVE_1, d);

		d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R2");
		relatives.put(TypeOfRelative.RELATIVE_2, d);

		newPerson = repository.save(newPerson);
		relatives = newPerson.getRelatives();
		assertThat(relatives).containsOnlyKeys(TypeOfRelative.RELATIVE_1, TypeOfRelative.RELATIVE_2);

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
				+ "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id "
				+ "RETURN size((t)-->(:Person))"
				+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
				.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(2L);
		}
	}

	@Test // GH-216
	void shouldWriteDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Map<TypeOfPet, List<Pet>> pets = newPerson.getPets();

		List<Pet> monsters = pets.computeIfAbsent(TypeOfPet.MONSTERS, s -> new ArrayList<>());
		monsters.add(new Pet("Godzilla"));
		monsters.add(new Pet("King Kong"));

		List<Pet> fish = pets.computeIfAbsent(TypeOfPet.FISH, s -> new ArrayList<>());
		fish.add(new Pet("Nemo"));

		newPerson = repository.save(newPerson);
		pets = newPerson.getPets();
		assertThat(pets).containsOnlyKeys(TypeOfPet.MONSTERS, TypeOfPet.FISH);

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
				+ "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id "
				+ "RETURN size((t)-->(:Pet))"
				+ " as numberOfRelations", Values.parameters("id", newPerson.getId()))
				.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(3L);
		}
	}

	interface PersonWithRelativesRepository extends CrudRepository<PersonWithRelatives, Long> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}
	}
}
