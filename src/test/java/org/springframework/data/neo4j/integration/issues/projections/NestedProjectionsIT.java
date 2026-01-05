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
package org.springframework.data.neo4j.integration.issues.projections;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.issues.projections.model.SourceNodeA;
import org.springframework.data.neo4j.integration.issues.projections.projection.SourceNodeAProjection;
import org.springframework.data.neo4j.integration.issues.projections.repository.SourceNodeARepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class NestedProjectionsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run(
					"CREATE (l:SourceNodeA {id: 'L-l1', version: 1})-[:A_TO_CENTRAL]->(e:CentralNode {id: 'E-l1', version: 1})<-[:B_TO_CENTRAL]-(c:SourceNodeB {id: 'C-l1', version: 1}) RETURN id(l)")
				.consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@BeforeEach
	void clearChangedProperties(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = driver.session()) {
			session.run("MATCH (n:SourceNodeA) SET n.value = null").consume();
			session.run("MATCH (n:CentralNode) SET n.name = null").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@RepeatedTest(20)
	// GH-2581
	void excludedHopMustNotVanish(@Autowired SourceNodeARepository repository) {

		Optional<SourceNodeA> optionalSourceNode = repository.findById("L-l1");
		assertThat(optionalSourceNode).isPresent();

		SourceNodeA toUpdate = optionalSourceNode.get();
		toUpdate.setValue("newValue");
		toUpdate.getCentralNode().setName("whatever");
		SourceNodeAProjection projectedSourceNode = repository.saveWithProjection(toUpdate);

		SourceNodeA updatedNode = repository.findById("L-l1").orElseThrow(IllegalStateException::new);

		assertThat(updatedNode.getCentralNode()).isNotNull();
		assertThat(updatedNode.getCentralNode().getSourceNodeB()).isNotNull();

		assertThat(projectedSourceNode.getCentralNode().getName()).isEqualTo("whatever");
		assertThat(updatedNode.getCentralNode().getName()).isEqualTo(projectedSourceNode.getCentralNode().getName());

		assertThat(projectedSourceNode.getValue()).isEqualTo("newValue");
		assertThat(updatedNode.getValue()).isEqualTo(projectedSourceNode.getValue());
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@ComponentScan
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(NestedProjectionsIT.class.getPackage().getName());
		}

		@Bean
		@Override
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
