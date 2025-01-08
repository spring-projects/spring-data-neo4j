/*
 * Copyright 2011-2025 the original author or authors.
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
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.AuditingITBase;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThing;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class AuditingWithoutDatesIT extends AuditingITBase {

	@Autowired AuditingWithoutDatesIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void settingOfDatesShouldBeTurnedOff(@Autowired ImmutableEntityTestRepository repository) {

		ImmutableAuditableThing thing = new ImmutableAuditableThing("A thing");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isNull();
		assertThat(thing.getCreatedBy()).isEqualTo("A user");

		assertThat(thing.getModifiedAt()).isNull();
		assertThat(thing.getModifiedBy()).isEqualTo("A user");

		verifyDatabase(thing.getId(), thing);

		thing = thing.withName("A new name");
		thing = repository.save(thing);

		assertThat(thing.getCreatedAt()).isNull();
		assertThat(thing.getCreatedBy()).isEqualTo("A user");

		assertThat(thing.getModifiedAt()).isNull();
		assertThat(thing.getModifiedBy()).isEqualTo("A user");

		assertThat(thing.getName()).isEqualTo("A new name");

		verifyDatabase(thing.getId(), thing);
	}

	interface ImmutableEntityTestRepository extends Neo4jRepository<ImmutableAuditableThing, Long> {}

	@Configuration
	@EnableNeo4jAuditing(setDates = false, auditorAwareRef = "auditorProvider")
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(ImmutableAuditableThing.class.getPackage().getName());
		}

		@Bean
		public AuditorAware<String> auditorProvider() {
			return () -> Optional.of("A user");
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

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
