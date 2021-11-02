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

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.VersionedThing;
import org.springframework.data.neo4j.integration.shared.common.VersionedThingWithAssignedId;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveOptimisticLockingIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	@Autowired
	ReactiveOptimisticLockingIT(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	void setup() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void shouldIncrementVersions(@Autowired VersionedThingRepository repository) {

		// would love to verify version change null -> 0 and 0 -> 1 within one test
		VersionedThing thing1 = repository.save(new VersionedThing("Thing1")).block();
		StepVerifier.create(repository.save(thing1))
				.assertNext(versionedThing -> assertThat(versionedThing.getMyVersion()).isEqualTo(1L)).verifyComplete();

	}

	@Test
	void shouldIncrementVersionsForMultipleSave(@Autowired VersionedThingRepository repository) {

		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");
		List<VersionedThing> thingsToSave = Arrays.asList(thing1, thing2);

		StepVerifier.create(repository.saveAll(thingsToSave)).recordWith(ArrayList::new).expectNextCount(2)
				.consumeRecordedWith(versionedThings -> assertThat(versionedThings)
						.allMatch(versionedThing -> versionedThing.getMyVersion().equals(0L)))
				.verifyComplete();

	}

	@Test
	void shouldIncrementVersionsOnRelatedEntities(@Autowired VersionedThingRepository repository) {

		VersionedThing parentThing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");

		parentThing.setOtherVersionedThings(Collections.singletonList(childThing));

		StepVerifier.create(repository.save(parentThing))
				.assertNext(
						versionedThing -> assertThat(versionedThing.getOtherVersionedThings().get(0).getMyVersion()).isEqualTo(0L))
				.verifyComplete();
	}

	@Test
	void shouldFailIncrementVersions(@Autowired VersionedThingRepository repository) {

		VersionedThing thing = repository.save(new VersionedThing("Thing1")).block();
		thing.setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.save(thing)).expectError(OptimisticLockingFailureException.class).verify();

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSave(@Autowired VersionedThingRepository repository) {

		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");

		List<VersionedThing> things = Arrays.asList(thing1, thing2);
		List<VersionedThing> savedThings = repository.saveAll(things).collectList().block();

		savedThings.get(0).setMutableProperty("changed");
		savedThings.get(1).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.saveAll(savedThings))
				.expectNextCount(1L)
				.expectError(OptimisticLockingFailureException.class)
				.verify();

		// Make sure the first object that has the correct version number doesn't get persisted either
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long cnt = session.run(
					"MATCH (n:VersionedThing) WHERE id(n) = $id AND n.mutableProperty = 'changed' RETURN count(*)",
					Collections.singletonMap("id", savedThings.get(0).getId())
			).single().get(0).asLong();
			assertThat(cnt).isEqualTo(0L);
		}
	}

	@Test
	void shouldFailIncrementVersionsOnRelatedEntities(@Autowired VersionedThingRepository repository) {

		VersionedThing thing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");
		thing.setOtherVersionedThings(Collections.singletonList(childThing));
		VersionedThing savedThing = repository.save(thing).block();
		savedThing.getOtherVersionedThings().get(0).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.save(savedThing)).expectError(OptimisticLockingFailureException.class).verify();

	}

	@Test
	void shouldIncrementVersionsForAssignedId(@Autowired VersionedThingWithAssignedIdRepository repository) {

		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = repository.save(thing1).block();

		assertThat(thing.getMyVersion()).isEqualTo(0L);

		StepVerifier.create(repository.save(thing))
				.assertNext(savedThing -> assertThat(savedThing.getMyVersion()).isEqualTo(1L)).verifyComplete();

	}

	@Test
	void shouldIncrementVersionsForMultipleSaveForAssignedId(
			@Autowired VersionedThingWithAssignedIdRepository repository) {

		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = repository.saveAll(thingsToSave).collectList().block();

		StepVerifier.create(repository.saveAll(versionedThings)).recordWith(ArrayList::new).expectNextCount(2)
				.consumeRecordedWith(
						savedThings -> assertThat(savedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(1L)))
				.verifyComplete();
	}

	@Test
	void shouldFailIncrementVersionsForAssignedIds(@Autowired VersionedThingWithAssignedIdRepository repository) {

		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = repository.save(thing1).block();

		thing.setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.save(thing)).expectError(OptimisticLockingFailureException.class).verify();

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSaveForAssignedId(
			@Autowired VersionedThingWithAssignedIdRepository repository) {

		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = repository.saveAll(thingsToSave).collectList().block();

		versionedThings.get(0).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.saveAll(versionedThings)).expectError(OptimisticLockingFailureException.class)
				.verify();

	}

	@Test
	void shouldNotFailOnDeleteByIdWithNullVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		StepVerifier.create(repository.deleteById(1L))
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long count = session.run("MATCH (v:VersionedThingWithAssignedId) return count(v) as vCount").single()
							.get("vCount").asLong();

			assertThat(count).isEqualTo(0);
		}
	}

	@Test
	void shouldNotFailOnDeleteByEntityWithNullVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		StepVerifier.create(repository.findById(1L).map(thing -> repository.deleteById(1L)))
				.expectNextCount(1L)
				.verifyComplete();

	}

	@Test
	void shouldNotFailOnDeleteByIdWithAnyVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1, myVersion:3})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		StepVerifier.create(repository.deleteById(1L))
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long count = session.run("MATCH (v:VersionedThingWithAssignedId) return count(v) as vCount").single()
							.get("vCount").asLong();

			assertThat(count).isEqualTo(0);
		}
	}

	@Test
	void shouldFailOnDeleteByEntityWithWrongVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1, myVersion:2})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		StepVerifier.create(repository.findById(1L)
				.flatMap(thing -> {
					thing.setMyVersion(3L);
					return repository.delete(thing);
		})).verifyError(OptimisticLockingFailureException.class);

	}

	@Test
	void shouldNotTraverseToBidiRelatedThingWithOldVersion(@Autowired VersionedThingRepository repository) {

		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");

		thing1.setOtherVersionedThings(Collections.singletonList(thing2));
		repository.save(thing1)
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

		Flux.zip(repository.findById(thing1.getId()), (repository.findById(thing2.getId())))
				.flatMap(t -> {
					VersionedThing thing1n = t.getT1();
					VersionedThing thing2n = t.getT2();

					thing2n.setOtherVersionedThings(Collections.singletonList(thing1n));
					return repository.save(thing2n);
				})
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> result = session
					.run("MATCH (t:VersionedThing{name:'Thing1'})-[:HAS]->(:VersionedThing{name:'Thing2'}) return t")
					.list();
			assertThat(result).hasSize(1);
		}
	}

	@Test // GH-2191
	void shouldNotTraverseToBidiRelatedThingsWithOldVersion(@Autowired VersionedThingRepository repository) {

		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");
		VersionedThing thing3 = new VersionedThing("Thing3");
		VersionedThing thing4 = new VersionedThing("Thing4");

		List<VersionedThing> thing1Relationships = new ArrayList<>();
		thing1Relationships.add(thing2);
		thing1Relationships.add(thing3);
		thing1Relationships.add(thing4);
		thing1.setOtherVersionedThings(thing1Relationships);
		StepVerifier.create(repository.save(thing1))
				.expectNextCount(1)
				.verifyComplete();

		Flux.zip(repository.findById(thing1.getId()), repository.findById(thing3.getId()))
				.flatMap(tuple -> {
				tuple.getT2().setOtherVersionedThings(Collections.singletonList(tuple.getT1()));
				return repository.save(tuple.getT2());
				})
				.as(StepVerifier::create)
				.expectNextCount(1)
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Long relationshipCount = session
					.run("MATCH (:VersionedThing)-[r:HAS]->(:VersionedThing) return count(r) as relationshipCount")
					.single().get("relationshipCount").asLong();
			assertThat(relationshipCount).isEqualTo(4);
		}
	}

	interface VersionedThingRepository extends ReactiveNeo4jRepository<VersionedThing, Long> {}

	interface VersionedThingWithAssignedIdRepository
			extends ReactiveNeo4jRepository<VersionedThingWithAssignedId, Long> {}

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
