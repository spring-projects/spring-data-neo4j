/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.reactive;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.cypherdsl.core.Cypher.*;
import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.core.ReactiveNeo4jOperations;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.integration.shared.ThingWithGeneratedId;
import org.neo4j.springframework.data.test.Neo4jIntegrationTest;
import org.neo4j.springframework.data.test.Neo4jExtension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@Tag(NEEDS_REACTIVE_SUPPORT)
class ReactiveNeo4jOperationsIT {
	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final ReactiveNeo4jOperations neo4jOperations;

	private Long person1Id;
	private Long person2Id;

	@Autowired ReactiveNeo4jOperationsIT(Driver driver, ReactiveNeo4jOperations neo4jOperations) {
		this.driver = driver;
		this.neo4jOperations = neo4jOperations;
	}

	/**
	 * Shall be configured by test making use of database selection, so that the verification queries run in the correct database.
	 *
	 * @return The session config used for verification methods.
	 */
	SessionConfig getSessionConfig() {

		return SessionConfig.defaultConfig();
	}

	@BeforeEach
	void setupData() {

		Transaction transaction = driver.session(getSessionConfig()).beginTransaction();
		transaction.run("MATCH (n) detach delete n");

		person1Id = transaction.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n)",
			Values.parameters("name", TEST_PERSON1_NAME)
		).next().get(0).asLong();
		person2Id = transaction.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n)",
			Values.parameters("name", TEST_PERSON2_NAME)
		).next().get(0).asLong();

		transaction.commit();
		transaction.close();
	}

	@Test
	void count() {
		StepVerifier.create(neo4jOperations.count(PersonWithAllConstructor.class))
			.assertNext(count -> assertThat(count).isEqualTo(2))
			.verifyComplete();
	}

	@Test
	void countWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(Functions.count(node)).build();

		StepVerifier.create(neo4jOperations.count(statement))
			.assertNext(count -> assertThat(count).isEqualTo(2))
			.verifyComplete();
	}

	@Test
	void countWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(parameter("name")))
			.returning(Functions.count(node)).build();

		StepVerifier.create(neo4jOperations.count(statement, singletonMap("name", TEST_PERSON1_NAME)))
			.assertNext(count -> assertThat(count).isEqualTo(1))
			.verifyComplete();
	}

	@Test
	void countWithCypherQuery() {

		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return count(p)";

		StepVerifier.create(neo4jOperations.count(cypherQuery))
			.assertNext(count -> assertThat(count).isEqualTo(2))
			.verifyComplete();
	}

	@Test
	void countWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return count(p)";

		StepVerifier.create(neo4jOperations.count(cypherQuery, singletonMap("name", TEST_PERSON1_NAME)))
			.assertNext(count -> assertThat(count).isEqualTo(1))
			.verifyComplete();
	}

	@Test
	void findAll() {
		StepVerifier.create(neo4jOperations.findAll(PersonWithAllConstructor.class))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void findAllWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(node).build();

		StepVerifier.create(neo4jOperations.findAll(statement, PersonWithAllConstructor.class))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void findAllWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(parameter("name")))
			.returning(node).build();

		StepVerifier.create(neo4jOperations.findAll(statement,
			Collections.singletonMap("name", TEST_PERSON1_NAME),
			PersonWithAllConstructor.class))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findOneWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(parameter("name")))
			.returning(node).build();

		StepVerifier.create(neo4jOperations.findOne(statement,
			Collections.singletonMap("name", TEST_PERSON1_NAME),
			PersonWithAllConstructor.class))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findAllWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return p";

		StepVerifier.create(neo4jOperations.findAll(cypherQuery, PersonWithAllConstructor.class))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void findAllWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		StepVerifier.create(neo4jOperations.findAll(cypherQuery,
			Collections.singletonMap("name", TEST_PERSON1_NAME),
			PersonWithAllConstructor.class))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findOneWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		StepVerifier.create(neo4jOperations.findOne(cypherQuery,
			Collections.singletonMap("name", TEST_PERSON1_NAME),
			PersonWithAllConstructor.class))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findById() {
		StepVerifier.create(neo4jOperations.findById(person1Id, PersonWithAllConstructor.class))
			.expectNextCount(1)
			.verifyComplete();
	}

	@Test
	void findAllById() {
		StepVerifier.create(neo4jOperations.findAllById(Arrays.asList(person1Id, person2Id),
			PersonWithAllConstructor.class))
			.expectNextCount(2)
			.verifyComplete();
	}

	@Test
	void save() {
		StepVerifier.create(neo4jOperations.save(new ThingWithGeneratedId("testThing")))
			.expectNextCount(1)
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Result result = session.run("MATCH (t:ThingWithGeneratedId{name: 'testThing'}) return t");
			Value resultValue = result.single().get("t");
			assertThat(resultValue).isNotNull();
			assertThat(resultValue.asMap().get("name")).isEqualTo("testThing");
		}
	}

	@Test
	void saveAll() {
		String thing1Name = "testThing1";
		String thing2Name = "testThing2";
		ThingWithGeneratedId thing1 = new ThingWithGeneratedId(thing1Name);
		ThingWithGeneratedId thing2 = new ThingWithGeneratedId(thing2Name);

		StepVerifier.create(neo4jOperations.saveAll(Arrays.asList(thing1, thing2)))
			.expectNextCount(2)
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("name1", thing1Name);
			paramMap.put("name2", thing2Name);

			Result result = session.run(
				"MATCH (t:ThingWithGeneratedId) WHERE t.name = $name1 or t.name = $name2 return t",
				paramMap);
			List<Record> resultValues = result.list();
			assertThat(resultValues).hasSize(2);
			assertThat(resultValues).allMatch(record ->
				record.asMap(Function.identity()).get("t").get("name").asString().startsWith("testThing"));
		}
	}

	@Test
	void deleteById() {
		StepVerifier.create(neo4jOperations.deleteById(person1Id, PersonWithAllConstructor.class))
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(1);
		}
	}

	@Test
	void deleteAllById() {
		StepVerifier
			.create(neo4jOperations.deleteAllById(Arrays.asList(person1Id, person2Id), PersonWithAllConstructor.class))
			.verifyComplete();

		try (Session session = driver.session(getSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override // needed here because there is no implicit registration of entities upfront some methods under test
		protected Collection<String> getMappingBasePackages() {
			return singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}
	}
}
