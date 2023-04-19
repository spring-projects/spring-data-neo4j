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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.executables.ExecutableResultStatement;
import org.neo4j.cypherdsl.core.executables.ExecutableStatement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.mapping.IdentitySupport;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.TestIdentitySupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class Neo4jClientIT {

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		try (
				Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()
		) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
		}
	}

	@Test // GH-2238
	void clientShouldIntegrateWithCypherDSL(@Autowired TransactionTemplate transactionTemplate,
			@Autowired Neo4jClient client,
			@Autowired BookmarkCapture bookmarkCapture) {

		Node namedAnswer = Cypher.node("TheAnswer", Cypher.mapOf("value",
				Cypher.literalOf(23).multiply(Cypher.literalOf(2)).subtract(Cypher.literalOf(4)))).named("n");
		ExecutableResultStatement statement = ExecutableStatement.makeExecutable(Cypher.create(namedAnswer)
				.returning(namedAnswer)
				.build());

		Long vanishedId = transactionTemplate.execute(transactionStatus -> {
			List<Record> records = statement.fetchWith(client.getQueryRunner());
			assertThat(records).hasSize(1)
					.first().extracting(r -> r.get("n").get("value").asLong()).isEqualTo(42L);

			transactionStatus.setRollbackOnly();
			return TestIdentitySupport.getInternalId(records.get(0).get("n").asNode());
		});

		// Make sure we actually interacted with the managed transaction (that had been rolled back)
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig())) {
			long cnt = session.run("MATCH (n) WHERE id(n) = $id RETURN count(n)",
					Collections.singletonMap("id", vanishedId)).single().get(0).asLong();
			assertThat(cnt).isEqualTo(0L);
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override // needed here because there is no implicit registration of entities upfront some methods under test
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
