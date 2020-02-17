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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.integration.shared.VersionedThing;
import org.neo4j.springframework.data.integration.shared.VersionedThingWithAssignedId;
import org.neo4j.springframework.data.repository.ReactiveNeo4jRepository;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveOptimisticLockingIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;
	@Autowired private VersionedThingRepository repository;
	@Autowired private VersionedThingWithAssignedIdRepository assignedIdRepository;
	@Autowired private Driver driver;

	@BeforeEach
	void setup() {
		Session session = driver.session(SessionConfig.defaultConfig());
		Transaction transaction = session.beginTransaction();
		transaction.run("MATCH (n) detach delete n");
		transaction.commit();
		session.close();
	}

	@Test
	void shouldIncrementVersions() {

		// would love to verify version change null -> 0 and 0 -> 1 within one test
		VersionedThing thing1 = repository.save(new VersionedThing("Thing1")).block();
		StepVerifier.create(repository.save(thing1))
			.assertNext(versionedThing -> assertThat(versionedThing.getMyVersion()).isEqualTo(1L))
			.verifyComplete();

	}

	@Test
	void shouldIncrementVersionsForMultipleSave() {
		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");
		List<VersionedThing> thingsToSave = Arrays.asList(thing1, thing2);

		StepVerifier.create(repository.saveAll(thingsToSave))
			.recordWith(ArrayList::new)
			.expectNextCount(2)
			.consumeRecordedWith(versionedThings ->
				assertThat(versionedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(0L)))
			.verifyComplete();

	}

	@Test
	void shouldIncrementVersionsOnRelatedEntities() {
		VersionedThing parentThing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");

		parentThing.setOtherVersionedThings(singletonList(childThing));

		StepVerifier.create(repository.save(parentThing))
			.assertNext(versionedThing ->
				assertThat(versionedThing.getOtherVersionedThings().get(0).getMyVersion()).isEqualTo(0L))
			.verifyComplete();
	}

	@Test
	void shouldFailIncrementVersions() {
		VersionedThing thing = repository.save(new VersionedThing("Thing1")).block();
		thing.setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.save(thing))
			.expectError(OptimisticLockingFailureException.class)
			.verify();

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSave() {
		VersionedThing thing1 = new VersionedThing("Thing1");
		VersionedThing thing2 = new VersionedThing("Thing2");

		List<VersionedThing> things = Arrays.asList(thing1, thing2);
		List<VersionedThing> savedThings = repository.saveAll(things).collectList().block();

		savedThings.get(1).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.saveAll(savedThings))
			.expectError(OptimisticLockingFailureException.class)
			.verify();

	}

	@Test
	void shouldFailIncrementVersionsOnRelatedEntities() {
		VersionedThing thing = new VersionedThing("Thing1");
		VersionedThing childThing = new VersionedThing("Thing2");
		thing.setOtherVersionedThings(singletonList(childThing));
		VersionedThing savedThing = repository.save(thing).block();
		savedThing.getOtherVersionedThings().get(0).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(repository.save(savedThing))
			.expectError(OptimisticLockingFailureException.class)
			.verify();

	}

	@Test
	void shouldIncrementVersionsForAssignedId() {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = assignedIdRepository.save(thing1).block();

		assertThat(thing.getMyVersion()).isEqualTo(0L);

		StepVerifier.create(assignedIdRepository.save(thing))
			.assertNext(savedThing -> assertThat(savedThing.getMyVersion()).isEqualTo(1L))
			.verifyComplete();

	}

	@Test
	void shouldIncrementVersionsForMultipleSaveForAssignedId() {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = assignedIdRepository.saveAll(thingsToSave).collectList().block();

		StepVerifier.create(assignedIdRepository.saveAll(versionedThings))
			.recordWith(ArrayList::new)
			.expectNextCount(2)
			.consumeRecordedWith(savedThings ->
				assertThat(savedThings).allMatch(versionedThing -> versionedThing.getMyVersion().equals(1L)))
			.verifyComplete();



	}

	@Test
	void shouldFailIncrementVersionsForAssignedIds() {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing = assignedIdRepository.save(thing1).block();

		thing.setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(assignedIdRepository.save(thing))
			.expectError(OptimisticLockingFailureException.class)
			.verify();

	}

	@Test
	void shouldFailIncrementVersionsForMultipleSaveForAssignedId() {
		VersionedThingWithAssignedId thing1 = new VersionedThingWithAssignedId(4711L, "Thing1");
		VersionedThingWithAssignedId thing2 = new VersionedThingWithAssignedId(42L, "Thing2");
		List<VersionedThingWithAssignedId> thingsToSave = Arrays.asList(thing1, thing2);

		List<VersionedThingWithAssignedId> versionedThings = assignedIdRepository.saveAll(thingsToSave).collectList().block();

		versionedThings.get(0).setMyVersion(1L); // Version in DB is 0

		StepVerifier.create(assignedIdRepository.saveAll(versionedThings))
			.expectError(OptimisticLockingFailureException.class)
			.verify();

	}

	public interface VersionedThingRepository extends ReactiveNeo4jRepository<VersionedThing, Long> {

	}

	public interface VersionedThingWithAssignedIdRepository extends ReactiveNeo4jRepository<VersionedThingWithAssignedId, Long> {

	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return singletonList(VersionedThing.class.getPackage().getName());
		}
	}
}
