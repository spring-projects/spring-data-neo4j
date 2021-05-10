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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AuditingITBase;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThing;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThingWithGeneratedId;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class AuditingIT extends AuditingITBase {

	@Autowired
	AuditingIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
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

		ImmutableAuditableThingWithGeneratedId thing = repository.findById(idOfExistingThingWithGeneratedId).get();

		thing = thing.withName("A new name");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isEqualTo(EXISTING_THING_CREATED_AT);
		assertThat(thing.getCreatedBy()).isEqualTo(EXISTING_THING_CREATED_BY);

		assertThat(thing.getModifiedAt()).isEqualTo(DEFAULT_CREATION_AND_MODIFICATION_DATE);
		assertThat(thing.getModifiedBy()).isEqualTo("A user");

		assertThat(thing.getName()).isEqualTo("A new name");

		verifyDatabase(idOfExistingThingWithGeneratedId, thing);
	}

	interface ImmutableEntityTestRepository extends Neo4jRepository<ImmutableAuditableThing, Long> {}

	interface ImmutableEntityWithGeneratedIdRepository
			extends Neo4jRepository<ImmutableAuditableThingWithGeneratedId, String> {}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableNeo4jAuditing(modifyOnCreate = false, auditorAwareRef = "auditorProvider",
			dateTimeProviderRef = "fixedDateTimeProvider")
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public AuditorAware<String> auditorProvider() {
			return () -> Optional.of("A user");
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(ImmutableAuditableThing.class.getPackage().getName());
		}

		@Bean
		public DateTimeProvider fixedDateTimeProvider() {
			return () -> Optional.of(DEFAULT_CREATION_AND_MODIFICATION_DATE);
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
