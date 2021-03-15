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
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.Expressions;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class QuerydslNeo4jPredicateExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Path<Person> person;
	private final Path<String> firstName;
	private final Path<String> lastName;

	QuerydslNeo4jPredicateExecutorIT() {

		this.person = Expressions.path(Person.class, "n");
		this.firstName = Expressions.path(String.class, person, "firstName");
		this.lastName = Expressions.path(String.class, person, "lastName");
	}

	@BeforeAll
	protected static void setupData() {
		try (Transaction transaction = neo4jConnectionSupport.getDriver().session().beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'B', lastName: 'LB'})");
			transaction.run("CREATE (p:Person{firstName: 'Helge', lastName: 'Schneider'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'})");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.commit();
		}
	}

	@Test
	void findOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findOne(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))))
				.hasValueSatisfying(p -> assertThat(p).extracting(Person::getLastName).isEqualTo("Schneider"));
	}

	@Test
	void findAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastName, Expressions.asString("B.")))))
				.extracting(Person::getFirstName)
				.containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test
	void sortedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.findAll(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastName, Expressions.asString("B."))),
						Sort.by("lastName").descending()
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.findAll(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastName, Expressions.asString("B."))),
						new OrderSpecifier(Order.DESC, lastName)
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllWithoutPredicateShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(new OrderSpecifier(Order.DESC, lastName)))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "B", "A", "Bela");
	}

	@Test
	void pagedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.findAll(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastName, Expressions.asString("B."))),
						PageRequest.of(1, 1, Sort.by("lastName").descending())
				))
				.extracting(Person::getFirstName)
				.containsExactly("B");
	}

	@Test
	void countShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.count(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("Helge"))
						.or(Expressions.predicate(Ops.EQ, lastName, Expressions.asString("B.")))
				))
				.isEqualTo(2L);
	}

	@Test
	void existsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.exists(Expressions.predicate(Ops.EQ, firstName, Expressions.asString("A"))))
				.isTrue();
	}

	interface QueryDSLPersonRepository extends Neo4jRepository<Person, Long>, QuerydslPredicateExecutor<Person> {
	}

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
