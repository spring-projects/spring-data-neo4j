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

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingAndReturn;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.NamesOnly;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class CypherdslStatementExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

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

	static Statement whoHasFirstName(String name) {
		Node p = Cypher.node("Person").named("p");
		return Cypher.match(p).where(p.property("firstName").isEqualTo(Cypher.anonParameter(name))).returning(p)
				.build();
	}

	// tag::sdn-mixins.using-cypher-dsl-statements.using[]
	static Statement whoHasFirstNameWithAddress(String name) { // <.>
		Node p = Cypher.node("Person").named("p"); // <.>
		Node a = Cypher.anyNode("a");
		Relationship r = p.relationshipTo(a, "LIVES_AT");
		return Cypher.match(r)
				.where(p.property("firstName").isEqualTo(Cypher.anonParameter(name))) // <.>
				.returning(
						p.getRequiredSymbolicName(),
						Functions.collect(r),
						Functions.collect(a)
				)
				.build();
	}
	// end::sdn-mixins.using-cypher-dsl-statements.using[]

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

		Optional<Person> result = repository.findOne(whoHasFirstNameWithAddress("Farin"));
		assertThat(result).isEmpty();
	}

	// tag::sdn-mixins.using-cypher-dsl-statements.using[]

	@Test
	void fineOneShouldWork(@Autowired PersonRepository repository) {

		Optional<Person> result = repository.findOne(whoHasFirstNameWithAddress("Helge"));  // <.>

		assertThat(result).hasValueSatisfying(namesOnly -> {
			assertThat(namesOnly.getFirstName()).isEqualTo("Helge");
			assertThat(namesOnly.getLastName()).isEqualTo("Schneider");
			assertThat(namesOnly.getAddress()).extracting(Person.Address::getCity)
					.isEqualTo("Mülheim an der Ruhr");
		});
	}

	// end::sdn-mixins.using-cypher-dsl-statements.using[]

	@Test
	void fineOneProjectedNoResultShouldWork(@Autowired PersonRepository repository) {

		Optional<NamesOnly> result = repository.findOne(whoHasFirstName("Farin"), NamesOnly.class);
		assertThat(result).isEmpty();
	}

	// tag::sdn-mixins.using-cypher-dsl-statements.using[]
	@Test
	void fineOneProjectedShouldWork(@Autowired PersonRepository repository) {

		Optional<NamesOnly> result = repository.findOne(
				whoHasFirstNameWithAddress("Helge"),
				NamesOnly.class  // <.>
		);

		assertThat(result).hasValueSatisfying(namesOnly -> {
			assertThat(namesOnly.getFirstName()).isEqualTo("Helge");
			assertThat(namesOnly.getLastName()).isEqualTo("Schneider");
			assertThat(namesOnly.getFullName()).isEqualTo("Helge Schneider");
		});
	}
	// end::sdn-mixins.using-cypher-dsl-statements.using[]

	@Test
	void findAllShouldWork(@Autowired PersonRepository repository) {

		Iterable<Person> result = repository.findAll(byCustomQuery());

		assertThat(result)
				.extracting(Person::getFirstName)
				.containsExactly("A", "B", "Bela", "Helge");
		assertThat(result).anySatisfy(p -> assertThat(p.getAddress()).isNotNull());
	}

	@Test
	void findAllProjectedShouldWork(@Autowired PersonRepository repository) {

		Iterable<NamesOnly> result = repository.findAll(byCustomQuery(), NamesOnly.class);

		assertThat(result)
				.extracting(NamesOnly::getFullName)
				.containsExactly("A LA", "B LB", "Bela B.", "Helge Schneider");
	}

	@Test
	void findPageShouldWork(@Autowired PersonRepository repository) {

		Node person = Cypher.node("Person");
		Page<Person> result = repository.findAll(
				byCustomQueryWithoutOrder(), Cypher.match(person).returning(Functions.count(person)).build(),
				PageRequest.of(1, 2, Sort.by("p.firstName").ascending())
		);

		assertThat(result.hasPrevious()).isTrue();
		assertThat(result.hasNext()).isFalse();
		assertThat(result)
				.extracting(Person::getFirstName)
				.containsExactly("Bela", "Helge");
	}

	@Test
	void findPageProjectedShouldWork(@Autowired PersonRepository repository) {

		Node person = Cypher.node("Person");
		Page<NamesOnly> result = repository.findAll(
				byCustomQueryWithoutOrder(), Cypher.match(person).returning(Functions.count(person)).build(),
				PageRequest.of(1, 2, Sort.by("p.firstName").ascending()),
				NamesOnly.class
		);

		assertThat(result.hasPrevious()).isTrue();
		assertThat(result.hasNext()).isFalse();
		assertThat(result)
				.extracting(NamesOnly::getFullName)
				.containsExactly("Bela B.", "Helge Schneider");
	}

	// tag::sdn-mixins.using-cypher-dsl-statements.add-mixin[]
	interface PersonRepository extends
			Neo4jRepository<Person, Long>,
			CypherdslStatementExecutor<Person> {
	}
	// end::sdn-mixins.using-cypher-dsl-statements.add-mixin[]

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

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
	}
}
