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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.VersionedThing;
import org.springframework.data.neo4j.integration.shared.VersionedThingWithAssignedId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
class OptimisticLockingIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	@Autowired
	OptimisticLockingIT(Driver driver) {
		this.driver = driver;
	}

	@BeforeEach
	void setup() {
		Session session = driver.session(SessionConfig.defaultConfig());
		Transaction transaction = session.beginTransaction();
		transaction.run("MATCH (n) detach delete n");
		transaction.commit();
		session.close();
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

	}
}
