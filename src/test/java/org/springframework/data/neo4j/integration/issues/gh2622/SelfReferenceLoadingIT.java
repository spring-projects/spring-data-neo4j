/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2622;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
class SelfReferenceLoadingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2622
	void throwCyclicMappingDependencyExceptionOnSelfReference(@Autowired GH2622Repository repository) {
		MePointingTowardsMe entity = new MePointingTowardsMe("me", new ArrayList<>());
		entity.others.add(entity);
		repository.save(entity);

		assertThatExceptionOfType(MappingException.class).isThrownBy(repository::findAll)
				.withRootCauseInstanceOf(MappingException.class)
				.extracting(Throwable::getCause, as(InstanceOfAssertFactories.THROWABLE))
				.hasMessageContaining("has a logical cyclic mapping dependency");

	}

	interface GH2622Repository extends Neo4jRepository<MePointingTowardsMe, Long> {
	}

	@Node
	static class MePointingTowardsMe {

		@Id
		@GeneratedValue
		Long id;

		final String name;

		@Relationship
		final List<MePointingTowardsMe> others;

		MePointingTowardsMe(String name, List<MePointingTowardsMe> others) {
			this.name = name;
			this.others = others;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MePointingTowardsMe that = (MePointingTowardsMe) o;
			return name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
