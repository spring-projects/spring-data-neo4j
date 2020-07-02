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

import static org.assertj.core.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.MultipleRelationshipsThing;
import org.springframework.data.neo4j.integration.shared.RelationshipsITBase;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test cases for various relationship scenarios (self references, multiple times to same instance).
 *
 * @author Michael J. Simons
 */
class RelationshipsIT extends RelationshipsITBase {

	@Autowired RelationshipsIT(Driver driver) {
		super(driver);
	}

	@Test
	void shouldSaveSingleRelationship(@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeA(new MultipleRelationshipsThing("c"));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent()
			.map(MultipleRelationshipsThing::getTypeA)
			.map(MultipleRelationshipsThing::getName)
			.hasValue("c");

		try (Session session = driver.session()) {
			List<String> names = session.run("MATCH (n:MultipleRelationshipsThing) RETURN n.name AS name")
				.list(r -> r.get("name").asString());
			assertThat(names).hasSize(2).containsExactlyInAnyOrder("p", "c");
		}
	}

	@Test
	void shouldSaveSingleRelationshipInList(@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeB(Collections.singletonList(new MultipleRelationshipsThing("c")));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent()
			.map(MultipleRelationshipsThing::getTypeB)
			.hasValueSatisfying(
				l -> assertThat(l).extracting(MultipleRelationshipsThing::getName).containsExactly("c"));

		try (Session session = driver.session()) {
			List<String> names = session.run("MATCH (n:MultipleRelationshipsThing) RETURN n.name AS name")
				.list(r -> r.get("name").asString());
			assertThat(names).hasSize(2).containsExactlyInAnyOrder("p", "c");
		}
	}

	/**
	 * This stores multiple, different instances.
	 *
	 * @param repository The repository to use.
	 */
	@Test
	void shouldSaveMultipleRelationshipsOfSameObjectType(@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeA(new MultipleRelationshipsThing("c1"));
		p.setTypeB(Collections.singletonList(new MultipleRelationshipsThing("c2")));
		p.setTypeC(Collections.singletonList(new MultipleRelationshipsThing("c3")));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent()
			.hasValueSatisfying(t -> {

				MultipleRelationshipsThing typeA = t.getTypeA();
				List<MultipleRelationshipsThing> typeB = t.getTypeB();
				List<MultipleRelationshipsThing> typeC = t.getTypeC();

				assertThat(typeA).isNotNull();
				assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
				assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c2");
				assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c3");
			});

		try (Session session = driver.session()) {

			List<String> names = session.run(
				"MATCH (n:MultipleRelationshipsThing {name: 'p'}) - [r:TYPE_A|TYPE_B|TYPE_C] -> (o) RETURN r, o")
				.list(record -> {
					String type = record.get("r").asRelationship().type();
					String name = record.get("o").get("name").asString();
					return type + "_" + name;
				});
			assertThat(names).containsExactlyInAnyOrder("TYPE_A_c1", "TYPE_B_c2", "TYPE_C_c3");
		}
	}

	/**
	 * This stores the same instance in different relationships
	 *
	 * @param repository The repository to use.
	 */
	@Test
	void shouldSaveMultipleRelationshipsOfSameInstance(@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		MultipleRelationshipsThing c = new MultipleRelationshipsThing("c1");
		p.setTypeA(c);
		p.setTypeB(Collections.singletonList(c));
		p.setTypeC(Collections.singletonList(c));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent()
			.hasValueSatisfying(t -> {

				MultipleRelationshipsThing typeA = t.getTypeA();
				List<MultipleRelationshipsThing> typeB = t.getTypeB();
				List<MultipleRelationshipsThing> typeC = t.getTypeC();

				assertThat(typeA).isNotNull();
				assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
				assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
				assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
			});

		try (Session session = driver.session()) {

			List<String> names = session.run(
				"MATCH (n:MultipleRelationshipsThing {name: 'p'}) - [r:TYPE_A|TYPE_B|TYPE_C] -> (o) RETURN r, o")
				.list(record -> {
					String type = record.get("r").asRelationship().type();
					String name = record.get("o").get("name").asString();
					return type + "_" + name;
				});
			assertThat(names).containsExactlyInAnyOrder("TYPE_A_c1", "TYPE_B_c1", "TYPE_C_c1");
		}
	}

	/**
	 * This stores the same instance in different relationships
	 *
	 * @param repository The repository to use.
	 */
	@Test
	void shouldSaveMultipleRelationshipsOfSameInstanceWithBackReference(
		@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		MultipleRelationshipsThing c = new MultipleRelationshipsThing("c1");
		p.setTypeA(c);
		p.setTypeB(Collections.singletonList(c));
		p.setTypeC(Collections.singletonList(c));

		c.setTypeA(p);

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent()
			.hasValueSatisfying(t -> {

				MultipleRelationshipsThing typeA = t.getTypeA();
				List<MultipleRelationshipsThing> typeB = t.getTypeB();
				List<MultipleRelationshipsThing> typeC = t.getTypeC();

				assertThat(typeA).isNotNull();
				assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
				assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
				assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
			});

		try (Session session = driver.session()) {

			Function<Record, String> withMapper = record -> {
				String type = record.get("r").asRelationship().type();
				String name = record.get("o").get("name").asString();
				return type + "_" + name;
			};

			String query = "MATCH (n:MultipleRelationshipsThing {name: $name}) - [r:TYPE_A|TYPE_B|TYPE_C] -> (o) RETURN r, o";
			List<String> names = session.run(query, Collections.singletonMap("name", "p")).list(withMapper);
			assertThat(names).containsExactlyInAnyOrder("TYPE_A_c1", "TYPE_B_c1", "TYPE_C_c1");

			names = session.run(query, Collections.singletonMap("name", "c1")).list(withMapper);
			assertThat(names).containsExactlyInAnyOrder("TYPE_A_p");
		}
	}

	interface MultipleRelationshipsThingRepository extends CrudRepository<MultipleRelationshipsThing, Long> {
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
