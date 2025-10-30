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
package org.springframework.data.neo4j.integration.imperative;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
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
import org.springframework.data.core.PropertyPath;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.EntityWithPrimitiveConstructorArguments;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAllConstructor;
import org.springframework.data.neo4j.integration.shared.common.PersonWithAssignedId;
import org.springframework.data.neo4j.integration.shared.common.ThingWithGeneratedId;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @author Rosetta Roberts
 * @author Corey Beres
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

	private static BiPredicate<PropertyPath, Neo4jPersistentProperty> create2LevelProjectingPredicate() {
		BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate = (path, property) -> false;
		predicate = predicate.or((path, property) -> property.getName().equals("lastName"));
		predicate = predicate.or((path, property) -> property.getName().equals("address")
				|| path.toDotPath().startsWith("address.") && property.getName().equals("street"));
		predicate = predicate.or((path, property) -> property.getName().equals("country")
				|| path.toDotPath().contains("address.country.") && property.getName().equals("name"));
		return predicate;
	}

	@BeforeEach
	void setupData() {

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");

			this.person1Id = transaction
				.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n) AS id",
						Values.parameters("name", TEST_PERSON1_NAME))
				.single()
				.get("id")
				.asLong();
			this.person2Id = transaction
				.run("CREATE (n:PersonWithAllConstructor) SET n.name = $name RETURN id(n) AS id",
						Values.parameters("name", TEST_PERSON2_NAME))
				.single()
				.get("id")
				.asLong();

			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'Michael', lastName: 'Siemons'})"
					+ " -[:LIVES_AT]-> (a:Address {city: 'Aachen', id: 1})"
					+ " -[:BASED_IN]->(c:YetAnotherCountryEntity{name: 'Gemany', countryCode: 'DE'})"
					+ " RETURN id(p)");
			transaction.run(
					"CREATE (p:Person{firstName: 'Helge', lastName: 'Schnitzel'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'}) RETURN id(p)");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.run("CREATE (p:PersonWithAssignedId{id: 'x', firstName: 'John', lastName: 'Doe'})");

			transaction.commit();
			this.bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void count() {
		assertThat(this.neo4jTemplate.count(PersonWithAllConstructor.class)).isEqualTo(2);
	}

	@Test
	void countWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(Cypher.count(node)).build();

		assertThat(this.neo4jTemplate.count(statement)).isEqualTo(2);
	}

	@Test
	void countWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(Cypher.parameter("name")))
			.returning(Cypher.count(node))
			.build();

		assertThat(this.neo4jTemplate.count(statement, Collections.singletonMap("name", TEST_PERSON1_NAME)))
			.isEqualTo(1);
	}

	@Test
	void countWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return count(p)";

		assertThat(this.neo4jTemplate.count(cypherQuery)).isEqualTo(2);
	}

	@Test
	void countWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return count(p)";

		assertThat(this.neo4jTemplate.count(cypherQuery, Collections.singletonMap("name", TEST_PERSON1_NAME)))
			.isEqualTo(1);
	}

	@Test
	void findAll() {
		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithStatement() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node).returning(node).build();

		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(statement, PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(Cypher.parameter("name")))
			.returning(node)
			.build();

		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(statement,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findOneWithStatementAndParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(Cypher.parameter("name")))
			.returning(node)
			.build();

		Optional<PersonWithAllConstructor> person = this.neo4jTemplate.findOne(statement,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test // 2230
	void findAllWithStatementWithoutParameters() {
		Node node = Cypher.node("PersonWithAllConstructor").named("n");
		Statement statement = Cypher.match(node)
			.where(node.property("name").isEqualTo(Cypher.parameter("name").withValue(TEST_PERSON1_NAME)))
			.returning(node)
			.build();

		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(statement, PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findAllWithCypherQuery() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) return p";

		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(cypherQuery, PersonWithAllConstructor.class);
		assertThat(people).hasSize(2);
	}

	@Test
	void findAllWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		List<PersonWithAllConstructor> people = this.neo4jTemplate.findAll(cypherQuery,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(people).hasSize(1);
	}

	@Test
	void findOneWithCypherQueryAndParameters() {
		String cypherQuery = "MATCH (p:PersonWithAllConstructor) WHERE p.name = $name return p";

		Optional<PersonWithAllConstructor> person = this.neo4jTemplate.findOne(cypherQuery,
				Collections.singletonMap("name", TEST_PERSON1_NAME), PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test
	void findById() {
		Optional<PersonWithAllConstructor> person = this.neo4jTemplate.findById(this.person1Id,
				PersonWithAllConstructor.class);

		assertThat(person).isPresent();
	}

	@Test
	void findAllById() {
		List<PersonWithAllConstructor> people = this.neo4jTemplate
			.findAllById(Arrays.asList(this.person1Id, this.person2Id), PersonWithAllConstructor.class);

		assertThat(people).hasSize(2);
	}

	@Test
	void save() {
		ThingWithGeneratedId testThing = this.neo4jTemplate.save(new ThingWithGeneratedId("testThing"));

		assertThat(testThing.getTheId()).isNotNull();

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
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
		List<ThingWithGeneratedId> savedThings = this.neo4jTemplate.saveAll(Arrays.asList(thing1, thing2));

		assertThat(savedThings).hasSize(2);

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			Map<String, Object> paramMap = new HashMap<>();
			paramMap.put("name1", thing1Name);
			paramMap.put("name2", thing2Name);

			Result result = session
				.run("MATCH (t:ThingWithGeneratedId) WHERE t.name = $name1 or t.name = $name2 return t", paramMap);
			List<Record> resultValues = result.list();
			assertThat(resultValues).hasSize(2);
			assertThat(resultValues).allMatch(record -> record.asMap(Function.identity())
				.get("t")
				.get("name")
				.asString()
				.startsWith("testThing"));
		}
	}

	@Test
	void deleteById() {
		this.neo4jTemplate.deleteById(this.person1Id, PersonWithAllConstructor.class);

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(1);
		}
	}

	@Test
	void deleteAllById() {
		this.neo4jTemplate.deleteAllById(Arrays.asList(this.person1Id, this.person2Id), PersonWithAllConstructor.class);

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithAllConstructor) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
		}
	}

	@Test // GH-2215
	void saveProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		DtoPersonProjection dtoPersonProjection = this.neo4jTemplate.find(Person.class)
			.as(DtoPersonProjection.class)
			.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Siemons"))
			.one()
			.get();

		dtoPersonProjection.setFirstName("Micha");
		dtoPersonProjection.setLastName("Simons");

		DtoPersonProjection savedProjection = this.neo4jTemplate.save(Person.class).one(dtoPersonProjection);

		// Assert that we saved and returned the correct data
		assertThat(savedProjection.getFirstName()).isEqualTo("Micha");
		assertThat(savedProjection.getLastName()).isEqualTo("Simons");

		// Assert the result inside the database.
		Person person = this.neo4jTemplate.findById(savedProjection.getId(), Person.class).get();
		assertThat(person.getFirstName()).isEqualTo("Micha");
		assertThat(person.getLastName()).isEqualTo("Simons");
		assertThat(person.getAddress()).isNotNull();
	}

	@Test // GH-2505
	void savePrimitivesShouldWork() {
		EntityWithPrimitiveConstructorArguments entity = new EntityWithPrimitiveConstructorArguments(true, 42);
		EntityWithPrimitiveConstructorArguments savedEntity = this.neo4jTemplate
			.save(EntityWithPrimitiveConstructorArguments.class)
			.one(entity);

		assertThat(savedEntity.someIntValue).isEqualTo(entity.someIntValue);
		assertThat(savedEntity.someBooleanValue).isEqualTo(entity.someBooleanValue);
	}

	@Test // GH-2215
	void saveAllProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		DtoPersonProjection dtoPersonProjection = this.neo4jTemplate.find(Person.class)
			.as(DtoPersonProjection.class)
			.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Siemons"))
			.one()
			.get();

		dtoPersonProjection.setFirstName("Micha");
		dtoPersonProjection.setLastName("Simons");

		Iterable<DtoPersonProjection> savedProjections = this.neo4jTemplate.save(Person.class)
			.all(Collections.singleton(dtoPersonProjection));

		DtoPersonProjection savedProjection = savedProjections.iterator().next();
		// Assert that we saved and returned the correct data
		assertThat(savedProjection.getFirstName()).isEqualTo("Micha");
		assertThat(savedProjection.getLastName()).isEqualTo("Simons");

		// Assert the result inside the database.
		Person person = this.neo4jTemplate.findById(savedProjection.getId(), Person.class).get();
		assertThat(person.getFirstName()).isEqualTo("Micha");
		assertThat(person.getLastName()).isEqualTo("Simons");
		assertThat(person.getAddress()).isNotNull();
	}

	@Test
	void saveAsWithOpenProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		OpenProjection openProjection = this.neo4jTemplate.saveAs(p, OpenProjection.class);

		assertThat(openProjection.getFullName()).isEqualTo("Michael Simons");
		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNotNull();
	}

	@Test
	void saveAllAsWithOpenProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();
		Person p2 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Schnitzel"), Person.class)
			.get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<OpenProjection> openProjections = this.neo4jTemplate.saveAllAs(Arrays.asList(p1, p2),
				OpenProjection.class);

		assertThat(openProjections).extracting(OpenProjection::getFullName)
			.containsExactlyInAnyOrder("Michael Simons", "Helge Schneider");

		List<Person> people = this.neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() != null);
	}

	@Test
	void saveAsWithSameClassShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		Person savedInstance = this.neo4jTemplate.saveAs(p, Person.class);

		assertThat(savedInstance.getFirstName()).isEqualTo("Micha");
		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Micha");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNull();
	}

	@Test
	void saveAllAsWithSameClassShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();
		Person p2 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Schnitzel"), Person.class)
			.get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<Person> openProjection = this.neo4jTemplate.saveAllAs(Arrays.asList(p1, p2), Person.class);

		assertThat(openProjection).extracting(Person::getFirstName).containsExactlyInAnyOrder("Micha", "Helga");

		List<Person> people = this.neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Micha", "Helga");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() == null);
	}

	@Test
	void saveAsWithClosedProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();

		p.setFirstName("Micha");
		p.setLastName("Simons");
		ClosedProjection closedProjection = this.neo4jTemplate.saveAs(p, ClosedProjection.class);

		assertThat(closedProjection.getLastName()).isEqualTo("Simons");
		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress()).isNotNull();
	}

	@Test
	void saveAsWithClosedProjectionOnSecondLevelShouldWork() {

		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address) RETURN p, collect(r), collect(a)",
					Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		p.getAddress().setCity("Braunschweig");
		p.getAddress().setStreet("Single Trail");
		ClosedProjectionWithEmbeddedProjection projection = this.neo4jTemplate.saveAs(p,
				ClosedProjectionWithEmbeddedProjection.class);

		assertThat(projection.getAddress().getStreet()).isEqualTo("Single Trail");
		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getAddress().getCity()).isEqualTo("Aachen");
		assertThat(p.getAddress().getStreet()).isEqualTo("Single Trail");
	}

	@Test // GH-2420
	void saveAsWithDynamicProjectionOnSecondLevelShouldWork() {

		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address) RETURN p, collect(r), collect(a)",
					Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		p.getAddress().setCity("Braunschweig");
		p.getAddress().setStreet("Single Trail");
		Person.Address.Country country = new Person.Address.Country();
		country.setName("Foo");
		country.setCountryCode("DE");
		p.getAddress().setCountry(country);

		BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate = create2LevelProjectingPredicate();

		Person projection = this.neo4jTemplate.saveAs(p, predicate);

		assertThat(projection.getAddress().getStreet()).isEqualTo("Single Trail");
		assertThat(projection.getAddress().getCountry().getName()).isEqualTo("Foo");
		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getAddress().getCity()).isEqualTo("Aachen");
		assertThat(p.getAddress().getStreet()).isEqualTo("Single Trail");
		assertThat(p.getAddress().getCountry().getName()).isEqualTo("Foo");
	}

	@Test // GH-2407
	void saveAllAsWithClosedProjectionOnSecondLevelShouldWork() {

		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address) RETURN p, collect(r), collect(a)",
					Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		p.setFirstName("Klaus");
		p.setLastName("Simons");
		p.getAddress().setCity("Braunschweig");
		p.getAddress().setStreet("Single Trail");
		List<ClosedProjectionWithEmbeddedProjection> projections = this.neo4jTemplate
			.saveAllAs(Collections.singletonList(p), ClosedProjectionWithEmbeddedProjection.class);

		assertThat(projections).hasSize(1)
			.first()
			.satisfies(projection -> assertThat(projection.getAddress().getStreet()).isEqualTo("Single Trail"));

		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress().getCity()).isEqualTo("Aachen");
		assertThat(p.getAddress().getStreet()).isEqualTo("Single Trail");
	}

	@Test // GH-2420
	void saveAllAsWithDynamicProjectionOnSecondLevelShouldWork() {

		Person p = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address) RETURN p, collect(r), collect(a)",
					Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		p.setFirstName("Klaus");
		p.setLastName("Simons");
		p.getAddress().setCity("Braunschweig");
		p.getAddress().setStreet("Single Trail");
		Person.Address.Country country = new Person.Address.Country();
		country.setName("Foo");
		country.setCountryCode("DE");
		p.getAddress().setCountry(country);

		BiPredicate<PropertyPath, Neo4jPersistentProperty> predicate = create2LevelProjectingPredicate();

		List<Person> projections = this.neo4jTemplate.saveAllAs(Collections.singletonList(p), predicate);

		assertThat(projections).hasSize(1).first().satisfies(projection -> {
			assertThat(projection.getAddress().getStreet()).isEqualTo("Single Trail");
			assertThat(projection.getAddress().getCountry().getName()).isEqualTo("Foo");
		});

		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		assertThat(p.getFirstName()).isEqualTo("Michael");
		assertThat(p.getLastName()).isEqualTo("Simons");
		assertThat(p.getAddress().getCity()).isEqualTo("Aachen");
		assertThat(p.getAddress().getStreet()).isEqualTo("Single Trail");
		assertThat(p.getAddress().getCountry().getName()).isEqualTo("Foo");
	}

	@Test // GH-2407
	void shouldSaveNewProjectedThing() {

		Person p = new Person();
		p.setFirstName("John");
		p.setLastName("Doe");

		ClosedProjection projection = this.neo4jTemplate.saveAs(p, ClosedProjection.class);
		List<Person> people = this.neo4jTemplate.findAll("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Doe"), Person.class);
		assertThat(people).hasSize(1).first().satisfies(person -> {
			assertThat(person.getFirstName()).isNull();
			assertThat(person.getLastName()).isEqualTo(projection.getLastName());
		});
	}

	@Test // GH-2407
	void shouldSaveAllNewProjectedThings() {

		Person p = new Person();
		p.setFirstName("John");
		p.setLastName("Doe");

		List<ClosedProjection> projections = this.neo4jTemplate.saveAllAs(Collections.singletonList(p),
				ClosedProjection.class);
		assertThat(projections).hasSize(1);

		ClosedProjection projection = projections.get(0);
		List<Person> people = this.neo4jTemplate.findAll("MATCH (p:Person {lastName: $lastName}) RETURN p",
				Collections.singletonMap("lastName", "Doe"), Person.class);
		assertThat(people).hasSize(1).first().satisfies(person -> {
			assertThat(person.getFirstName()).isNull();
			assertThat(person.getLastName()).isEqualTo(projection.getLastName());
		});
	}

	@Test // GH-2407
	void shouldSaveAllAsWithAssignedIdProjected() {

		PersonWithAssignedId p = this.neo4jTemplate.findById("x", PersonWithAssignedId.class).get();
		p.setLastName("modifiedLast");
		p.setFirstName("modifiedFirst");

		List<ClosedProjection> projections = this.neo4jTemplate.saveAllAs(Collections.singletonList(p),
				ClosedProjection.class);
		assertThat(projections).hasSize(1);

		ClosedProjection projection = projections.get(0);
		List<PersonWithAssignedId> people = this.neo4jTemplate.findAll(
				"MATCH (p:PersonWithAssignedId {id: $id}) RETURN p", Collections.singletonMap("id", "x"),
				PersonWithAssignedId.class);
		assertThat(people).hasSize(1).first().satisfies(person -> {
			assertThat(person.getFirstName()).isEqualTo("John");
			assertThat(person.getLastName()).isEqualTo(projection.getLastName());
		});
	}

	@Test // GH-2407
	void shouldSaveAsWithAssignedIdProjected() {

		PersonWithAssignedId p = this.neo4jTemplate.findById("x", PersonWithAssignedId.class).get();
		p.setLastName("modifiedLast");
		p.setFirstName("modifiedFirst");

		ClosedProjection projection = this.neo4jTemplate.saveAs(p, ClosedProjection.class);
		List<PersonWithAssignedId> people = this.neo4jTemplate.findAll(
				"MATCH (p:PersonWithAssignedId {id: $id}) RETURN p", Collections.singletonMap("id", "x"),
				PersonWithAssignedId.class);
		assertThat(people).hasSize(1).first().satisfies(person -> {
			assertThat(person.getFirstName()).isEqualTo("John");
			assertThat(person.getLastName()).isEqualTo(projection.getLastName());
		});
	}

	@Test
	void saveAsWithClosedProjectionOnThreeLevelShouldWork() {

		Person p = this.neo4jTemplate.findOne(
				"MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address)-[r2:BASED_IN]->(c:YetAnotherCountryEntity) RETURN p, collect(r), collect(r2), collect(a), collect(c)",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		Person.Address.Country country = p.getAddress().getCountry();
		country.setName("Germany");
		country.setCountryCode("AT");

		ClosedProjectionWithEmbeddedProjection projection = this.neo4jTemplate.saveAs(p,
				ClosedProjectionWithEmbeddedProjection.class);
		assertThat(projection.getAddress().getCountry().getName()).isEqualTo("Germany");

		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		Person.Address.Country savedCountry = p.getAddress().getCountry();
		assertThat(savedCountry.getCountryCode()).isEqualTo("DE");
		assertThat(savedCountry.getName()).isEqualTo("Germany");
	}

	@Test // GH-2407
	void saveAllAsWithClosedProjectionOnThreeLevelShouldWork() {

		Person p = this.neo4jTemplate.findOne(
				"MATCH (p:Person {lastName: $lastName})-[r:LIVES_AT]-(a:Address)-[r2:BASED_IN]->(c:YetAnotherCountryEntity) RETURN p, collect(r), collect(r2), collect(a), collect(c)",
				Collections.singletonMap("lastName", "Siemons"), Person.class)
			.get();

		Person.Address.Country country = p.getAddress().getCountry();
		country.setName("Germany");
		country.setCountryCode("AT");

		List<ClosedProjectionWithEmbeddedProjection> projections = this.neo4jTemplate
			.saveAllAs(Collections.singletonList(p), ClosedProjectionWithEmbeddedProjection.class);

		assertThat(projections).hasSize(1)
			.first()
			.satisfies(projection -> assertThat(projection.getAddress().getCountry().getName()).isEqualTo("Germany"));

		p = this.neo4jTemplate.findById(p.getId(), Person.class).get();
		Person.Address.Country savedCountry = p.getAddress().getCountry();
		assertThat(savedCountry.getCountryCode()).isEqualTo("DE");
		assertThat(savedCountry.getName()).isEqualTo("Germany");
	}

	@Test
	void saveAllAsWithClosedProjectionShouldWork() {

		// Using a query on purpose so that the address is null
		Person p1 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p", Collections.singletonMap("lastName", "Siemons"),
					Person.class)
			.get();
		Person p2 = this.neo4jTemplate
			.findOne("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Schnitzel"), Person.class)
			.get();

		p1.setFirstName("Micha");
		p1.setLastName("Simons");

		p2.setFirstName("Helga");
		p2.setLastName("Schneider");

		List<ClosedProjection> closedProjections = this.neo4jTemplate.saveAllAs(Arrays.asList(p1, p2),
				ClosedProjection.class);

		assertThat(closedProjections).extracting(ClosedProjection::getLastName)
			.containsExactlyInAnyOrder("Simons", "Schneider");

		List<Person> people = this.neo4jTemplate.findAllById(Arrays.asList(p1.getId(), p2.getId()), Person.class);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Michael", "Helge");
		assertThat(people).extracting(Person::getLastName).containsExactlyInAnyOrder("Simons", "Schneider");
		assertThat(people).allMatch(p -> p.getAddress() != null);
	}

	@Test // GH-2544
	void saveAllAsWithEmptyList() {
		List<ClosedProjection> projections = this.neo4jTemplate.saveAllAs(Collections.emptyList(),
				ClosedProjection.class);

		assertThat(projections).isEmpty();
	}

	@Test // GH-2544
	void saveWeirdHierarchy() {

		List<Object> things = new ArrayList<>();
		things.add(new X());
		things.add(new Y());

		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.neo4jTemplate.saveAllAs(things, ClosedProjection.class))
			.withMessage("Could not determine a common element of an heterogeneous collection");
	}

	@Test
	void updatingFindShouldWork(@Autowired PlatformTransactionManager transactionManager) {
		Map<String, Object> params = new HashMap<>();
		params.put("wrongName", "Siemons");
		params.put("correctName", "Simons");
		new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
			Optional<Person> optionalResult = this.neo4jTemplate.findOne(
					"MERGE (p:Person {lastName: $wrongName}) ON MATCH set p.lastName = $correctName RETURN p", params,
					Person.class);

			assertThat(optionalResult).hasValueSatisfying(updatedPerson -> {
				assertThat(updatedPerson.getLastName()).isEqualTo("Simons");
				assertThat(updatedPerson.getAddress()).isNull(); // We didn't fetch it
			});
		});
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsShouldWork() {
		List<Person> people = this.neo4jTemplate.find(Person.class).all();
		assertThat(people).hasSize(4);
	}

	@Test
	void executableFindShouldWorkAllDomainObjectsProjectedShouldWork() {
		List<OpenProjection> people = this.neo4jTemplate.find(Person.class).as(OpenProjection.class).all();
		assertThat(people).extracting(OpenProjection::getFullName)
			.containsExactlyInAnyOrder("Helge Schnitzel", "Michael Siemons", "Bela B.", "A LA");
	}

	@Test // GH-2270
	void executableFindShouldWorkAllDomainObjectsProjectedDTOShouldWork() {
		List<DtoPersonProjection> people = this.neo4jTemplate.find(Person.class).as(DtoPersonProjection.class).all();
		assertThat(people).extracting(DtoPersonProjection::getLastName)
			.containsExactlyInAnyOrder("Schnitzel", "Siemons", "B.", "LA");
	}

	@Test // GH-2270
	void executableFindShouldWorkOneDomainObjectsProjectedDTOShouldWork() {
		Optional<DtoPersonProjection> person = this.neo4jTemplate.find(Person.class)
			.as(DtoPersonProjection.class)
			.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Schnitzel"))
			.one();
		assertThat(person).map(DtoPersonProjection::getLastName).hasValue("Schnitzel");
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQuery() {
		List<Person> people = this.neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p LIMIT 1").all();
		assertThat(people).hasSize(1);
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndParam() {
		List<Person> people = this.neo4jTemplate.find(Person.class)
			.matching("MATCH (p:Person {lastName: $lastName}) RETURN p",
					Collections.singletonMap("lastName", "Schnitzel"))
			.all();
		assertThat(people).hasSize(1);
	}

	@Test
	void executableFindShouldWorkDomainObjectsWithQueryAndNullParams() {
		List<Person> people = this.neo4jTemplate.find(Person.class)
			.matching("MATCH (p:Person) RETURN p LIMIT 1", null)
			.all();
		assertThat(people).hasSize(1);
	}

	@Test
	void oneShouldWork() {
		Optional<Person> people = this.neo4jTemplate.find(Person.class)
			.matching("MATCH (p:Person) RETURN p LIMIT 1")
			.one();
		assertThat(people).isPresent();
	}

	@Test
	void oneShouldWorkWithIncorrectResultSize() {
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
			.isThrownBy(() -> this.neo4jTemplate.find(Person.class).matching("MATCH (p:Person) RETURN p").one());
	}

	@Test
	void statementShouldWork() {
		Node person = Cypher.node("Person");
		List<Person> people = this.neo4jTemplate.find(Person.class)
			.matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.anonParameter("Siemons")))
				.returning(person)
				.build())
			.all();
		assertThat(people).extracting(Person::getLastName).containsExactly("Siemons");
	}

	@Test
	void statementWithParamsShouldWork() {
		Node person = Cypher.node("Person");
		List<Person> people = this.neo4jTemplate.find(Person.class)
			.matching(Cypher.match(person)
				.where(person.property("lastName").isEqualTo(Cypher.parameter("lastName", "Siemons")))
				.returning(person)
				.build(), Collections.singletonMap("lastName", "Schnitzel"))
			.all();
		assertThat(people).extracting(Person::getLastName).containsExactly("Schnitzel");
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

	public static final class DtoPersonProjection {

		/**
		 * The ID is required in a project that should be saved.
		 */
		private final Long id;

		private String lastName;

		private String firstName;

		DtoPersonProjection(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		public String getLastName() {
			return this.lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public String getFirstName() {
			return this.firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		protected boolean canEqual(final Object other) {
			return other instanceof DtoPersonProjection;
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof DtoPersonProjection)) {
				return false;
			}
			final DtoPersonProjection other = (DtoPersonProjection) o;
			if (!other.canEqual((Object) this)) {
				return false;
			}
			final Object this$id = this.getId();
			final Object other$id = other.getId();
			if (!Objects.equals(this$id, other$id)) {
				return false;
			}
			final Object this$lastName = this.getLastName();
			final Object other$lastName = other.getLastName();
			if (!Objects.equals(this$lastName, other$lastName)) {
				return false;
			}
			final Object this$firstName = this.getFirstName();
			final Object other$firstName = other.getFirstName();
			return Objects.equals(this$firstName, other$firstName);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.getId();
			result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
			final Object $lastName = this.getLastName();
			result = result * PRIME + (($lastName != null) ? $lastName.hashCode() : 43);
			final Object $firstName = this.getFirstName();
			result = result * PRIME + (($firstName != null) ? $firstName.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "Neo4jTemplateIT.DtoPersonProjection(id=" + this.getId() + ", lastName=" + this.getLastName()
					+ ", firstName=" + this.getFirstName() + ")";
		}

	}

	static class X {

	}

	static class Y {

	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override // needed here because there is no implicit registration of entities
					// upfront some methods under test
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}

		@Bean
		BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
			transactionManager.setValidateExistingTransaction(true);
			return transactionManager;
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
