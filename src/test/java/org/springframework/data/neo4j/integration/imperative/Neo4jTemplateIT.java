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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.integration.shared.common.ThingWithGeneratedId;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Rosetta Roberts
 */
@Neo4jIntegrationTest
class Neo4jTemplateIT {
	private static final String TEST_PERSON1_NAME = "Test";
	private static final String TEST_PERSON2_NAME = "Test2";

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final Neo4jTemplate neo4jTemplate;
	private final BookmarkCapture bookmarkCapture;

	private Long person1Id;
	private Long person2Id;

	@Autowired
	Neo4jTemplateIT(Driver driver, Neo4jTemplate neo4jTemplate, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.neo4jTemplate = neo4jTemplate;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	void setupData() {

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
			transaction
					.run("CREATE (p:Person{firstName: 'Michael', lastName: 'Siemons'}) -[:LIVES_AT]-> (a:Address {city: 'Aachen'}) RETURN id(p)");
			transaction
					.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schnitzel'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'}) RETURN id(p)");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");

			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void count() {
		assertThat(neo4jTemplate.count(PersonWithAllConstructor.class)).isEqualTo(2);
	}

	@Test
	void countWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(Functions.count(node)).build();

		assertThat(neo4jTemplate.count(statement)).isEqualTo(2);
	}

	@Test
	void countWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(Cypher.parameter("name")))
				.returning(Functions.count(node)).build();

		assertThat(neo4jTemplate.count(statement, Collections.singletonMap("name", TEST_PERSON1_NAME))).isEqualTo(1);
	}

	@Test
	void countWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return count(p)";

		assertThat(neo4jTemplate.count(cypherQuery)).isEqualTo(2);
	}

	@Test
	void countWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return count(p)";

		assertThat(neo4jTemplate.count(cypherQuery, Collections.singletonMap("name", TEST_PERSON1_NAME))).isEqualTo(1);
	}

	@Test
	void findAll() {
		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(node).build();

		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(statement, PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(Cypher.parameter("name")))
				.returning(node).build();

		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(statement, Collections
						.singletonMap("name", TEST_PERSON1_NAME),
				PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findOneWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(Cypher.parameter("name")))
				.returning(node).build();

		Optional<PersonWithAllConstructor> person = neo4jTemplate.findOne(statement,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test // 2230
	void findAllWithStatementWithoutParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).where(node.property("name").isEqualTo(Cypher.parameter("name").withValue(TEST_PERSON1_NAME)))
				.returning(node).build();

		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(statement, PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findAllWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return p";

		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(cypherQuery, PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		List<PersonWithAllConstructor> people = neo4jTemplate.findAll(cypherQuery,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findOneWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		Optional<PersonWithAllConstructor> person = neo4jTemplate.findOne(cypherQuery,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test
	void findById() {
		Optional<PersonWithAllConstructor> person = neo4jTemplate.findById(person1Id, PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test
	void findAllById() {
		List<PersonWithAllConstructor> people = neo4jTemplate.findAllById(Arrays.asList(person1Id, person2Id),
				PersonWithAllConstructor.class);

		assertThat(people).hasSize(2);
	}

	@Test
	void save() {
		ThingWithGeneratedId testThing = neo4jTemplate.save(new ThingWithGeneratedId("testThing"));

		assertThat(testThing.getTheId()).isNotNull();

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
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
		List<ThingWithGeneratedId> savedThings = neo4jTemplate.saveAll(Arrays.asList(thing1, thing2));

		assertThat(savedThings).hasSize(2);

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

	@Test
	void deleteById() {
		neo4jTemplate.deleteById(person1Id, PersonWithAllConstructor.class);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(1);
		}
	}

	@Test
	void deleteAllById() {
		neo4jTemplate.deleteAllById(Arrays.asList(person1Id, person2Id), PersonWithAllConstructor.class);

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

	@Test
	void saveAsWithOpenProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		OpenProjection openProjection = neo4jTemplate.saveAs(p, OpenProjection.class);

		assertThat(openProjection.getFullName()).isEqualTo("Michael Simons");
		p = neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNotNull();
	}

	@Test
	void saveAllAsWithOpenProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();
		Person p2 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Schnitzel"), Person.class).get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<OpenProjection> openProjection = neo4jTemplate.saveAllAs(Arrays.asList(p1, p2), OpenProjection.class);

		assertThat(openProjection).extracting(OpenProjection::getFullName)
				.containsExactlyInAnyOrder("Michael Simons", "Helge Schneider");

		List<Person> people = neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() != null);
	}

	@Test
	void saveAsWithSameClassShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		Person savedInstance = neo4jTemplate.saveAs(p, Person.class);

		assertThat(savedInstance.getFirstName()).isEqualTo("Micha");
		p = neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Micha");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNull();
	}

	@Test
	void saveAllAsWithSameClassShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();
		Person p2 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Schnitzel"), Person.class).get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<Person> openProjection = neo4jTemplate.saveAllAs(Arrays.asList(p1, p2), Person.class);

		assertThat(openProjection).extracting(Person::getFirstName)
				.containsExactlyInAnyOrder("Micha", "Helga");

		List<Person> people = neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Micha", "Helga");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() == null);
	}

	@Test
	void saveAsWithClosedProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		ClosedProjection closedProjection = neo4jTemplate.saveAs(p, ClosedProjection.class);

		assertThat(closedProjection.getLastName()).isEqualTo("Simons");
		p = neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNotNull();
	}

	@Test
	void saveAllAsWithClosedProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Siemons"), Person.class).get();
		Person p2 = neo4jTemplate.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Schnitzel"), Person.class).get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<ClosedProjection> openProjection = neo4jTemplate
				.saveAllAs(Arrays.asList(p1, p2), ClosedProjection.class);

		assertThat(openProjection).extracting(ClosedProjection::getLastName)
				.containsExactlyInAnyOrder("Simons", "Schneider");

		List<Person> people = neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() != null);
	}

	@Test
	void updatingFindShouldWork() {
		Map<String, Object> params = new HashMap<>();
		params.put("wrongName", "Siemons");
		params.put("correctName", "Simons");
		Optional<Person> optionalResult = neo4jTemplate
				.findOne("MERGE (p:Person {lastName: $wrongName}) ON MATCH set p.lastName = $correctName RETURN p",
						params, Person.class);

		assertThat(optionalResult).hasValueSatisfying(
				updatedPerson -> {
					assertThat(updatedPerson.getLastName()).isEqualTo("Simons");
					assertThat(updatedPerson.getAddress()).isNull(); // We didn't fetch it
				}
		);
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsShouldWork() {
		List<Person> people = neo4jTemplate.find(Person.class).all();
		assertThat(people).hasSize(4);
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsProjectedShouldWork() {
		List<OpenProjection> people = neo4jTemplate.find(Person.class).as(OpenProjection.class).all();
		assertThat(people).extracting(OpenProjection::getFullName)
				.containsExactlyInAnyOrder("Helge Schnitzel", "Michael Siemons", "Bela B.", "A LA");
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQuery() {
		List<Person> people = neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1").all();
		assertThat(people).hasSize(1);
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndParam() {
		List<Person> people = neo4jTemplate.find(Person.class)
				.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
						Collections.singletonMap("lastName", "Schnitzel")).all();
		assertThat(people).hasSize(1);
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndNullParams() {
		List<Person> people = neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1", null)
				.all();
		assertThat(people).hasSize(1);
	}

	@Test
	void oneShouldWork() {
		Optional<Person> people = neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1")
				.one();
		assertThat(people).isPresent();
	}

	@Test
	void oneShouldWorkWithIncorrectResultSize() {
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p").one());
	}

	@Test
	void statementShouldWork() {
		Node person = Cypher.node("Person");
		List<Person> people = neo4jTemplate.find(Person.class).matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.anonParameter("Siemons")))
				.returning(person).build())
				.all();
		assertThat(people).extracting(Person::getLastName).containsExactly("Siemons");
	}

	@Test
	void statementWithParamsShouldWork() {
		Node person = Cypher.node("Person");
		List<Person> people = neo4jTemplate.find(Person.class).matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.parameter("lastName", "Siemons")))
				.returning(person).build(), Collections.singletonMap("lastName", "Schnitzel"))
				.all();
		assertThat(people).extracting(Person::getLastName).containsExactly("Schnitzel");
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

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
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}
	}
}
