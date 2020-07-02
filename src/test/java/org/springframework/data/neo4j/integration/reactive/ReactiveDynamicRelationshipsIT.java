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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;
import static org.springframework.data.neo4j.test.Neo4jExtension.*;

import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.DynamicRelationshipsITBase;
import org.springframework.data.neo4j.integration.shared.Person;
import org.springframework.data.neo4j.integration.shared.PersonWithRelatives;
import org.springframework.data.neo4j.integration.shared.PersonWithRelatives.TypeOfPet;
import org.springframework.data.neo4j.integration.shared.PersonWithRelatives.TypeOfRelative;
import org.springframework.data.neo4j.integration.shared.Pet;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveDynamicRelationshipsIT extends DynamicRelationshipsITBase<PersonWithRelatives> {

	@Autowired ReactiveDynamicRelationshipsIT(Driver driver) {
		super(driver);
	}

	@Test
	void shouldReadDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				assertThat(person).isNotNull();
				assertThat(person.getName()).isEqualTo("A");

				Map<TypeOfRelative, Person> relatives = person.getRelatives();
				assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_WIFE, TypeOfRelative.HAS_DAUGHTER);
				assertThat(relatives.get(TypeOfRelative.HAS_WIFE).getFirstName()).isEqualTo("B");
				assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C");
			})
			.verifyComplete();
	}

	@Test // GH-216
	void shouldReadDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				assertThat(person).isNotNull();
				assertThat(person.getName()).isEqualTo("A");

				Map<TypeOfPet, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);
				assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield");
				assertThat(pets.get(TypeOfPet.DOGS)).extracting(Pet::getName).containsExactlyInAnyOrder("Benji", "Lassie");
			})
			.verifyComplete();
	}

	@Test
	void shouldUpdateDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.map(person -> {
				assumeThat(person).isNotNull();
				assumeThat(person.getName()).isEqualTo("A");

				Map<TypeOfRelative, Person> relatives = person.getRelatives();
				assumeThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_WIFE, TypeOfRelative.HAS_DAUGHTER);

				relatives.remove(TypeOfRelative.HAS_WIFE);
				Person d = new Person();
				ReflectionTestUtils.setField(d, "firstName", "D");
				relatives.put(TypeOfRelative.HAS_SON, d);
				ReflectionTestUtils.setField(relatives.get(TypeOfRelative.HAS_DAUGHTER), "firstName", "C2");
				return person;
			})
			.flatMap(repository::save)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				Map<TypeOfRelative, Person> relatives = person.getRelatives();
				assertThat(relatives).containsOnlyKeys(TypeOfRelative.HAS_DAUGHTER, TypeOfRelative.HAS_SON);
				assertThat(relatives.get(TypeOfRelative.HAS_DAUGHTER).getFirstName()).isEqualTo("C2");
				assertThat(relatives.get(TypeOfRelative.HAS_SON).getFirstName()).isEqualTo("D");
			})
			.verifyComplete();
	}

	@Test // GH-216
	void shouldUpdateDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.map(person -> {
				assumeThat(person).isNotNull();
				assumeThat(person.getName()).isEqualTo("A");

				Map<TypeOfPet, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.DOGS);

				pets.remove(TypeOfPet.DOGS);
				pets.get(TypeOfPet.CATS).add(new Pet("Delilah"));

				pets.put(TypeOfPet.FISH, Collections.singletonList(new Pet("Nemo")));

				return person;
			})
			.flatMap(repository::save)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				Map<TypeOfPet, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys(TypeOfPet.CATS, TypeOfPet.FISH);
				assertThat(pets.get(TypeOfPet.CATS)).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield", "Delilah");
				assertThat(pets.get(TypeOfPet.FISH)).extracting(Pet::getName).containsExactlyInAnyOrder("Nemo");
			})
			.verifyComplete();
	}

	@Test
	void shouldWriteDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Person d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R1");
		newPerson.getRelatives().put(TypeOfRelative.RELATIVE_1, d);
		d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R2");
		newPerson.getRelatives().put(TypeOfRelative.RELATIVE_2, d);

		List<PersonWithRelatives> recorded = new ArrayList<>();
		repository.save(newPerson)
			.as(StepVerifier::create)
			.recordWith(() -> recorded)
			.consumeNextWith(personWithRelatives -> {
				Map<TypeOfRelative, Person> relatives = personWithRelatives.getRelatives();
				assertThat(relatives).containsOnlyKeys(TypeOfRelative.RELATIVE_1, TypeOfRelative.RELATIVE_2);
			})
			.verifyComplete();

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
					+ "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id "
					+ "RETURN size((t)-->(:Person))"
					+ " as numberOfRelations",
				Values.parameters("id", recorded.get(0).getId()))
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

		List<PersonWithRelatives> recorded = new ArrayList<>();
		repository.save(newPerson)
			.as(StepVerifier::create)
			.recordWith(() -> recorded)
			.consumeNextWith(person -> {
				Map<TypeOfPet, List<Pet>> writtenPets = person.getPets();
				assertThat(writtenPets).containsOnlyKeys(TypeOfPet.MONSTERS, TypeOfPet.FISH);
			})
			.verifyComplete();

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
				+ "MATCH (t:" + labelOfTestSubject + ") WHERE id(t) = $id "
				+ "RETURN size((t)-->(:Pet))"
				+ " as numberOfRelations", Values.parameters("id", recorded.get(0).getId()))
				.single().get("numberOfRelations").asLong();
			assertThat(numberOfRelations).isEqualTo(3L);
		}
	}

	interface PersonWithRelativesRepository extends ReactiveNeo4jRepository<PersonWithRelatives, Long> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

	}
}
