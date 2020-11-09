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

import static org.assertj.core.api.Assertions.assertThat;

import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.VersionedThing;
import org.springframework.data.neo4j.integration.shared.common.VersionedThingWithAssignedId;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveOptimisticLockingIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	@Autowired
	ReactiveOptimisticLockingIT(Driver driver) {
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

		savedThings.get(1).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.saveAll(savedThings)).expectError(OptimisticLockingFailureException.class).verify();

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

	}
}
