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

import org.neo4j.driver.Session;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.ReactiveTransactionManager;
import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.integration.shared.common.NamesOnly;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslStatementExecutor;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@Neo4jIntegrationTest
class ReactiveCypherdslStatementExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final Node person;
	private final Property firstName;
	private final Property lastName;

	ReactiveCypherdslStatementExecutorIT(@Autowired Driver driver) {

		this.driver = driver;

		this.person = Cypher.node("Person").named(Constants.NAME_OF_ROOT_NODE);
		this.firstName = this.person.property("firstName");
		this.lastName = this.person.property("lastName");
	}

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
			Transaction transaction = session.beginTransaction()
		) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'B', lastName: 'LB'})");
			transaction
					.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schneider'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'})");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.commit();
		}
	}

	static Statement whoHasFirstName(String name) {
		Node p = Cypher.node("Person").named("p");
		return Cypher.match(p).where(p.property("firstName").isEqualTo(Cypher.anonParameter(name))).returning(p)
				.build();
	}

	static Statement whoHasFirstNameWithAddress(String name) {
		Node p = Cypher.node("Person").named("p");
		Node a = Cypher.anyNode("a");
		Relationship r = p.relationshipTo(a, "LIVES_AT");
		return Cypher.match(r)
				.where(p.property("firstName").isEqualTo(Cypher.anonParameter(name)))
				.returning(
						p.getRequiredSymbolicName(),
						Functions.collect(r),
						Functions.collect(a)
				)
				.build();
	}

	static Statement byCustomQuery() {
		Node p = Cypher.node("Person").named("p");
		Node a = Cypher.anyNode("a");
		Relationship r = p.relationshipTo(a, "LIVES_AT");
		return Cypher.match(p).optionalMatch(r)
				.returning(
						p.getRequiredSymbolicName(),
						Functions.collect(r),
						Functions.collect(a)
				)
				.orderBy(p.property("firstName").ascending())
				.build();
	}

	static OngoingReadingAndReturn byCustomQueryWithoutOrder() {
		Node p = Cypher.node("Person").named("p");
		Node a = Cypher.anyNode("a");
		Relationship r = p.relationshipTo(a, "LIVES_AT");
		return Cypher.match(p).optionalMatch(r)
				.returning(
						p.getRequiredSymbolicName(),
						Functions.collect(r),
						Functions.collect(a)
				);
	}

	@Test
	void fineOneNoResultShouldWork(@Autowired PersonRepository repository) {

		repository.findOne(whoHasFirstNameWithAddress("Farin"))
				.as(StepVerifier::create)
				.verifyComplete();
	}

	@Test
	void fineOneShouldWork(@Autowired PersonRepository repository) {

		repository.findOne(whoHasFirstNameWithAddress("Helge"))
				.as(StepVerifier::create)
				.expectNextMatches(p -> p.getFirstName().equals("Helge") && p.getLastName().equals("Schneider") &&
										p.getAddress().getCity().equals("Mülheim an der Ruhr"))
				.verifyComplete();
	}

	@Test
	void fineOneProjectedNoResultShouldWork(@Autowired PersonRepository repository) {

		repository.findOne(whoHasFirstName("Farin"), NamesOnly.class)
				.as(StepVerifier::create)
				.verifyComplete();
	}

	@Test
	void fineOneProjectedShouldWork(@Autowired PersonRepository repository) {

		repository.findOne(whoHasFirstName("Helge"), NamesOnly.class)
				.as(StepVerifier::create)
				.expectNextMatches(p -> p.getFullName().equals("Helge Schneider"))
				.verifyComplete();
	}

	@Test
	void findAllShouldWork(@Autowired PersonRepository repository) {

		repository.findAll(byCustomQuery())
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("A", "B", "Bela", "Helge")
				.verifyComplete();
	}

	@Test
	void findAllProjectedShouldWork(@Autowired PersonRepository repository) {

		repository.findAll(byCustomQuery(), NamesOnly.class)
				.map(NamesOnly::getFullName)
				.as(StepVerifier::create)
				.expectNext("A LA", "B LB", "Bela B.", "Helge Schneider")
				.verifyComplete();
	}

	interface PersonRepository extends ReactiveNeo4jRepository<Person, Long>, ReactiveCypherdslStatementExecutor<Person> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
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
