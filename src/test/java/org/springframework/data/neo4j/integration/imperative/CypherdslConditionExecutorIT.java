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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
// tag::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.Neo4jRepository;
// end::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
// tag::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;

// end::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class CypherdslConditionExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Property firstName;
	private final Property lastName;

	@Autowired
	CypherdslConditionExecutorIT() {

		//CHECKSTYLE:OFF
		// tag::sdn-mixins.dynamic-conditions.usage[]
		Node person = Cypher.node("Person").named("person"); // <.>
		Property firstName = person.property("firstName"); // <.>
		Property lastName = person.property("lastName");
		// end::sdn-mixins.dynamic-conditions.usage[]
		//CHECKSTYLE:ON

		this.firstName = firstName;
		this.lastName = lastName;
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
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void findOneShouldWork(@Autowired PersonRepository repository) {

		assertThat(repository.findOne(firstName.eq(Cypher.literalOf("Helge"))))
				.hasValueSatisfying(p -> assertThat(p).extracting(Person::getLastName).isEqualTo("Schneider"));
	}

	@Test
	void findAllShouldWork(@Autowired PersonRepository repository) {

		assertThat(repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B.")))))
				.extracting(Person::getFirstName)
				.containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test
	void sortedFindAllShouldWork(@Autowired PersonRepository repository) {

		assertThat(
				repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
						Sort.by("lastName").descending()
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void sortedFindAllShouldWorkWithParameter(@Autowired PersonRepository repository) {

		// tag::sdn-mixins.dynamic-conditions.usage[]

		assertThat(
				repository.findAll(
						firstName.eq(Cypher.anonParameter("Helge"))
								.or(lastName.eq(Cypher.parameter("someName", "B."))), // <.>
						lastName.descending() // <.>
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
		// end::sdn-mixins.dynamic-conditions.usage[]
	}

	@Test
	void orderedFindAllShouldWork(@Autowired PersonRepository repository) {

		assertThat(
				repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
						Sort.by("lastName").descending()
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllWithoutPredicateShouldWork(@Autowired PersonRepository repository) {

		assertThat(repository.findAll(lastName.descending()))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "B", "A", "Bela");
	}

	@Test
	void pagedFindAllShouldWork(@Autowired PersonRepository repository) {

		Page<Person> people = repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
						PageRequest.of(1, 1, Sort.by("lastName").descending())
				);

		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
		assertThat(people.getTotalElements()).isEqualTo(2);
		assertThat(people)
				.extracting(Person::getFirstName)
				.containsExactly("Bela");
	}

	@Test // GH-2194
	void pagedFindAllShouldWork2(@Autowired PersonRepository repository) {

		Page<Person> people = repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
				PageRequest.of(0, 20, Sort.by("lastName").descending())
		);

		assertThat(people.hasPrevious()).isFalse();
		assertThat(people.hasNext()).isFalse();
		assertThat(people.getTotalElements()).isEqualTo(2);
		assertThat(people)
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void countShouldWork(@Autowired PersonRepository repository) {

		assertThat(repository.count(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B.")))))
				.isEqualTo(2L);
	}

	@Test
	void existsShouldWork(@Autowired PersonRepository repository) {

		assertThat(repository.exists(firstName.eq(Cypher.literalOf("A")))).isTrue();
	}

	@Test // GH-2261
	void standardDerivedFinderMethodsShouldWork(@Autowired ARepositoryWithDerivedFinderMethods repository) {

		assertThat(repository.findByFirstName("Helge")).isNotNull();
	}

	// tag::sdn-mixins.dynamic-conditions.add-mixin[]
	interface PersonRepository extends
			Neo4jRepository<Person, Long>, // <.>
			CypherdslConditionExecutor<Person> { // <.>
	}
	// end::sdn-mixins.dynamic-conditions.add-mixin[]

	interface ARepositoryWithDerivedFinderMethods extends Neo4jRepository<Person, Long>, CypherdslConditionExecutor<Person> {

		Person findByFirstName(String firstName);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
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

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
