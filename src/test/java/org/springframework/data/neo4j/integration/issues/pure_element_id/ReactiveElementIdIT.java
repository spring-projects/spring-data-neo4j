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
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.lang.NonNull;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Assertions that no {@code id()} calls are generated when no deprecated id types are present.
 * This test deliberately uses blocking calls into reactor because it's really not about the reactive flows but about
 * catching all the code paths that might interact with ids on the reactive side of things. Yes, Reactors testing tools
 * are known.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@ExtendWith(LogbackCapturingExtension.class)
public class ReactiveElementIdIT extends AbstractElementIdTestBase {


	interface Repo1 extends ReactiveNeo4jRepository<NodeWithGeneratedId1, String> {
	}

	interface Repo2 extends ReactiveNeo4jRepository<NodeWithGeneratedId2, String> {
	}

	interface Repo3 extends ReactiveNeo4jRepository<NodeWithGeneratedId3, String> {
	}

	interface Repo4 extends ReactiveNeo4jRepository<NodeWithGeneratedId4, String> {
	}


	@Test
	void simpleNodeCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var node = repo1.save(new NodeWithGeneratedId1("from-sdn-repo")).block();
		assertThat(node).isNotNull();
		assertThat(node.getId())
				.matches(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void simpleNodeAllCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var nodes = repo1.saveAll(List.of(new NodeWithGeneratedId1("from-sdn-repo"))).collectList().block();
		assertThat(nodes).isNotEmpty()
				.extracting(NodeWithGeneratedId1::getId)
				.allMatch(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findByIdMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String id;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode().elementId();
		}

		var optionalNode = Optional.ofNullable(repo1.findById(id).block());
		assertThat(optionalNode).map(NodeWithGeneratedId1::getValue)
				.hasValue("whatever");
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findAllMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n").single().get("n").asNode().elementId();
		}

		var nodes = repo1.findAll().collectList().block();
		assertThat(nodes).isNotEmpty()
				.extracting(NodeWithGeneratedId1::getId)
				.allMatch(validIdForCurrentNeo4j());
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

		node = repo1.save(node).block();
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

		var nodes = repo1.saveAll(List.of(node)).collectList().block();
		assertThat(nodes).isNotEmpty()
				.extracting(NodeWithGeneratedId1::getId)
				.allMatch(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void nodeAndRelationshipsWithoutPropsAndIdsMustNotUseIdFunctionWhileCreating(LogbackCapture logbackCapture, @Autowired Repo2 repo2, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId2("owner");
		owner.setRelatedNodes(List.of(new NodeWithGeneratedId1("child1"), new NodeWithGeneratedId1("child2")));
		owner = repo2.save(owner).block();

		assertThat(owner).isNotNull();
		assertThat(owner.getId()).isNotNull();
		assertThat(owner.getRelatedNodes())
				.allSatisfy(owned -> assertThat(owned.getId()).matches(validIdForCurrentNeo4j()));
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

		var owner = repo2.findById(ownerId).block();
		assertThat(owner).isNotNull();
		assertThat(owner.getRelatedNodes())
				.hasSize(1)
				.first()
				.extracting(NodeWithGeneratedId1::getId)
				.isEqualTo(ownedId);


		owner.getRelatedNodes().get(0).setValue("owned_changed");
		owner.setValue("owner_changed");

		repo2.save(owner).block();

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run(adaptQueryTo44IfNecessary("""
							MATCH (n:NodeWithGeneratedId2 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: $v2})
							WHERE elementId(n) = $id1 AND elementId(m) = $id2
							RETURN count(*)"""),
					Map.of("v1", "owner_changed", "v2", "owned_changed", "id1", ownerId, "id2", ownedId)).single().get(0).asLong();
			assertThat(count).isOne();
		}
	}

	@Test
	void relsWithPropOnCreation(LogbackCapture logbackCapture, @Autowired Repo3 repo3, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId3("owner");
		var target1 = new NodeWithGeneratedId1("target1");
		var target2 = new NodeWithGeneratedId1("target2");

		owner.setRelatedNodes(
				List.of(
						new RelWithProps(target1, "vr1"),
						new RelWithProps(target2, "vr2"))
		);

		owner = repo3.save(owner).block();
		assertThat(owner).isNotNull();
		assertThat(owner.getId()).matches(validIdForCurrentNeo4j());
		assertThat(owner.getRelatedNodes())
				.hasSize(2)
				.allSatisfy(r -> assertThat(r.getTarget().getId()).isNotNull())
				.extracting(RelWithProps::getRelValue)
				.containsExactlyInAnyOrder("vr1", "vr2");

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run(adaptQueryTo44IfNecessary("""
							MATCH (n:NodeWithGeneratedId3 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1)
							WHERE elementId(n) = $id1
							  AND r.relValue IN $rv
							RETURN count(*)"""),
					Map.of("v1", "owner", "id1", owner.getId(), "rv", List.of("vr1", "vr2"))).single().get(0).asLong();
			assertThat(count).isEqualTo(2L);
		}
	}

	@Test
	void relsWithPropOnUpdate(LogbackCapture logbackCapture, @Autowired Repo3 repo3, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			ownerId = session.run(adaptQueryTo44IfNecessary("""
					CREATE (n:NodeWithGeneratedId3 {value: 'owner'}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: 'owned'})
					RETURN elementId(n)""")
			).single().get(0).asString();
		}

		var owner = repo3.findById(ownerId).block();
		assertThat(owner).isNotNull();
		owner.setValue("owner_updated");
		var rel = owner.getRelatedNodes().get(0);
		rel.setRelValue("whatever");
		rel.getTarget().setValue("owned_updated");

		repo3.save(owner).block();

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run(adaptQueryTo44IfNecessary("""
							MATCH (n:NodeWithGeneratedId3 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: $v2})
							WHERE elementId(n) = $id1
							  AND r.relValue IN $rv
							RETURN count(*)"""),
					Map.of("v1", "owner_updated", "v2", "owned_updated", "id1", owner.getId(), "rv", List.of("whatever"))).single().get(0).asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	@Test
	void relsWithsCyclesOnCreation(LogbackCapture logbackCapture, @Autowired Repo4 repo4, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId4("owner");
		var intermediate = new NodeWithGeneratedId4.Intermediate();
		intermediate.setEnd(new NodeWithGeneratedId4("end"));
		owner.setIntermediate(intermediate);

		owner = repo4.save(owner).block();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		var validIdForCurrentNeo4j = validIdForCurrentNeo4j();
		assertThat(owner).isNotNull();
		assertThat(owner.getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getEnd().getId()).matches(validIdForCurrentNeo4j);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var query = """
					MATCH (n:NodeWithGeneratedId4 {value: $v1}) -[r:INTERMEDIATE]-> (i:Intermediate) -[:END]-> (e:NodeWithGeneratedId4 {value: $v2})
					WHERE elementId(n) = $id1
					  AND elementId(i) = $id2
					  AND elementId(e) = $id3
					RETURN count(*)""";
			var count = session.run(adaptQueryTo44IfNecessary(query),
							Map.of("v1", "owner", "v2", "end", "id1", owner.getId(), "id2", owner.getIntermediate().getId(), "id3", owner.getIntermediate().getEnd().getId()))
					.single().get(0).asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	@Test
	void relsWithsCyclesOnUpdate(LogbackCapture logbackCapture, @Autowired Repo4 repo4, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			ownerId = session.run(adaptQueryTo44IfNecessary("""
							CREATE (n:NodeWithGeneratedId4 {value: 'a'}) -[r:INTERMEDIATE]-> (i:Intermediate) -[:END]-> (e:NodeWithGeneratedId4 {value: 'b'})
							RETURN elementId(n)"""))
					.single().get(0).asString();
		}

		var owner = repo4.findAllById(List.of(ownerId)).blockFirst();
		assertThat(owner).isNotNull();
		owner.setValue("owner");
		owner.getIntermediate().getEnd().setValue("end");

		owner = repo4.save(owner).block();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		var validIdForCurrentNeo4j = validIdForCurrentNeo4j();
		assertThat(owner).isNotNull();
		assertThat(owner.getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getEnd().getId()).matches(validIdForCurrentNeo4j);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run(adaptQueryTo44IfNecessary("""
									MATCH (n:NodeWithGeneratedId4 {value: $v1}) -[r:INTERMEDIATE]-> (i:Intermediate) -[:END]-> (e:NodeWithGeneratedId4 {value: $v2})
									WHERE elementId(n) = $id1
									  AND elementId(i) = $id2
									  AND elementId(e) = $id3
									RETURN count(*)"""),
							Map.of("v1", "owner", "v2", "end", "id1", owner.getId(), "id2", owner.getIntermediate().getId(), "id3", owner.getIntermediate().getEnd().getId()))
					.single().get(0).asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	private static void assertThatLogMessageDoNotIndicateIDUsage(LogbackCapture logbackCapture) {
		assertThat(logbackCapture.getFormattedMessages())
				.noneMatch(s -> s.contains("Neo.ClientNotification.Statement.FeatureDeprecationWarning") ||
						s.contains("The query used a deprecated function. ('id' is no longer supported)"));
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		@NonNull
		public ReactiveTransactionManager reactiveTransactionManager(@NonNull Driver driver, @NonNull ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider,
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
