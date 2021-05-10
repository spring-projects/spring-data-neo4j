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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.ImmutableVersionedThing;
import org.springframework.data.neo4j.integration.shared.common.VersionedThing;
import org.springframework.data.neo4j.integration.shared.common.VersionedThingWithAssignedId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
class OptimisticLockingIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	@Autowired
	OptimisticLockingIT(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	void setup() {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction tx = session.beginTransaction()) {
			tx.run("MATCH (n) detach delete n");
			tx.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void shouldIncrementVersions(@Autowired VersionedThingRepository repository) {
		VersionedThing thing = repository.save(new VersionedThing("Thing1"));

		assertThat(thing.getMyVersion()).isEqualTo(0L);

		thing = repository.save(thing);

		assertThat(thing.getMyVersion()).isEqualTo(1L);

	}

	@Test
	void shouldIncrementVersionsForMultipleSave(@Autowired VersionedThingRepository repository) {
		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");
		List<VersionedThing> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThing> versionedThings = repository.saveAll(thingsToSave);

		assertThat(versionedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(0L));

		versionedThings = repository.saveAll(versionedThings);

		assertThat(versionedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(1L));

	}

	@Test
	void shouldIncrementVersionsOnRelatedEntities(@Autowired VersionedThingRepository repository) {
		VersionedThing parentThing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");

		parentThing.setOtherVersionedThings(Collections.singletonList(childThing));

		VersionedThing thing = repository.save(parentThing);

		assertThat(thing.getOtherVersionedThings().get(0).getMyVersion()).isEqualTo(0L);

		thing = repository.save(thing);

		assertThat(thing.getOtherVersionedThings().get(0).getMyVersion()).isEqualTo(1L);
	}

	@Test
	void shouldFailIncrementVersions(@Autowired VersionedThingRepository repository) {
		VersionedThing thing = repository.save(new VersionedThing("Thing1"));

		thing.setMyVersion(1L); // Version in DB is 0

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() -> repository.save(thing));

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSave(@Autowired VersionedThingRepository repository) {
		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");
		List<VersionedThing> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThing> versionedThings = repository.saveAll(thingsToSave);

		versionedThings.get(0).setMyVersion(1L); // Version in DB is 0

		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> repository.saveAll(versionedThings));

	}

	@Test
	void shouldFailIncrementVersionsOnRelatedEntities(@Autowired VersionedThingRepository repository) {
		VersionedThing parentThing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");
		parentThing.setOtherVersionedThings(Collections.singletonList(childThing));

		VersionedThing thing = repository.save(parentThing);

		thing.getOtherVersionedThings().get(0).setMyVersion(1L); // Version in DB is 0

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() -> repository.save(thing));

	}

	@Test
	void shouldIncrementVersionsForAssignedId(@Autowired VersionedThingWithAssignedIdRepository repository) {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = repository.save(thing1);

		assertThat(thing.getMyVersion()).isEqualTo(0L);

		thing = repository.save(thing);

		assertThat(thing.getMyVersion()).isEqualTo(1L);

	}

	@Test
	void shouldIncrementVersionsForMultipleSaveForAssignedId(
			@Autowired VersionedThingWithAssignedIdRepository repository) {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = repository.saveAll(thingsToSave);

		assertThat(versionedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(0L));

		versionedThings = repository.saveAll(versionedThings);

		assertThat(versionedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(1L));

	}

	@Test
	void shouldFailIncrementVersionsForAssignedIds(@Autowired VersionedThingWithAssignedIdRepository repository) {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = repository.save(thing1);

		thing.setMyVersion(1L); // Version in DB is 0

		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() -> repository.save(thing));

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSaveForAssignedId(
			@Autowired VersionedThingWithAssignedIdRepository repository) {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = repository.saveAll(thingsToSave);

		versionedThings.get(0).setMyVersion(1L); // Version in DB is 0

		assertThatExceptionOfType(OptimisticLockingFailureException.class)
				.isThrownBy(() -> repository.saveAll(versionedThings));

	}

	@Test
	void shouldNotFailOnDeleteByIdWithNullVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		repository.deleteById(1L);

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

		VersionedThingWithAssignedId thing = repository.findById(1L).get();
		repository.delete(thing);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			long count = session.run("MATCH (v:VersionedThingWithAssignedId) return count(v) as vCount").single()
							.get("vCount").asLong();

			assertThat(count).isEqualTo(0);
		}
	}

	@Test
	void shouldNotFailOnDeleteByIdWithAnyVersion(@Autowired VersionedThingWithAssignedIdRepository repository) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (v:VersionedThingWithAssignedId {id:1, myVersion:3})").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		repository.deleteById(1L);

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

		VersionedThingWithAssignedId thing = repository.findById(1L).get();
		thing.setMyVersion(3L);
		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() -> repository.delete(thing));
	}

	@Test // GH-2154
	void immutablesShouldWork(@Autowired Neo4jTemplate neo4jTemplate) {

		ImmutableVersionedThing immutableVersionedThing = new ImmutableVersionedThing(23L, "Hello");

		immutableVersionedThing = neo4jTemplate.save(immutableVersionedThing);
		assertThat(immutableVersionedThing.getMyVersion()).isNotNull();

		ImmutableVersionedThing copy = immutableVersionedThing.withMyVersion(4711L).withName("World");
		assertThatExceptionOfType(OptimisticLockingFailureException.class).isThrownBy(() -> neo4jTemplate.save(copy));
	}

	@Test
	void shouldNotTraverseToBidiRelatedThingWithOldVersion(@Autowired VersionedThingRepository repository) {

		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");

		thing1.setOtherVersionedThings(Collections.singletonList(thing2));
		repository.save(thing1);

		thing1 = repository.findById(thing1.getId()).get();
		thing2 = repository.findById(thing2.getId()).get();

		thing2.setOtherVersionedThings(Collections.singletonList(thing1));
		repository.save(thing2);

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
		repository.save(thing1);
		// Initially creates:
		// Thing1-[:HAS]->Thing2
		// Thing1-[:HAS]->Thing3
		// Thing1-[:HAS]->Thing4

		thing1 = repository.findById(thing1.getId()).get();
		thing3 = repository.findById(thing3.getId()).get();
		thing3.setOtherVersionedThings(Collections.singletonList(thing1));
		repository.save(thing3);
		// adds
		// Thing3-[:HAS]->Thing1

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Long relationshipCount = session
					.run("MATCH (:VersionedThing)-[r:HAS]->(:VersionedThing) return count(r) as relationshipCount")
					.single().get("relationshipCount").asLong();
			assertThat(relationshipCount).isEqualTo(4);
		}
	}

	interface VersionedThingRepository extends Neo4jRepository<VersionedThing, Long> {}

	interface VersionedThingWithAssignedIdRepository extends Neo4jRepository<VersionedThingWithAssignedId, Long> {}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
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
