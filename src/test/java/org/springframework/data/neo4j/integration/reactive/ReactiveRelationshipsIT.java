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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.MultipleRelationshipsThing;
import org.springframework.data.neo4j.integration.shared.common.RelationshipsITBase;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test cases for various relationship scenarios (self references, multiple times to same instance).
 *
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveRelationshipsIT extends RelationshipsITBase {

	@Autowired
	ReactiveRelationshipsIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void shouldSaveSingleRelationship(@Autowired MultipleRelationshipsThingRepository repository,
									  @Autowired BookmarkCapture bookmarkCapture) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeA(new MultipleRelationshipsThing("c"));

		repository.save(p).map(MultipleRelationshipsThing::getId).flatMap(repository::findById).as(StepVerifier::create)
				.assertNext(loadedThing -> assertThat(loadedThing).extracting(MultipleRelationshipsThing::getTypeA)
						.extracting(MultipleRelationshipsThing::getName).isEqualTo("c"))
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<String> names = session.run("MATCH (n:MultipleRelationshipsThing) RETURN n.name AS name")
					.list(r -> r.get("name").asString());
			assertThat(names).hasSize(2).containsExactlyInAnyOrder("p", "c");
		}
	}

	@Test
	void shouldSaveSingleRelationshipInList(@Autowired MultipleRelationshipsThingRepository repository,
											@Autowired BookmarkCapture bookmarkCapture) {

		MultipleRelationshipsThing p = new MultipleRelationshipsThing("p");
		p.setTypeB(Collections.singletonList(new MultipleRelationshipsThing("c")));

		repository.save(p).map(MultipleRelationshipsThing::getId).flatMap(repository::findById).as(StepVerifier::create)
				.assertNext(loadedThing -> assertThat(loadedThing.getTypeB()).extracting(MultipleRelationshipsThing::getName)
						.containsExactly("c"))
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
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

		repository.save(p).map(MultipleRelationshipsThing::getId).flatMap(repository::findById).as(StepVerifier::create)
				.assertNext(loadedThing -> {
					MultipleRelationshipsThing typeA = loadedThing.getTypeA();
					List<MultipleRelationshipsThing> typeB = loadedThing.getTypeB();
					List<MultipleRelationshipsThing> typeC = loadedThing.getTypeC();

					assertThat(typeA).isNotNull();
					assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
					assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c2");
					assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c3");
				}).verifyComplete();

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

		repository.save(p).map(MultipleRelationshipsThing::getId).flatMap(repository::findById).as(StepVerifier::create)
				.assertNext(loadedThing -> {

					MultipleRelationshipsThing typeA = loadedThing.getTypeA();
					List<MultipleRelationshipsThing> typeB = loadedThing.getTypeB();
					List<MultipleRelationshipsThing> typeC = loadedThing.getTypeC();

					assertThat(typeA).isNotNull();
					assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
					assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
					assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
				}).verifyComplete();

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

		repository.save(p).map(MultipleRelationshipsThing::getId).flatMap(repository::findById).as(StepVerifier::create)
				.assertNext(loadedThing -> {

					MultipleRelationshipsThing typeA = loadedThing.getTypeA();
					List<MultipleRelationshipsThing> typeB = loadedThing.getTypeB();
					List<MultipleRelationshipsThing> typeC = loadedThing.getTypeC();

					assertThat(typeA).isNotNull();
					assertThat(typeA).extracting(MultipleRelationshipsThing::getName).isEqualTo("c1");
					assertThat(typeB).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
					assertThat(typeC).extracting(MultipleRelationshipsThing::getName).containsExactly("c1");
				}).verifyComplete();

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

	interface MultipleRelationshipsThingRepository extends ReactiveCrudRepository<MultipleRelationshipsThing, Long> {}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
