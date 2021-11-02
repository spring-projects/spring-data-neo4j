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

import reactor.test.StepVerifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.querydsl.ReactiveQuerydslPredicateExecutor;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveQuerydslNeo4jPredicateExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Path<Person> personPath;
	private final Path<String> firstNamePath;
	private final Path<String> lastNamePath;

	ReactiveQuerydslNeo4jPredicateExecutorIT() {
		this.personPath = Expressions.path(Person.class, "person");
		this.firstNamePath = Expressions.path(String.class, personPath, "firstName");
		this.lastNamePath = Expressions.path(String.class, personPath, "lastName");
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
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // GH-2361
	void fluentFindOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		repository.findBy(predicate, q -> q.one())
				.map(Person::getLastName)
				.as(StepVerifier::create)
				.expectNext("Schneider")
				.verifyComplete();
	}

	@Test // GH-2361
	void fluentFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		repository.findBy(predicate, q -> q.all())
				.map(Person::getFirstName)
				.sort() // Due to not having something like containsExactlyInAnyOrder
				.as(StepVerifier::create)
				.expectNext("Bela", "Helge")
				.verifyComplete();
	}

	@Test // GH-2361
	void fluentFindAllProjectingShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		repository.findBy(predicate, q -> q.project("firstName").all())
				.as(StepVerifier::create)
				.expectNextMatches(p -> {
					assertThat(p.getFirstName()).isEqualTo("Helge");
					assertThat(p.getId()).isNotNull();

					assertThat(p.getLastName()).isNull();
					assertThat(p.getAddress()).isNull();
					return true;
				})
				.verifyComplete();
	}

	static class DtoPersonProjection {

		private final String firstName;

		DtoPersonProjection(String firstName) {
			this.firstName = firstName;
		}

		public String getFirstName() {
			return firstName;
		}
	}

	@Test // GH-2361
	void fluentfindAllAsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		repository.findBy(predicate, q -> q.as(DtoPersonProjection.class).all())
				.map(DtoPersonProjection::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge")
				.verifyComplete();
	}

	@Test // GH-2361
	void fluentFindFirstShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.TRUE.isTrue();
		repository.findBy(predicate, q -> q.sortBy(Sort.by(Sort.Direction.DESC, "lastName")).first())
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge")
				.verifyComplete();
	}

	@Test // GH-2361
	void fluentFindAllWithSortShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.TRUE.isTrue();
		repository.findBy(predicate, q -> q.sortBy(Sort.by(Sort.Direction.DESC, "lastName")).all())
				.map(Person::getLastName)
				.as(StepVerifier::create)
				.expectNext("Schneider", "LB", "LA", "B.")
				.verifyComplete();
	}

	@Test // GH-2361
	void fluentFindAllWithPaginationShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		repository.findBy(predicate, q -> q.page(PageRequest.of(1, 1, Sort.by("lastName").ascending())))
				.as(StepVerifier::create)
				.expectNextMatches(people -> {

					assertThat(people).extracting(Person::getFirstName).containsExactly("Helge");
					assertThat(people.hasPrevious()).isTrue();
					assertThat(people.hasNext()).isFalse();
					return true;
				}).verifyComplete();
	}

	@Test // GH-2361
	void fluentExistsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		repository.findBy(predicate, q -> q.exists()).as(StepVerifier::create).expectNext(true).verifyComplete();
	}

	@Test // GH-2361
	void fluentCountShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		repository.findBy(predicate, q -> q.count()).as(StepVerifier::create).expectNext(2L).verifyComplete();
	}

	@Test // GH-2361
	void findOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.findOne(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge")))
				.map(Person::getLastName)
				.as(StepVerifier::create)
				.expectNext("Schneider")
				.verifyComplete();
	}

	@Test // GH-2361
	void findAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))))
				.map(Person::getFirstName)
				.sort() // Due to not having something like containsExactlyInAnyOrder
				.as(StepVerifier::create)
				.expectNext("Bela", "Helge")
				.verifyComplete();
	}

	@Test // GH-2361
	void sortedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
						new OrderSpecifier(Order.DESC, lastNamePath)
				)
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "Bela")
				.verifyComplete();
	}

	@Test // GH-2361
	void orderedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
						Sort.by("lastName").descending()
				)
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "Bela")
				.verifyComplete();
	}

	@Test // GH-2361
	void orderedFindAllWithoutPredicateShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.findAll(new OrderSpecifier(Order.DESC, lastNamePath))
				.map(Person::getFirstName)
				.as(StepVerifier::create)
				.expectNext("Helge", "B", "A", "Bela")
				.verifyComplete();
	}

	@Test // GH-2361
	void countShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.count(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
						.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")))
				).as(StepVerifier::create)
				.expectNext(2L)
				.verifyComplete();
	}

	@Test // GH-2361
	void existsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		repository.exists(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("A")))
				.as(StepVerifier::create)
				.expectNext(true)
				.verifyComplete();
	}

	interface QueryDSLPersonRepository extends ReactiveNeo4jRepository<Person, Long>, ReactiveQuerydslPredicateExecutor<Person> {
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
