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
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.config.EnableNeo4jAuditing;
import org.neo4j.springframework.data.integration.shared.AuditingITBase;
import org.neo4j.springframework.data.integration.shared.ImmutableAuditableThing;
import org.neo4j.springframework.data.integration.shared.ImmutableAuditableThingWithGeneratedId;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class AuditingIT extends AuditingITBase {


	@Autowired AuditingIT(Driver driver) {
		super(driver);
	}

	@Test
	void auditingOfCreationShouldWork(@Autowired ImmutableEntityTestRepository repository) {

		ImmutableAuditableThing thing = new ImmutableAuditableThing("A thing");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(thing.getCreatedBy()).isEqualTo("A user");

		assertThat(thing.getModifiedAt()).isNull();
		assertThat(thing.getModifiedBy()).isNull();

		verifyDatabase(thing.getId(), thing);
	}

	@Test
	void auditingOfModificationShouldWork(@Autowired ImmutableEntityTestRepository repository) {

		ImmutableAuditableThing thing = repository.findById(idOfExistingThing).get();
		thing = thing.withName("A new name");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isEqualTo(EXISTING_THING_CREATED_AT);
		assertThat(thing.getCreatedBy()).isEqualTo(EXISTING_THING_CREATED_BY);

		assertThat(thing.getModifiedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(thing.getModifiedBy()).isEqualTo("A user");

		assertThat(thing.getName()).isEqualTo("A new name");

		verifyDatabase(idOfExistingThing, thing);
	}

	@Test
	void auditingOfEntityWithGeneratedIdCreationShouldWork(
		@Autowired ImmutableEntityWithGeneratedIdRepository repository) {

		ImmutableAuditableThingWithGeneratedId thing = new ImmutableAuditableThingWithGeneratedId("A thing");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(thing.getCreatedBy()).isEqualTo("A user");

		assertThat(thing.getModifiedAt()).isNull();
		assertThat(thing.getModifiedBy()).isNull();

		verifyDatabase(thing.getId(), thing);
	}

	@Test
	void auditingOfEntityWithGeneratedIdModificationShouldWork(
		@Autowired ImmutableEntityWithGeneratedIdRepository repository) {

		ImmutableAuditableThingWithGeneratedId thing = repository
			.findById(idOfExistingThingWithGeneratedId).get();

		thing = thing.withName("A new name");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isEqualTo(EXISTING_THING_CREATED_AT);
		assertThat(thing.getCreatedBy()).isEqualTo(EXISTING_THING_CREATED_BY);

		assertThat(thing.getModifiedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(thing.getModifiedBy()).isEqualTo("A user");

		assertThat(thing.getName()).isEqualTo("A new name");

		verifyDatabase(idOfExistingThingWithGeneratedId, thing);
	}

	interface ImmutableEntityTestRepository extends CrudRepository<ImmutableAuditableThing, Long> {
	}

	interface ImmutableEntityWithGeneratedIdRepository
		extends CrudRepository<ImmutableAuditableThingWithGeneratedId, String> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableNeo4jAuditing(modifyOnCreate = false, auditorAwareRef = "auditorProvider", dateTimeProviderRef = "fixedDateTimeProvider")
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ImmutableAuditableThing.class.getPackage().getName());
		}

		@Bean
		public AuditorAware<String> auditorProvider() {
			return () -> Optional.of("A user");
		}

		@Bean
		public DateTimeProvider fixedDateTimeProvider() {
			return () -> Optional.of(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		}
	}
}
