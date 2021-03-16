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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.integration.shared.common.Person;
// tag::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.Neo4jRepository;
// end::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
// tag::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;

// end::sdn-mixins.dynamic-conditions.add-mixin[]
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class CypherdslConditionExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;
	private final Node person;
	private final Property firstName;
	private final Property lastName;

	CypherdslConditionExecutorIT(@Autowired Driver driver) {

		this.driver = driver;

		//CHECKSTYLE:OFF
		// tag::sdn-mixins.dynamic-conditions.usage[]
		Node person = Cypher.node("Person").named(Constants.NAME_OF_ROOT_NODE); // <.>
		Property firstName = person.property("firstName"); // <.>
		Property lastName = person.property("lastName");
		// end::sdn-mixins.dynamic-conditions.usage[]
		//CHECKSTYLE:ON

		this.person = person;
		this.firstName = firstName;
		this.lastName = lastName;
	}

	@BeforeAll
	protected static void setupData() {
		try (Transaction transaction = neo4jConnectionSupport.getDriver().session().beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'B', lastName: 'LB'})");
			transaction
					.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schneider'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'})");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.commit();
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

		assertThat(
				repository.findAll(firstName.eq(Cypher.literalOf("Helge")).or(lastName.eq(Cypher.literalOf("B."))),
						PageRequest.of(1, 1, Sort.by("lastName").descending())
				))
				.extracting(Person::getFirstName)
				.containsExactly("B");
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

	// tag::sdn-mixins.dynamic-conditions.add-mixin[]
	interface PersonRepository extends
			Neo4jRepository<Person, Long>, // <.>
			CypherdslConditionExecutor<Person> { // <.>
	}
	// end::sdn-mixins.dynamic-conditions.add-mixin[]

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
