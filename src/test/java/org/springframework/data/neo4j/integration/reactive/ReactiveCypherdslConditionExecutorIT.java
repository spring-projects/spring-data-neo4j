/*
 * Copyright 2011-2022 the original author or authors.
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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.ReactiveCypherdslConditionExecutor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import reactor.test.StepVerifier;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@Neo4jIntegrationTest
class ReactiveCypherdslConditionExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final Node person;
	private final Property firstName;
	private final Property lastName;

	ReactiveCypherdslConditionExecutorIT(@Autowired Driver driver) {

		this.driver = driver;

		this.person = Cypher.node("Person").named("person");
		this.firstName = person.property("firstName");
		this.lastName = person.property("lastName");
	}

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
			 Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'B', lastName: 'LB'})");
			transaction
					.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schneider'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'})");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void findOneShouldWork(@Autowired PersonRepository repository) {

		repository.findOne(firstName.eq(Cypher.literalOf("Helge")))
				.as(StepVerifier::create)
				.expectNextMatches(p -> p.getLastName().equals("Schneider"))
				.verifyComplete();
	}

	@Test
	void findAllShouldWork(@Autowired PersonRepository repository) {

		repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))))
				.map(Person::getFirstName)
				.sort()
				.as(StepVerifier::create)
				.expectNext("Bela", "Helge")
				.verifyComplete();
	}

	@Test
	void sortedFindAllShouldWork(@Autowired PersonRepository repository) {

		repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
						Sort.by("lastName").descending()
				)
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "Bela")
				.verifyComplete();
	}

	@Test
	void sortedFindAllShouldWorkWithParameter(@Autowired PersonRepository repository) {

		repository.findAll(
				firstName.eq(Cypher.anonParameter("Helge"))
						.or(lastName.eq(Cypher.parameter("someName", "B."))), // <.>
				lastName.descending() // <.>
		)
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "Bela")
				.verifyComplete();
	}

	@Test
	void orderedFindAllShouldWork(@Autowired PersonRepository repository) {

			repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
					Sort.by("lastName").descending()
			)
					.map(Person::getFirstName)
					.as(StepVerifier::create)
					.expectNext("Helge", "Bela")
					.verifyComplete();
	}

	@Test
	void orderedFindAllWithoutPredicateShouldWork(@Autowired PersonRepository repository) {

		repository.findAll(lastName.descending())
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "B", "A", "Bela")
				.verifyComplete();
	}

	@Test
	void countShouldWork(@Autowired PersonRepository repository) {

		repository.count(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))))
				.as(StepVerifier::create)
				.expectNext(2L)
				.verifyComplete();
	}

	@Test
	void existsShouldWork(@Autowired PersonRepository repository) {

		repository.exists(firstName.eq(Cypher.literalOf("A")))
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
	}

	interface PersonRepository extends ReactiveNeo4jRepository<Person, Long>, ReactiveCypherdslConditionExecutor<Person> {
	}

	@Configuration
	@EnableTransactionManagement
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jReactiveTestConfiguration {

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

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
