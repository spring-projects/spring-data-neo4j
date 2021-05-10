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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Multiple1O1Relationships;
import org.springframework.data.neo4j.integration.shared.common.MultipleRelationshipsThing;
import org.springframework.data.neo4j.integration.shared.common.RelationshipsITBase;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test cases for various relationship scenarios (self references, multiple times to same instance).
 *
 * @author Michael J. Simons
 */
class RelationshipsIT extends RelationshipsITBase {

	@Autowired
	RelationshipsIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void shouldSaveSingleRelationship(@Autowired MultipleRelationshipsThingRepository repository) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeA(new MultipleRelationshipsThing("c"));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent().map(MultipleRelationshipsThing::getTypeA)
				.map(MultipleRelationshipsThing::getName).hasValue("c");

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
		assertThat(loadedThing).isPresent().map(MultipleRelationshipsThing::getTypeB)
				.hasValueSatisfying(l -> assertThat(l).extracting(MultipleRelationshipsThing::getName).containsExactly("c"));

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
	void shouldSaveMultipleRelationshipsOfSameObjectType(@Autowired MultipleRelationshipsThingRepository repository,
														 @Autowired BookmarkCapture bookmarkCapture) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeA(new MultipleRelationshipsThing("c1"));
		p.setTypeB(Collections.singletonList(new MultipleRelationshipsThing("c2")));
		p.setTypeC(Collections.singletonList(new MultipleRelationshipsThing("c3")));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent().hasValueSatisfying(t -> {

			MultipleRelationshipsThing typeA = t.getTypeA();
			List<MultipleRelationshipsThing> typeB = t.getTypeB();
			List<MultipleRelationshipsThing> typeC = t.getTypeC();

			assertThat(typeA).isNotNull();
			assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
			assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c2");
			assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c3");
		});

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {

			List<String> names = session
					.run("MATCH (n:MultipleRelationshipsThing {name: 'p'}) - [r:TYPE_A|TYPE_B|TYPE_C] -> (o) RETURN r, o")
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
	void shouldSaveMultipleRelationshipsOfSameInstance(@Autowired MultipleRelationshipsThingRepository repository,
													   @Autowired BookmarkCapture bookmarkCapture) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		MultipleRelationshipsThing c = new MultipleRelationshipsThing("c1");
		p.setTypeA(c);
		p.setTypeB(Collections.singletonList(c));
		p.setTypeC(Collections.singletonList(c));

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent().hasValueSatisfying(t -> {

			MultipleRelationshipsThing typeA = t.getTypeA();
			List<MultipleRelationshipsThing> typeB = t.getTypeB();
			List<MultipleRelationshipsThing> typeC = t.getTypeC();

			assertThat(typeA).isNotNull();
			assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
			assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
			assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
		});

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {

			List<String> names = session
					.run("MATCH (n:MultipleRelationshipsThing {name: 'p'}) - [r:TYPE_A|TYPE_B|TYPE_C] -> (o) RETURN r, o")
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
			@Autowired MultipleRelationshipsThingRepository repository,
			@Autowired BookmarkCapture bookmarkCapture) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		MultipleRelationshipsThing c = new MultipleRelationshipsThing("c1");
		p.setTypeA(c);
		p.setTypeB(Collections.singletonList(c));
		p.setTypeC(Collections.singletonList(c));

		c.setTypeA(p);

		p = repository.save(p);

		Optional<MultipleRelationshipsThing> loadedThing = repository.findById(p.getId());
		assertThat(loadedThing).isPresent().hasValueSatisfying(t -> {

			MultipleRelationshipsThing typeA = t.getTypeA();
			List<MultipleRelationshipsThing> typeB = t.getTypeB();
			List<MultipleRelationshipsThing> typeC = t.getTypeC();

			assertThat(typeA).isNotNull();
			assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
			assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
			assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
		});

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {

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

	@Test // DATAGRAPH-1424
	void shouldMatchOnTheCorrectRelationship(@Autowired Multiple1O1RelationshipsRepository repository,
											 @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction tx = session.beginTransaction()) {
			tx.run(""
				   + "CREATE (p1:AltPerson {name: 'val1'})\n"
				   + "CREATE (p2:AltPerson {name: 'val2'})\n"
				   + "CREATE (p3:AltPerson {name: 'val3'})\n"
				   + "CREATE (m1:Multiple1O1Relationships {name: 'm1'})\n"
				   + "CREATE (m2:Multiple1O1Relationships {name: 'm2'})\n"
				   + "CREATE (m1) - [:REL_1] -> (p1)\n"
				   + "CREATE (m1) - [:REL_2] -> (p2)\n"
				   + "CREATE (m2) - [:REL_1] -> (p1)\n"
				   + "CREATE (m2) - [:REL_2] -> (p3)");
			tx.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		List<Multiple1O1Relationships> objects = repository.findAllByPerson1NameAndPerson2Name("val1", "val2");

		assertThat(objects).hasSize(1).first()
				.satisfies(m -> {
					assertThat(m.getName()).isEqualTo("m1");
					assertThat(m.getPerson1().getName()).isEqualTo("val1");
					assertThat(m.getPerson2().getName()).isEqualTo("val2");
				});
	}

	interface MultipleRelationshipsThingRepository extends CrudRepository<MultipleRelationshipsThing, Long> {}

	interface Multiple1O1RelationshipsRepository extends CrudRepository<Multiple1O1Relationships, Long> {

		List<Multiple1O1Relationships> findAllByPerson1NameAndPerson2Name(String name1, String name2);
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
