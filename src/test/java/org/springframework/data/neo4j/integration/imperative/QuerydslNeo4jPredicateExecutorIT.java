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

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.transaction.PlatformTransactionManager;
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
class QuerydslNeo4jPredicateExecutorIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Path<Person> personPath;
	private final Path<String> firstNamePath;
	private final Path<String> lastNamePath;

	QuerydslNeo4jPredicateExecutorIT() {
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
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test // GH-2343
	void fluentFindOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		Person person = repository.findBy(predicate, q -> q.oneValue());

		assertThat(person).isNotNull();
		assertThat(person).extracting(Person::getLastName).isEqualTo("Schneider");
	}

	@Test // GH-2343
	void fluentFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		List<Person> people = repository.findBy(predicate, q -> q.all());

		assertThat(people).extracting(Person::getFirstName)
				.containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test // GH-2343
	void fluentFindAllProjectingShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		List<Person> people = repository.findBy(predicate, q -> q.project("firstName").all());

		assertThat(people)
				.hasSize(1)
				.first().satisfies(p -> {
					assertThat(p.getFirstName()).isEqualTo("Helge");
					assertThat(p.getId()).isNotNull();

					assertThat(p.getLastName()).isNull();
					assertThat(p.getAddress()).isNull();
				});
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

	@Test // GH-2343
	void fluentfindAllAsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));

		List<DtoPersonProjection> people = repository.findBy(predicate, q -> q.as(DtoPersonProjection.class).all());
		assertThat(people)
				.hasSize(1)
				.extracting(DtoPersonProjection::getFirstName)
				.first().isEqualTo("Helge");
	}

	@Test // GH-2343
	void fluentStreamShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		Stream<Person> people = repository.findBy(predicate, FluentQuery.FetchableFluentQuery::stream);

		assertThat(people.map(Person::getFirstName)).containsExactly("Helge");
	}

	@Test // GH-2343
	void fluentStreamProjectingShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		Stream<DtoPersonProjection> people = repository.findBy(predicate,
				q -> q.as(DtoPersonProjection.class).stream());

		assertThat(people.map(DtoPersonProjection::getFirstName)).containsExactly("Helge");
	}

	@Test // GH-2343
	void fluentFindFirstShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.TRUE.isTrue();
		Person person = repository.findBy(predicate, q -> q.sortBy(Sort.by(Sort.Direction.DESC, "lastName")).firstValue());

		assertThat(person).isNotNull();
		assertThat(person).extracting(Person::getFirstName).isEqualTo("Helge");
	}

	@Test // GH-2343
	void fluentFindAllWithSortShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.TRUE.isTrue();
		List<Person> people = repository.findBy(predicate,
				q -> q.sortBy(Sort.by(Sort.Direction.DESC, "lastName")).all());

		assertThat(people).extracting(Person::getLastName).containsExactly("Schneider", "LB", "LA", "B.");
	}

	@Test // GH-2343
	void fluentFindAllWithPaginationShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		Page<Person> people = repository.findBy(predicate,
				q -> q.page(PageRequest.of(1, 1, Sort.by("lastName").ascending())));

		assertThat(people).extracting(Person::getFirstName).containsExactly("Helge");
		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
	}

	@Test // GH-2343
	void fluentExistsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"));
		boolean exists = repository.findBy(predicate, q -> q.exists());

		assertThat(exists).isTrue();
	}

	@Test // GH-2343
	void fluentCountShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")));
		long count = repository.findBy(predicate, q -> q.count());

		assertThat(count).isEqualTo(2);
	}

	@Test
	void findOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findOne(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))))
				.hasValueSatisfying(p -> assertThat(p).extracting(Person::getLastName).isEqualTo("Schneider"));
	}

	@Test
	void findAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
				.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")))))
				.extracting(Person::getFirstName)
				.containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test
	void sortedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
						new OrderSpecifier(Order.DESC, lastNamePath)
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
								.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
						Sort.by("lastName").descending()
				))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllWithoutPredicateShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(new OrderSpecifier(Order.DESC, lastNamePath)))
				.extracting(Person::getFirstName)
				.containsExactly("Helge", "B", "A", "Bela");
	}

	@Test
	void pagedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Page<Person> people = repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
						.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
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
	void pagedFindAllShouldWork2(@Autowired QueryDSLPersonRepository repository) {

		Page<Person> people = repository.findAll(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
						.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B."))),
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
	void countShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(
				repository.count(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("Helge"))
						.or(Expressions.predicate(Ops.EQ, lastNamePath, Expressions.asString("B.")))
				))
				.isEqualTo(2L);
	}

	@Test
	void existsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.exists(Expressions.predicate(Ops.EQ, firstNamePath, Expressions.asString("A"))))
				.isTrue();
	}

	// tag::sdn-mixins.dynamic-conditions.add-mixin[]
	interface QueryDSLPersonRepository extends
			Neo4jRepository<Person, Long>, // <.>
			QuerydslPredicateExecutor<Person> { // <.>
	}
	// end::sdn-mixins.dynamic-conditions.add-mixin[]

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
