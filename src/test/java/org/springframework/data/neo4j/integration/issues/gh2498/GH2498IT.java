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
package org.springframework.data.neo4j.integration.issues.gh2498;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class GH2498IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.writeTransaction(tx -> tx.run("MATCH (n:DomainModel) DETACH DELETE n").consume());
			session.writeTransaction(tx -> tx.run(
							"UNWIND ['A', 'B', 'C'] AS name WITH name CREATE (n:DomainModel {id: randomUUID(), name: name})")
					.consume());
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2498
	void cypherdslConditionExecutorShouldWorkWithAnonParameters(@Autowired DomainModelRepository repository) {

		Node node = Cypher.node("DomainModel").named("n");
		Property name = node.property("name");
		Parameter<List<String>> parameters = Cypher.anonParameter(Arrays.asList("A", "C"));
		Condition in = name.in(parameters);
		Collection<DomainModel> result = repository.findAll(in, Cypher.sort(name).descending());
		Assertions.assertThat(result).hasSize(2)
				.map(DomainModel::getName)
				.containsExactly("C", "A");
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories
	static class Config extends AbstractNeo4jConfig {

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
	}
}
