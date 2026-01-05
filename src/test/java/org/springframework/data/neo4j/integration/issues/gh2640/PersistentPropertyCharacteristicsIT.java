/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2640;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.PersistentPropertyCharacteristics;
import org.springframework.data.neo4j.core.mapping.PersistentPropertyCharacteristicsProvider;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class PersistentPropertyCharacteristicsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
		// GH-2640
	void implicitTransientPropertiesShouldNotBeWritten(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture, @Autowired Neo4jTemplate template) {

		SomeWithImplicitTransientProperties1 node1 = template.save(new SomeWithImplicitTransientProperties1("the name", 1.23));
		SomeWithImplicitTransientProperties2 node2 = template.save(new SomeWithImplicitTransientProperties2(47.11));

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			List<Record> records = session.run("MATCH (n) WHERE n.id IN $ids RETURN n", Values.parameters("ids", Arrays.asList(node1.id.toString(), node2.id.toString()))).list();
			assertThat(records)
					.hasSize(2)
					.noneMatch(r -> {
						Map<String, Object> properties = r.get("n").asNode().asMap();
						return properties.containsKey("foobar") || properties.containsKey("bazbar");
					});
		}
	}

	@Node
	static class SomeWithImplicitTransientProperties1 {

		@Id
		@GeneratedValue
		private UUID id;

		private String name;

		private Double foobar;

		SomeWithImplicitTransientProperties1(String name, Double foobar) {
			this.name = name;
			this.foobar = foobar;
		}
	}

	@Node
	static class SomeWithImplicitTransientProperties2 {

		@Id
		@GeneratedValue
		private UUID id;

		private Double bazbar;

		SomeWithImplicitTransientProperties2(Double bazbar) {
			this.bazbar = bazbar;
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
		public PersistentPropertyCharacteristicsProvider persistentPropertyCharacteristicsProvider() {
			return (property, owner) -> {

				if (property.getType().equals(Double.class)) {
					return PersistentPropertyCharacteristics.treatAsTransient();
				}

				return PersistentPropertyCharacteristics.useDefaults();
			};
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
