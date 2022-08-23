/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2583;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to ensure that also backward updated through the result set work without
 * running into a StackOverflowError.
 */
@Neo4jIntegrationTest
public class GH2583IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run("CREATE (n:GH2583Node)-[:LINKED]->(m:GH2583Node)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)" +
					"-[:LINKED]->(n)-[:LINKED]->(m)-[:LINKED]->(n)-[:LINKED]->(m)").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void mapStandardCustomQueryWithLotsOfRelationshipsProperly(@Autowired GH2583Repository repository) {
		Page<GH2583Node> nodePage = repository.getNodesByCustomQuery(PageRequest.of(0, 300));

		List<GH2583Node> nodes = nodePage.getContent();
		assertThat(nodes).hasSize(2);
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

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(GH2583Node.class.getPackage().getName());
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
