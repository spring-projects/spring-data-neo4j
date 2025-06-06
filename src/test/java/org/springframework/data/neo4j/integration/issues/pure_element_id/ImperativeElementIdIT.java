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
package org.springframework.data.neo4j.integration.issues.pure_element_id;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.LogbackCapture;
import org.springframework.data.neo4j.test.LogbackCapturingExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assertions that no {@code id()} calls are generated when no deprecated id types are
 * present
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@ExtendWith(LogbackCapturingExtension.class)
public class ImperativeElementIdIT extends AbstractElementIdTestBase {

	@Test
	void dontCallIdForDerivedQueriesWithInClause(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var node = repo1.save(new NodeWithGeneratedId1("testValue"));
		String id = node.getId();

		repo1.findByIdIn(List.of(id));

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void dontCallIdForDerivedQueriesWithRelatedInClause(LogbackCapture logbackCapture, @Autowired Repo2 repo2) {
		var node1 = new NodeWithGeneratedId1("testValue");
		var node2 = new NodeWithGeneratedId2("testValue");
		node2.setRelatedNodes(List.of(node1));
		var savedNode2 = repo2.save(node2);

		String id = savedNode2.getRelatedNodes().get(0).getId();

		repo2.findByRelatedNodesIdIn(List.of(id));

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void simpleNodeCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var node = repo1.save(new NodeWithGeneratedId1("from-sdn-repo"));
		assertThat(node.getId()).matches(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void simpleNodeAllCreationShouldFillIdAndNotUseIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1) {

		var nodes = repo1.saveAll(List.of(new NodeWithGeneratedId1("from-sdn-repo")));
		assertThat(nodes).isNotEmpty().extracting(NodeWithGeneratedId1::getId).allMatch(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findByIdMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String id;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			id = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n")
				.single()
				.get("n")
				.asNode()
				.elementId();
		}

		var optionalNode = repo1.findById(id);
		assertThat(optionalNode).map(NodeWithGeneratedId1::getValue).hasValue("whatever");
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void findAllMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n")
				.single()
				.get("n")
				.asNode()
				.elementId();
		}

		var nodes = repo1.findAll();
		assertThat(nodes).isNotEmpty().extracting(NodeWithGeneratedId1::getId).allMatch(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void updateMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		NodeWithGeneratedId1 node;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var dbNode = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n")
				.single()
				.get("n")
				.asNode();
			node = new NodeWithGeneratedId1(dbNode.get("value").asString() + "_edited");
			node.setId(dbNode.elementId());
		}

		node = repo1.save(node);
		assertThat(node).extracting(NodeWithGeneratedId1::getValue).isEqualTo("whatever_edited");
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void updateAllMustNotCallIdFunction(LogbackCapture logbackCapture, @Autowired Repo1 repo1,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		NodeWithGeneratedId1 node;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var dbNode = session.run("CREATE (n:NodeWithGeneratedId1 {value: 'whatever'}) RETURN n")
				.single()
				.get("n")
				.asNode();
			node = new NodeWithGeneratedId1(dbNode.get("value").asString() + "_edited");
			node.setId(dbNode.elementId());
		}

		var nodes = repo1.saveAll(List.of(node));
		assertThat(nodes).isNotEmpty().extracting(NodeWithGeneratedId1::getId).allMatch(validIdForCurrentNeo4j());
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void nodeAndRelationshipsWithoutPropsAndIdsMustNotUseIdFunctionWhileCreating(LogbackCapture logbackCapture,
			@Autowired Repo2 repo2, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId2("owner");
		owner.setRelatedNodes(List.of(new NodeWithGeneratedId1("child1"), new NodeWithGeneratedId1("child2")));
		owner = repo2.save(owner);

		assertThat(owner.getId()).isNotNull();
		assertThat(owner.getRelatedNodes())
			.allSatisfy(owned -> assertThat(owned.getId()).matches(validIdForCurrentNeo4j()));
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	@Test
	void nodeAndRelationshipsWithoutPropsAndIdsMustNotUseIdFunctionWhileUpdating(LogbackCapture logbackCapture,
			@Autowired Repo2 repo2, @Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		String ownedId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var row = session.run(
					"CREATE (n:NodeWithGeneratedId2 {value: 'owner'}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value:'owned'}) RETURN *")
				.single();
			ownerId = row.get("n").asNode().elementId();
			ownedId = row.get("m").asNode().elementId();
		}

		var owner = repo2.findById(ownerId).orElseThrow();
		assertThat(owner.getRelatedNodes()).hasSize(1)
			.first()
			.extracting(NodeWithGeneratedId1::getId)
			.isEqualTo(ownedId);

		owner.getRelatedNodes().get(0).setValue("owned_changed");
		owner.setValue("owner_changed");

		repo2.save(owner);

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session
				.run(adaptQueryTo44IfNecessary(
						"""
								MATCH (n:NodeWithGeneratedId2 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: $v2})
								WHERE elementId(n) = $id1 AND elementId(m) = $id2
								RETURN count(*)"""),
						Map.of("v1", "owner_changed", "v2", "owned_changed", "id1", ownerId, "id2", ownedId))
				.single()
				.get(0)
				.asLong();
			assertThat(count).isOne();
		}
	}

	@Test
	void relsWithPropOnCreation(LogbackCapture logbackCapture, @Autowired Repo3 repo3,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId3("owner");
		var target1 = new NodeWithGeneratedId1("target1");
		var target2 = new NodeWithGeneratedId1("target2");

		owner.setRelatedNodes(List.of(new RelWithProps(target1, "vr1"), new RelWithProps(target2, "vr2")));

		owner = repo3.save(owner);
		assertThat(owner.getId()).matches(validIdForCurrentNeo4j());
		assertThat(owner.getRelatedNodes()).hasSize(2)
			.allSatisfy(r -> assertThat(r.getTarget().getId()).isNotNull())
			.extracting(RelWithProps::getRelValue)
			.containsExactlyInAnyOrder("vr1", "vr2");

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session.run(adaptQueryTo44IfNecessary("""
					MATCH (n:NodeWithGeneratedId3 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1)
					WHERE elementId(n) = $id1
					  AND r.relValue IN $rv
					RETURN count(*)"""), Map.of("v1", "owner", "id1", owner.getId(), "rv", List.of("vr1", "vr2")))
				.single()
				.get(0)
				.asLong();
			assertThat(count).isEqualTo(2L);
		}
	}

	@Test
	void relsWithPropOnUpdate(LogbackCapture logbackCapture, @Autowired Repo3 repo3,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			ownerId = session
				.run(adaptQueryTo44IfNecessary(
						"""
								CREATE (n:NodeWithGeneratedId3 {value: 'owner'}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: 'owned'})
								RETURN elementId(n)"""))
				.single()
				.get(0)
				.asString();
		}

		var owner = repo3.findById(ownerId).orElseThrow();
		owner.setValue("owner_updated");
		var rel = owner.getRelatedNodes().get(0);
		rel.setRelValue("whatever");
		rel.getTarget().setValue("owned_updated");

		repo3.save(owner);

		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session
				.run(adaptQueryTo44IfNecessary(
						"""
								MATCH (n:NodeWithGeneratedId3 {value: $v1}) -[r:RELATED_NODES] -> (m:NodeWithGeneratedId1 {value: $v2})
								WHERE elementId(n) = $id1
								  AND r.relValue IN $rv
								RETURN count(*)"""),
						Map.of("v1", "owner_updated", "v2", "owned_updated", "id1", owner.getId(), "rv",
								List.of("whatever")))
				.single()
				.get(0)
				.asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	@Test
	void relsWithsCyclesOnCreation(LogbackCapture logbackCapture, @Autowired Repo4 repo4,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		var owner = new NodeWithGeneratedId4("owner");
		var intermediate = new NodeWithGeneratedId4.Intermediate();
		intermediate.setEnd(new NodeWithGeneratedId4("end"));
		owner.setIntermediate(intermediate);

		owner = repo4.save(owner);
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		var validIdForCurrentNeo4j = validIdForCurrentNeo4j();
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
			var count = session
				.run(adaptQueryTo44IfNecessary(query), Map.of("v1", "owner", "v2", "end", "id1", owner.getId(), "id2",
						owner.getIntermediate().getId(), "id3", owner.getIntermediate().getEnd().getId()))
				.single()
				.get(0)
				.asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	@Test
	void relsWithsCyclesOnUpdate(LogbackCapture logbackCapture, @Autowired Repo4 repo4,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Driver driver) {

		String ownerId;
		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			ownerId = session
				.run(adaptQueryTo44IfNecessary(
						"""
								CREATE (n:NodeWithGeneratedId4 {value: 'a'}) -[r:INTERMEDIATE]-> (i:Intermediate) -[:END]-> (e:NodeWithGeneratedId4 {value: 'b'})
								RETURN elementId(n)"""))
				.single()
				.get(0)
				.asString();
		}

		var owner = repo4.findAllById(List.of(ownerId)).get(0);
		owner.setValue("owner");
		owner.getIntermediate().getEnd().setValue("end");

		owner = repo4.save(owner);
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);

		var validIdForCurrentNeo4j = validIdForCurrentNeo4j();
		assertThat(owner.getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getId()).matches(validIdForCurrentNeo4j);
		assertThat(owner.getIntermediate().getEnd().getId()).matches(validIdForCurrentNeo4j);

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			var count = session
				.run(adaptQueryTo44IfNecessary(
						"""
								MATCH (n:NodeWithGeneratedId4 {value: $v1}) -[r:INTERMEDIATE]-> (i:Intermediate) -[:END]-> (e:NodeWithGeneratedId4 {value: $v2})
								WHERE elementId(n) = $id1
								  AND elementId(i) = $id2
								  AND elementId(e) = $id3
								RETURN count(*)"""),
						Map.of("v1", "owner", "v2", "end", "id1", owner.getId(), "id2", owner.getIntermediate().getId(),
								"id3", owner.getIntermediate().getEnd().getId()))
				.single()
				.get(0)
				.asLong();
			assertThat(count).isEqualTo(1L);
		}
	}

	@Test
	@Tag("GH-2927")
	void fluentOpsMustUseCypherDSLConfig(LogbackCapture logbackCapture, @Autowired Driver driver,
			@Autowired BookmarkCapture bookmarkCapture, @Autowired Neo4jTemplate neo4jTemplate) {

		try (var session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MERGE (n:" + Thing.THING_LABEL + "{foo: 'bar'})").consume();
		}

		var thingNode = Cypher.node(Thing.THING_LABEL);
		var cypherStatement = Statement.builder()
			.match(thingNode)
			.where(Cypher.elementId(thingNode).eq(Cypher.literalOf("test")))
			.returning(thingNode)
			.build();
		neo4jTemplate.find(Thing.class).matching(cypherStatement).one();
		assertThatLogMessageDoNotIndicateIDUsage(logbackCapture);
	}

	interface Repo1 extends Neo4jRepository<NodeWithGeneratedId1, String> {

		NodeWithGeneratedId1 findByIdIn(List<String> ids);

	}

	interface Repo2 extends Neo4jRepository<NodeWithGeneratedId2, String> {

		NodeWithGeneratedId2 findByRelatedNodesIdIn(List<String> ids);

	}

	interface Repo3 extends Neo4jRepository<NodeWithGeneratedId3, String> {

	}

	interface Repo4 extends Neo4jRepository<NodeWithGeneratedId4, String> {

	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
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
