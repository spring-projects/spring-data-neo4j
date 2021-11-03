/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

import lombok.Data;
import reactor.core.publisher.Flux;
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
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.issues.gh2415.BaseNodeEntity;
import org.springframework.data.neo4j.integration.issues.gh2415.NodeEntity;
import org.springframework.data.neo4j.integration.issues.gh2415.NodeWithDefinedCredentials;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAssignedId;
import org.springframework.data.neo4j.integration.shared.common.ThingWithGeneratedId;
import org.springframework.data.neo4j.repository.query.QueryFragmentsAndParameters;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveNeo4jTemplateIT {
	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final ReactiveNeo4jTemplate neo4jTemplate;

	private Long person1Id;
	private Long person2Id;
	private Long simonsId;
	private Long nullNullSchneider;

	@Autowired ReactiveNeo4jTemplateIT(Driver driver, ReactiveNeo4jTemplate neo4jTemplate) {
		this.driver = driver;
		this.neo4jTemplate = neo4jTemplate;
	}

	@BeforeEach
	void setupData(@Autowired BookmarkCapture bookmarkCapture) {

		try (
				Session session = driver.session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction();
		) {
			transaction.run("MATCH (n) detach delete n");

			person1Id = transaction.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n) AS id",
					Values.parameters("name", TEST_PERSON1_NAME)).single().get("id").asLong();
			person2Id = transaction.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n) AS id",
					Values.parameters("name", TEST_PERSON2_NAME)).single().get("id").asLong();

			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			simonsId = transaction
					.run("CREATE (p:Person{firstName: 'Michael', lastName: 'Siemons'})" +
							"-[:LIVES_AT]->(a:Address {city: 'Aachen'})" +
							"-[:BASED_IN]->(c:YetAnotherCountryEntity{name: 'Gemany', countryCode: 'DE'})" +
							"RETURN id(p)")
					.single().get(0).asLong();
			nullNullSchneider = transaction
					.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schnitzel'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'}) RETURN id(p)")
					.single().get(0).asLong();
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.run("CREATE (p:PersonWithAssignedId{id: 'x', firstName: 'John', lastName: 'Doe'})");

			transaction.run(
					"CREATE (root:NodeEntity:BaseNodeEntity{nodeId: 'root'}) " +
					"CREATE (company:NodeEntity:BaseNodeEntity{nodeId: 'comp'}) " +
					"CREATE (cred:Credential{id: 'uuid-1', name: 'Creds'}) " +
					"CREATE (company)-[:CHILD_OF]->(root) " +
					"CREATE (root)-[:HAS_CREDENTIAL]->(cred) " +
					"CREATE (company)-[:WITH_CREDENTIAL]->(cred)");

			transaction.commit();

			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void count() {
		StepVerifier.create(neo4jTemplate.count(PersonWithAllConstructor.class))
				.assertNext(count -> assertThat(count).isEqualTo(2)).verifyComplete();
	}

	@Test
	void countWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(Functions.count(node)).build();

		StepVerifier.create(neo4jTemplate.count(statement)).assertNext(count -> assertThat(count).isEqualTo(2))
				.verifyComplete();
	}

	@Test
	void countWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(parameter("name")))
				.returning(Functions.count(node)).build();

		StepVerifier.create(neo4jTemplate.count(statement, Collections.singletonMap("name", TEST_PERSON1_NAME)))
				.assertNext(count -> assertThat(count).isEqualTo(1)).verifyComplete();
	}

	@Test
	void countWithCypherQuery() {

		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return count(p)";

		StepVerifier.create(neo4jTemplate.count(cypherQuery)).assertNext(count -> assertThat(count).isEqualTo(2))
				.verifyComplete();
	}

	@Test
	void countWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return count(p)";

		StepVerifier.create(neo4jTemplate.count(cypherQuery, Collections.singletonMap("name", TEST_PERSON1_NAME)))
				.assertNext(count -> assertThat(count).isEqualTo(1)).verifyComplete();
	}

	@Test
	void findAll() {
		StepVerifier.create(neo4jTemplate.findAll(PersonWithAllConstructor.class)).expectNextCount(2).verifyComplete();
	}

	@Test
	void findAllWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(node).build();

		StepVerifier.create(neo4jTemplate.findAll(statement, PersonWithAllConstructor.class)).expectNextCount(2)
				.verifyComplete();
	}

	@Test
	void findAllWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(parameter("name")))
				.returning(node)
				.build();

		StepVerifier.create(neo4jTemplate.findAll(statement, Collections.singletonMap("name", TEST_PERSON1_NAME),
				PersonWithAllConstructor.class)).expectNextCount(1).verifyComplete();
	}

	@Test
	void findOneWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(parameter("name")))
				.returning(node)
				.build();

		StepVerifier.create(neo4jTemplate.findOne(statement, Collections.singletonMap("name", TEST_PERSON1_NAME),
				PersonWithAllConstructor.class)).expectNextCount(1).verifyComplete();
	}

	@Test
	void findAllWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return p";

		StepVerifier.create(neo4jTemplate.findAll(cypherQuery, PersonWithAllConstructor.class)).expectNextCount(2)
				.verifyComplete();
	}

	@Test
	void findAllWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		StepVerifier.create(neo4jTemplate.findAll(cypherQuery, Collections.singletonMap("name", TEST_PERSON1_NAME),
				PersonWithAllConstructor.class)).expectNextCount(1).verifyComplete();
	}

	@Test
	void findOneWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		StepVerifier.create(neo4jTemplate.findOne(cypherQuery, Collections.singletonMap("name", TEST_PERSON1_NAME),
				PersonWithAllConstructor.class)).expectNextCount(1).verifyComplete();
	}

	@Test
	void findById() {
		StepVerifier.create(neo4jTemplate.findById(person1Id, PersonWithAllConstructor.class)).expectNextCount(1)
				.verifyComplete();
	}

	@Test
	void findAllById() {
		StepVerifier
				.create(neo4jTemplate.findAllById(Arrays.asList(person1Id, person2Id), PersonWithAllConstructor.class))
				.expectNextCount(2).verifyComplete();
	}

	@Test
	void save(@Autowired BookmarkCapture bookmarkCapture) {
		StepVerifier.create(neo4jTemplate.save(new ThingWithGeneratedId("testThing"))).expectNextCount(1)
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (t:ThingWithGeneratedId{name: 'testThing'}) return t");
			Value resultValue = result.single().get("t");
			assertThat(resultValue).isNotNull();
			assertThat(resultValue.asMap().get("name")).isEqualTo("testThing");
		}
	}

	@Test
	void saveAll(@Autowired BookmarkCapture bookmarkCapture) {
		String thing1Name = "testThing1";
		String thing2Name = "testThing2";
		ThingWithGeneratedId thing1 = new ThingWithGeneratedId(thing1Name);
		ThingWithGeneratedId thing2 = new ThingWithGeneratedId(thing2Name);

		StepVerifier.create(neo4jTemplate.saveAll(Arrays.asList(thing1, thing2))).expectNextCount(2).verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("name1", thing1Name);
			paramMap.put("name2", thing2Name);

			Result result = session
					.run("MATCH (t:ThingWithGeneratedId) WHERE t.name = $name1 or t.name = $name2 return t",
							paramMap);
			List<Record> resultValues = result.list();
			assertThat(resultValues).hasSize(2);
			assertThat(resultValues).allMatch(
					record -> record.asMap(Function.identity()).get("t").get("name").asString()
							.startsWith("testThing"));
		}
	}

	@Test // 2230
	void findAllWithStatementWithoutParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(Cypher.parameter("name").withValue(TEST_PERSON1_NAME)))
				.returning(node).build();

		neo4jTemplate.findAll(statement, PersonWithAllConstructor.class)
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
	}

	@Test
	void deleteById(@Autowired BookmarkCapture bookmarkCapture) {
		StepVerifier.create(neo4jTemplate.deleteById(person1Id, PersonWithAllConstructor.class)).verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(1);
		}
	}

	@Test
	void deleteAllById(@Autowired BookmarkCapture bookmarkCapture) {

		StepVerifier
				.create(neo4jTemplate
						.deleteAllById(Arrays.asList(person1Id, person2Id), PersonWithAllConstructor.class))
				.verifyComplete();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
		}
	}

	interface OpenProjection {

		String getLastName();

		@org.springframework.beans.factory.annotation.Value("#{target.firstName + ' ' + target.lastName}")
		String getFullName();
	}

	interface ClosedProjection {

		String getLastName();
	}

	interface ClosedProjectionWithEmbeddedProjection {

		String getLastName();

		AddressProjection getAddress();

		interface AddressProjection {

			String getStreet();

			CountryProjection getCountry();

			interface CountryProjection {
				String getName();
			}
		}
	}

	@Data
	static class DtoPersonProjection {

		/** The ID is required in a project that should be saved. */
		private final Long id;

		private String lastName;
		private String firstName;
	}

	@Test
	void saveAsWithOpenProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.flatMap(p -> {
					p.setFirstName("Micha");
					p.setLastName("Simons");
					return template.saveAs(p, OpenProjection.class);
				}).map(OpenProjection::getFullName)
				.as(StepVerifier::create)
				.expectNext("Michael Simons")
				.verifyComplete();

		template.findById(simonsId, Person.class)
				.as(StepVerifier::create)
				.consumeNextWith(p -> {
					assertThat(p.getFirstName()).isEqualTo("Michael");
					assertThat(p.getLastName()).isEqualTo("Simons");
					assertThat(p.getAddress()).isNotNull();
				})
				.verifyComplete();
	}

	@Test // GH-2215
	void saveProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		template
				.find(Person.class)
				.as(DtoPersonProjection.class)
				.matching("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"))
				.one()
				.flatMap(p -> {
					p.setFirstName("Micha");
					p.setLastName("Simons");
					return template.save(Person.class).one(p);
				})
				.doOnNext(signal -> {
					assertThat(signal.getFirstName()).isEqualTo("Micha");
					assertThat(signal.getLastName()).isEqualTo("Simons");
				})
				.flatMap(savedProjection -> template.findById(savedProjection.getId(), Person.class))
				.as(StepVerifier::create)
				.expectNextMatches(
						person -> person.getFirstName().equals("Micha") && person.getLastName().equals("Simons")
								  && person.getAddress() != null)
				.verifyComplete();
	}

	@Test // GH-2215
	void saveAllProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		template
				.find(Person.class)
				.as(DtoPersonProjection.class)
				.matching("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"))
				.one()
				.flatMapMany(p -> {
					p.setFirstName("Micha");
					p.setLastName("Simons");
					return template.save(Person.class).all(Collections.singleton(p));
				})
				.doOnNext(signal -> {
					assertThat(signal.getFirstName()).isEqualTo("Micha");
					assertThat(signal.getLastName()).isEqualTo("Simons");
				})
				.flatMap(savedProjection -> template.findById(savedProjection.getId(), Person.class))
				.as(StepVerifier::create)
				.expectNextMatches(
						person -> person.getFirstName().equals("Micha") && person.getLastName().equals("Simons")
								  && person.getAddress() != null)
				.verifyComplete();
	}

	@Test
	void saveAllAsWithOpenProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.zipWith(template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
						Collections.singletonMap("lastName", "Schnitzel"), Person.class))
				.flatMapMany(t -> {
					Person p1 = t.getT1();
					Person p2 = t.getT2();

					p1.setFirstName("Micha");
					p1.setLastName("Simons");

					p2.setFirstName("Helga");
					p2.setLastName("Schneider");
					return template.saveAllAs(Arrays.asList(p1, p2), OpenProjection.class);
				})
				.map(OpenProjection::getFullName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("Helge Schneider", "Michael Simons")
				.verifyComplete();

		template.findAllById(Arrays.asList(simonsId, nullNullSchneider), Person.class)
				.collectList()
				.as(StepVerifier::create)
				.consumeNextWith(people -> {
					assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
					assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
					assertThat(people).allMatch(p -> p.getAddress() != null);
				})
				.verifyComplete();
	}

	@Test
	void saveAsWithSameClassShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.flatMap(p -> {
					p.setFirstName("Micha");
					p.setLastName("Simons");
					return template.saveAs(p, Person.class);
				}).map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Micha")
				.verifyComplete();

		template.findById(simonsId, Person.class)
				.as(StepVerifier::create)
				.consumeNextWith(p -> {
					assertThat(p.getFirstName()).isEqualTo("Micha");
					assertThat(p.getLastName()).isEqualTo("Simons");
					assertThat(p.getAddress()).isNull();
				})
				.verifyComplete();
	}

	@Test
	void saveAllAsWithSameClassShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.zipWith(template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
						Collections.singletonMap("lastName", "Schnitzel"), Person.class))
				.flatMapMany(t -> {
					Person p1 = t.getT1();
					Person p2 = t.getT2();

					p1.setFirstName("Micha");
					p1.setLastName("Simons");

					p2.setFirstName("Helga");
					p2.setLastName("Schneider");
					return template.saveAllAs(Arrays.asList(p1, p2), Person.class);
				})
				.map(Person::getLastName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("Schneider", "Simons")
				.verifyComplete();

		template.findAllById(Arrays.asList(simonsId, nullNullSchneider), Person.class)
				.collectList()
				.as(StepVerifier::create)
				.consumeNextWith(people -> {
					assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Micha", "Helga");
					assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
					assertThat(people).allMatch(p -> p.getAddress() == null);
				})
				.verifyComplete();
	}

	@Test
	void saveAsWithClosedProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.flatMap(p -> {
					p.setFirstName("Micha");
					p.setLastName("Simons");
					return template.saveAs(p, ClosedProjection.class);
				}).map(ClosedProjection::getLastName)
				.as(StepVerifier::create)
				.expectNext("Simons")
				.verifyComplete();

		template.findById(simonsId, Person.class)
				.as(StepVerifier::create)
				.consumeNextWith(p -> {
					assertThat(p.getFirstName()).isEqualTo("Michael");
					assertThat(p.getLastName()).isEqualTo("Simons");
					assertThat(p.getAddress()).isNotNull();
				})
				.verifyComplete();
	}

	@Test
	void saveAsWithClosedProjectionOnSecondLevelShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		template.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address) RETURN p, collect(r), collect(a)",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.flatMapMany(p -> {

					p.getAddress().setCity("Braunschweig");
					p.getAddress().setStreet("Single Trail");
					return neo4jTemplate.saveAs(p, ClosedProjectionWithEmbeddedProjection.class);
				})
				.as(StepVerifier::create)
				.assertNext(projection -> {
					assertThat(projection.getAddress().getStreet()).isEqualTo("Single Trail");
				})
				.verifyComplete();
		template.findById(simonsId, Person.class)
				.as(StepVerifier::create)
				.assertNext(p -> {
					assertThat(p.getAddress().getCity()).isEqualTo("Aachen");
					assertThat(p.getAddress().getStreet()).isEqualTo("Single Trail");
				})
				.verifyComplete();
	}

	@Test
	void saveAsWithClosedProjectionOnThreeLevelShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		template.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address)-[r2:BASED_IN]->(c:YetAnotherCountryEntity) RETURN p, collect(r), collect(r2), collect(a), collect(c)",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.flatMapMany(p -> {

					Person.Address.Country country = p.getAddress().getCountry();
					country.setName("Germany");
					country.setCountryCode("AT");
					return neo4jTemplate.saveAs(p, ClosedProjectionWithEmbeddedProjection.class);
				})
				.as(StepVerifier::create)
				.assertNext(p -> assertThat(p.getAddress().getCountry().getName()).isEqualTo("Germany"))
				.verifyComplete();

		template.findById(simonsId, Person.class)
				.as(StepVerifier::create)
				.assertNext(p -> {
					Person.Address.Country savedCountry = p.getAddress().getCountry();
					assertThat(savedCountry.getCountryCode()).isEqualTo("DE");
					assertThat(savedCountry.getName()).isEqualTo("Germany");
				})
				.verifyComplete();
	}

	@Test
	void saveAllAsWithClosedProjectionShouldWork(@Autowired ReactiveNeo4jTemplate template) {

		// Using a query on purpose so that the address is null
		template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
				.zipWith(template.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
						Collections.singletonMap("lastName", "Schnitzel"), Person.class))
				.flatMapMany(t -> {
					Person p1 = t.getT1();
					Person p2 = t.getT2();

					p1.setFirstName("Micha");
					p1.setLastName("Simons");

					p2.setFirstName("Helga");
					p2.setLastName("Schneider");
					return template.saveAllAs(Arrays.asList(p1, p2), ClosedProjection.class);
				})
				.map(ClosedProjection::getLastName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("Schneider", "Simons")
				.verifyComplete();

		template.findAllById(Arrays.asList(simonsId, nullNullSchneider), Person.class)
				.collectList()
				.as(StepVerifier::create)
				.consumeNextWith(people -> {
					assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
					assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
					assertThat(people).allMatch(p -> p.getAddress() != null);
				})
				.verifyComplete();
	}

	@Test
	void updatingFindShouldWork() {
		Map<String, Object> params = new HashMap<>();
		params.put("wrongName", "Siemons");
		params.put("correctName", "Simons");
		neo4jTemplate
				.findOne("MERGE (p:Person {lastName: $wrongName}) ON MATCH set p.lastName = $correctName RETURN p",
						params, Person.class)
				.as(StepVerifier::create)
				.consumeNextWith(updatedPerson -> {

					assertThat(updatedPerson.getLastName()).isEqualTo("Simons");
					assertThat(updatedPerson.getAddress()).isNull(); // We didn't fetch it
				})
				.verifyComplete();
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsShouldWork() {
		neo4jTemplate.find(Person.class).all().as(StepVerifier::create)
				.expectNextCount(4L)
				.verifyComplete();
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsProjectedShouldWork() {
		neo4jTemplate.find(Person.class).as(OpenProjection.class).all()
				.map(OpenProjection::getFullName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("A LA", "Bela B.", "Helge Schnitzel", "Michael Siemons")
				.verifyComplete();
	}

	@Test // GH-2270
	void executableFindShouldWorkAllDomainObjectsProjectedDTOShouldWork() {

		neo4jTemplate.find(Person.class).as(DtoPersonProjection.class).all()
				.map(DtoPersonProjection::getLastName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("B.", "LA", "Schnitzel", "Siemons")
				.verifyComplete();
	}

	@Test // GH-2270
	void executableFindShouldWorkOneDomainObjectsProjectedDTOShouldWork() {

		neo4jTemplate.find(Person.class).as(DtoPersonProjection.class)
				.matching("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Schnitzel"))
				.one()
				.map(DtoPersonProjection::getLastName)
				.as(StepVerifier::create)
				.expectNext("Schnitzel")
				.verifyComplete();
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQuery() {
		neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1").all()
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndParam() {
		neo4jTemplate.find(Person.class)
				.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
						Collections.singletonMap("lastName", "Schnitzel"))
				.all()
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndNullParams() {

		neo4jTemplate.find(Person.class)
				.matching("MATCH (p:Person) RETURN p LIMIT 1", null)
				.all()
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();

	}

	@Test
	void oneShouldWork() {
		neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1").one()
				.as(StepVerifier::create)
				.expectNextCount(1L)
				.verifyComplete();
	}

	@Test
	void oneShouldWorkWithIncorrectResultSize() {
		neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p").one()
				.as(StepVerifier::create)
				.verifyError(IncorrectResultSizeDataAccessException.class);
	}

	@Test
	void statementShouldWork() {
		Node person = Cypher.node("Person");
		Flux<Person> people = neo4jTemplate.find(Person.class).matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.anonParameter("Siemons")))
				.returning(person).build())
				.all();
		people.map(Person::getLastName).as(StepVerifier::create).expectNext("Siemons").verifyComplete();
	}

	@Test
	void statementWithParamsShouldWork() {
		Node person = Cypher.node("Person");
		Flux<Person> people = neo4jTemplate.find(Person.class).matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.parameter("lastName", "Siemons")))
				.returning(person).build(), Collections.singletonMap("lastName", "Schnitzel"))
				.all();
		people.map(Person::getLastName).as(StepVerifier::create).expectNext("Schnitzel").verifyComplete();
	}

	@Test // GH-2407
	void shouldSaveAllAsWithAssignedIdProjected() {

		neo4jTemplate.findById("x", PersonWithAssignedId.class)
				.flatMapMany(p -> {
					p.setLastName("modifiedLast");
					p.setFirstName("modifiedFirst");
					return neo4jTemplate.saveAllAs(Collections.singletonList(p), ClosedProjection.class);
				}).map(ClosedProjection::getLastName)
				.as(StepVerifier::create)
				.expectNext("modifiedLast")
				.verifyComplete();

		neo4jTemplate.findById("x", PersonWithAssignedId.class)
				.as(StepVerifier::create)
				.consumeNextWith(p -> {
					assertThat(p.getFirstName()).isEqualTo("John");
					assertThat(p.getLastName()).isEqualTo("modifiedLast");
				})
				.verifyComplete();
	}

	@Test // GH-2407
	void shouldSaveAsWithAssignedIdProjected() {

		neo4jTemplate.findById("x", PersonWithAssignedId.class)
				.flatMap(p -> {
					p.setLastName("modifiedLast");
					p.setFirstName("modifiedFirst");
					return neo4jTemplate.saveAs(p, ClosedProjection.class);
				}).map(ClosedProjection::getLastName)
				.as(StepVerifier::create)
				.expectNext("modifiedLast")
				.verifyComplete();

		neo4jTemplate.findById("x", PersonWithAssignedId.class)
				.as(StepVerifier::create)
				.consumeNextWith(p -> {
					assertThat(p.getFirstName()).isEqualTo("John");
					assertThat(p.getLastName()).isEqualTo("modifiedLast");
				})
				.verifyComplete();
	}

	@Test // GH-2415
	void saveWithProjectionImplementedByEntity(@Autowired Neo4jMappingContext mappingContext) {

		Neo4jPersistentEntity<?> metaData = mappingContext.getPersistentEntity(BaseNodeEntity.class);
		neo4jTemplate
				.find(BaseNodeEntity.class)
				.as(NodeEntity.class)
				.matching(QueryFragmentsAndParameters.forCondition(metaData, Constants.NAME_OF_TYPED_ROOT_NODE.apply(metaData).property("nodeId").isEqualTo(Cypher.literalOf("root"))))
				.one()
				.flatMap(nodeEntity -> neo4jTemplate.saveAs(nodeEntity, NodeWithDefinedCredentials.class))
				.flatMap(nodeEntity -> neo4jTemplate.findById(nodeEntity.getNodeId(), NodeEntity.class))
				.as(StepVerifier::create)
				.consumeNextWith(nodeEntity -> assertThat(nodeEntity.getChildren()).hasSize(1))
				.verifyComplete();
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
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
