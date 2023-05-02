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
package org.springframework.data.neo4j.integration.issues.pure_element_id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Assertions that no {@code id()} calls are generated when no deprecated id types are present
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@ExtendWith(LogbackCapturingExtension.class)
public class ImperativeElementIdIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	interface Repo1 extends Neo4jRepository<NodeWithGeneratedId1, String> {
	}

	interface Repo2 extends Neo4jRepository<NodeWithGeneratedId2, String> {
	}

	@Test
	void simpleNodeCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var node = repo1.save(new NodeWithGeneratedId1("from-sdn-repo"));
		assertThat(node.getId())
				.isNotNull();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void simpleNodeAllCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var nodes = repo1.saveAll(List.of(new NodeWithGeneratedId1("from-sdn-repo")));
		assertThat(nodes).isNotEmpty()
				.noneMatch(v -> v.getId() == null)
				.isNotNull();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findByIdMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String id;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode().elementId();
		}

		var optionalNode = repo1.findById(id);
		assertThat(optionalNode).map(NodeWithGeneratedId1::getValue)
				.hasValue("whatever");
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findAllMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode().elementId();
		}

		var nodes = repo1.findAll();
		assertThat(nodes).isNotEmpty()
				.noneMatch(v -> v.getId() == null)
				.isNotNull();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void updateMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		NodeWithGeneratedId1 node;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var dbNode = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode();
			node = new NodeWithGeneratedId1(dbNode.get("value").asString() + "_edited");
			node.setId(dbNode.elementId());
		}

		node = repo1.save(node);
		assertThat(node).extracting(NodeWithGeneratedId1::getValue)
				.isEqualTo("whatever_edited");
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void updateAllMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		NodeWithGeneratedId1 node;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var dbNode = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode();
			node = new NodeWithGeneratedId1(dbNode.get("value").asString() + "_edited");
			node.setId(dbNode.elementId());
		}

		var nodes = repo1.saveAll(List.of(node));
		assertThat(nodes).isNotEmpty()
				.noneMatch(v -> v.getId() == null)
				.isNotNull();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void nodeAndRelationshipsWithoutPropsAndIdsMustNotUseIdFunctionWhileCreating(LogbackCapture logbackCapture, @Autowired Repo2 repo2, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId2("owner");
		owner.setRelatedNodes(List.of(new NodeWithGeneratedId1("child1"), new NodeWithGeneratedId1("child2")));
		owner = repo2.save(owner);

		assertThat(owner.getId()).isNotNull();
		assertThat(owner.getRelatedNodes())
				.allSatisfy(owned -> assertThat(owned.getId()).isNotNull());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void nodeAndRelationshipsWithoutPropsAndIdsMustNotUseIdFunctionWhileUpdating(LogbackCapture logbackCapture, @Autowired Repo2 repo2, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		String ownedId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var row = session.run("CREATE (n:NodeWithGeneratedId2 {value: 'owner'}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value:'owned'}) RETURN *").single();
			ownerId = row.get("n").asNode().elementId();
			ownedId = row.get("m").asNode().elementId();
		}

		var owner = repo2.findById(ownerId).orElseThrow();
		assertThat(owner.getRelatedNodes())
				.hasSize(1)
				.first()
				.extracting(NodeWithGeneratedId1::getId)
				.isEqualTo(ownedId);


		owner.getRelatedNodes().get(0).setValue("owned_changed");
		owner.setValue("owner_changed");

		repo2.save(owner);

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run("""
							MATCH (n:NodeWithGeneratedId2 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: $v2})
							WHERE elementId(n) = $id1 AND elementId(m) = $id2
							RETURN count(*)""",
					Map.of("v1", "owner_changed", "v2", "owned_changed", "id1", ownerId, "id2", ownedId)).single().get(0).asLong();
			assertThat(count).isOne();
		}
	}

	// TODO rels with cycles
	// TODO rels withs props

	private static void assertThatLogMessageDoNotIndicateIDUsage(LogbackCapture logbackCapture) {
		assertThat(logbackCapture.getFormattedMessages())
				.noneMatch(s -> s.contains("Neo.ClientNotification.Statement.FeatureDeprecationWarning") ||
						s.contains("The query used a deprecated function. ('id' is no longer supported)"));
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
		@NonNull
		public PlatformTransactionManager transactionManager(@NonNull Driver driver, @NonNull DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		@NonNull
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
