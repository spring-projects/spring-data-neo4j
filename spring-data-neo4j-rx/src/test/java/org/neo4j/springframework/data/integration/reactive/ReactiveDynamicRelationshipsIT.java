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
package org.neo4j.springframework.data.integration.reactive;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assumptions.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

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
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.DynamicRelationshipsITBase;
import org.neo4j.springframework.data.integration.shared.Person;
import org.neo4j.springframework.data.integration.shared.PersonWithRelatives;
import org.neo4j.springframework.data.integration.shared.Pet;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveDynamicRelationshipsIT extends DynamicRelationshipsITBase {

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

				Map<String, Person> relatives = person.getRelatives();
				assertThat(relatives).containsOnlyKeys("HAS_WIFE", "HAS_DAUGHTER");
				assertThat(relatives.get("HAS_WIFE").getFirstName()).isEqualTo("B");
				assertThat(relatives.get("HAS_DAUGHTER").getFirstName()).isEqualTo("C");
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

				Map<String, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys("CATS", "DOGS");
				assertThat(pets.get("CATS")).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield");
				assertThat(pets.get("DOGS")).extracting(Pet::getName).containsExactlyInAnyOrder("Benji", "Lassie");
			})
			.verifyComplete();
	}

	@Test
	void shouldUpdateDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.map(person -> {
				assumeThat(person).isNotNull();
				assumeThat(person.getName()).isEqualTo("A");

				Map<String, Person> relatives = person.getRelatives();
				assumeThat(relatives).containsOnlyKeys("HAS_WIFE", "HAS_DAUGHTER");

				relatives.remove("HAS_WIFE");
				Person d = new Person();
				ReflectionTestUtils.setField(d, "firstName", "D");
				relatives.put("HAS_SON", d);
				ReflectionTestUtils.setField(relatives.get("HAS_DAUGHTER"), "firstName", "C2");
				return person;
			})
			.flatMap(repository::save)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				Map<String, Person> relatives = person.getRelatives();
				assertThat(relatives).containsOnlyKeys("HAS_DAUGHTER", "HAS_SON");
				assertThat(relatives.get("HAS_DAUGHTER").getFirstName()).isEqualTo("C2");
				assertThat(relatives.get("HAS_SON").getFirstName()).isEqualTo("D");
			})
			.verifyComplete();
	}

	@Test // GH-216
	void shouldUpdateDynamicCollectionRelationships(@Autowired PersonWithRelativesRepository repository) {

		repository.findById(idOfExistingPerson)
			.map(person -> {
				assumeThat(person).isNotNull();
				assumeThat(person.getName()).isEqualTo("A");

				Map<String, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys("CATS", "DOGS");

				pets.remove("DOGS");
				pets.get("CATS").add(new Pet("Delilah"));

				pets.put("FISH", Collections.singletonList(new Pet("Nemo")));

				return person;
			})
			.flatMap(repository::save)
			.as(StepVerifier::create)
			.consumeNextWith(person -> {
				Map<String, List<Pet>> pets = person.getPets();
				assertThat(pets).containsOnlyKeys("CATS", "FISH");
				assertThat(pets.get("CATS")).extracting(Pet::getName).containsExactlyInAnyOrder("Tom", "Garfield", "Delilah");
				assertThat(pets.get("FISH")).extracting(Pet::getName).containsExactlyInAnyOrder("Nemo");
			})
			.verifyComplete();
	}

	@Test
	void shouldWriteDynamicRelationships(@Autowired PersonWithRelativesRepository repository) {

		PersonWithRelatives newPerson = new PersonWithRelatives("Test");
		Person d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R1");
		newPerson.getRelatives().put("RELATIVE_1", d);
		d = new Person();
		ReflectionTestUtils.setField(d, "firstName", "R2");
		newPerson.getRelatives().put("RELATIVE_2", d);

		List<PersonWithRelatives> recorded = new ArrayList<>();
		repository.save(newPerson)
			.as(StepVerifier::create)
			.recordWith(() -> recorded)
			.consumeNextWith(personWithRelatives -> {
				Map<String, Person> relatives = personWithRelatives.getRelatives();
				assertThat(relatives).containsOnlyKeys("RELATIVE_1", "RELATIVE_2");
			})
			.verifyComplete();

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
					+ "MATCH (t:PersonWithRelatives) WHERE id(t) = $id "
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
		Map<String, List<Pet>> pets = newPerson.getPets();

		List<Pet> monsters = pets.computeIfAbsent("MONSTERS", s -> new ArrayList<>());
		monsters.add(new Pet("Godzilla"));
		monsters.add(new Pet("King Kong"));

		List<Pet> fish = pets.computeIfAbsent("FISH", s -> new ArrayList<>());
		fish.add(new Pet("Nemo"));

		List<PersonWithRelatives> recorded = new ArrayList<>();
		repository.save(newPerson)
			.as(StepVerifier::create)
			.recordWith(() -> recorded)
			.consumeNextWith(person -> {
				Map<String, List<Pet>> writtenPets = person.getPets();
				assertThat(writtenPets).containsOnlyKeys("MONSTERS", "FISH");
			})
			.verifyComplete();

		try (Transaction transaction = driver.session().beginTransaction()) {
			long numberOfRelations = transaction.run(""
				+ "MATCH (t:PersonWithRelatives) WHERE id(t) = $id "
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
