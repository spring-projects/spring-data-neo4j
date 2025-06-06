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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.querydsl.core.types.Ops;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.Expressions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Person;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

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
		this.firstNamePath = Expressions.path(String.class, this.personPath, "firstName");
		this.lastNamePath = Expressions.path(String.class, this.personPath, "lastName");
	}

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
				Transaction transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.run("CREATE (p:Person{firstName: 'A', lastName: 'LA'})");
			transaction.run("CREATE (p:Person{firstName: 'B', lastName: 'LB'})");
			transaction.run(
					"CREATE (p:Person{firstName: 'Helge', lastName: 'Schneider'}) -[:LIVES_AT]-> (a:Address {city: 'Mülheim an der Ruhr'})");
			transaction.run("CREATE (p:Person{firstName: 'Bela', lastName: 'B.'})");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test // GH-2343
	void fluentFindOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));
		Person person = repository.findBy(predicate, FetchableFluentQuery::oneValue);

		assertThat(person).isNotNull();
		assertThat(person).extracting(Person::getLastName).isEqualTo("Schneider");
	}

	@Test // GH-2343
	void fluentFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));
		List<Person> people = repository.findBy(predicate, FetchableFluentQuery::all);

		assertThat(people).extracting(Person::getFirstName).containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test // GH-2343
	void fluentFindAllProjectingShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));
		List<Person> people = repository.findBy(predicate, q -> q.project("firstName").all());

		assertThat(people).hasSize(1).first().satisfies(p -> {
			assertThat(p.getFirstName()).isEqualTo("Helge");
			assertThat(p.getId()).isNotNull();

			assertThat(p.getLastName()).isNull();
			assertThat(p.getAddress()).isNull();
		});
	}

	@Test
	@Tag("GH-2726")
	void scrollByExampleWithNoOffset(@Autowired QueryDSLPersonRepository repository) {
		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));

		Window<Person> peopleWindow = repository.findBy(predicate,
				q -> q.limit(1).sortBy(Sort.by("firstName").descending()).scroll(ScrollPosition.offset()));

		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactlyInAnyOrder("Helge");

		assertThat(peopleWindow.isLast()).isFalse();
		assertThat(peopleWindow.hasNext()).isTrue();

		assertThat(peopleWindow.positionAt(peopleWindow.getContent().get(0))).isEqualTo(ScrollPosition.offset(0));
	}

	@Test
	@Tag("GH-2726")
	void scrollByExampleWithOffset(@Autowired QueryDSLPersonRepository repository) {
		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));

		Window<Person> peopleWindow = repository.findBy(predicate,
				q -> q.limit(1).sortBy(Sort.by("firstName").descending()).scroll(ScrollPosition.offset(0)));

		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactlyInAnyOrder("Bela");

		assertThat(peopleWindow.isLast()).isTrue();

		assertThat(peopleWindow.positionAt(peopleWindow.getContent().get(0))).isEqualTo(ScrollPosition.offset(1));
	}

	@Test
	@Tag("GH-2726")
	void scrollByExampleWithContinuingOffset(@Autowired QueryDSLPersonRepository repository) {
		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));

		var firstName = Sort.by("firstName").descending();
		Window<Person> peopleWindow = repository.findBy(predicate,
				q -> q.limit(1).sortBy(firstName).scroll(ScrollPosition.offset()));
		ScrollPosition currentPosition = peopleWindow.positionAt(peopleWindow.getContent().get(0));
		peopleWindow = repository.findBy(predicate, q -> q.limit(1).sortBy(firstName).scroll(currentPosition));

		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactlyInAnyOrder("Bela");

		assertThat(peopleWindow.isLast()).isTrue();
	}

	@Test
	@Tag("GH-2726")
	void scrollByExampleWithKeysetOffset(@Autowired QueryDSLPersonRepository repository) {
		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));

		Window<Person> peopleWindow = repository.findBy(predicate,
				q -> q.sortBy(Sort.by("firstName")).limit(1).scroll(ScrollPosition.keyset()));
		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactly("Bela");

		ScrollPosition currentPosition = peopleWindow.positionAt(peopleWindow.size() - 1);
		peopleWindow = repository.findBy(predicate, q -> q.limit(1).scroll(currentPosition));

		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactlyInAnyOrder("Helge");

		assertThat(peopleWindow.isLast()).isTrue();
	}

	@Test
	@Tag("GH-2726")
	void scrollByExampleWithKeysetOffsetBackward(@Autowired QueryDSLPersonRepository repository) {
		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));

		KeysetScrollPosition startPosition = ScrollPosition.backward(Map.of("lastName", "Schneider"));
		Window<Person> peopleWindow = repository.findBy(predicate,
				q -> q.sortBy(Sort.by("firstName")).limit(1).scroll(startPosition));
		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactly("Helge");

		var nextPos = ScrollPosition.backward(((KeysetScrollPosition) peopleWindow.positionAt(0)).getKeys());

		peopleWindow = repository.findBy(predicate, q -> q.limit(1).scroll(nextPos));

		assertThat(peopleWindow.getContent()).extracting(Person::getFirstName).containsExactlyInAnyOrder("Bela");
	}

	@Test // GH-2343
	void fluentfindAllAsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));

		List<DtoPersonProjection> people = repository.findBy(predicate, q -> q.as(DtoPersonProjection.class).all());
		assertThat(people).hasSize(1).extracting(DtoPersonProjection::getFirstName).first().isEqualTo("Helge");
	}

	@Test // GH-2343
	void fluentStreamShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));
		Stream<Person> people = repository.findBy(predicate, FetchableFluentQuery::stream);

		assertThat(people.map(Person::getFirstName)).containsExactly("Helge");
	}

	@Test // GH-2343
	void fluentStreamProjectingShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));
		Stream<DtoPersonProjection> people = repository.findBy(predicate,
				q -> q.as(DtoPersonProjection.class).stream());

		assertThat(people.map(DtoPersonProjection::getFirstName)).containsExactly("Helge");
	}

	@Test // GH-2343
	void fluentFindFirstShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.TRUE.isTrue();
		Person person = repository.findBy(predicate,
				q -> q.sortBy(Sort.by(Sort.Direction.DESC, "lastName")).firstValue());

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

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));
		Page<Person> people = repository.findBy(predicate,
				q -> q.page(PageRequest.of(1, 1, Sort.by("lastName").ascending())));

		assertThat(people).extracting(Person::getFirstName).containsExactly("Helge");
		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
	}

	@Test // GH-2343
	void fluentExistsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"));
		boolean exists = repository.findBy(predicate, q -> q.exists());

		assertThat(exists).isTrue();
	}

	@Test // GH-2343
	void fluentCountShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));
		long count = repository.findBy(predicate, q -> q.count());

		assertThat(count).isEqualTo(2);
	}

	@Test // GH-2726
	void fluentFindAllWithLimitShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Predicate predicate = Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")));
		List<Person> people = repository.findBy(predicate, q -> q.sortBy(Sort.by("firstName").descending()).limit(1))
			.all();

		assertThat(people).hasSize(1);
		assertThat(people).extracting(Person::getFirstName).containsExactly("Helge");
	}

	@Test
	void findOneShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findOne(Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))))
			.hasValueSatisfying(p -> assertThat(p).extracting(Person::getLastName).isEqualTo("Schneider"));
	}

	@Test
	void findAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B.")))))
			.extracting(Person::getFirstName)
			.containsExactlyInAnyOrder("Bela", "Helge");
	}

	@Test
	void sortedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(
				Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
					.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B."))),
				new OrderSpecifier(Order.DESC, this.lastNamePath)))
			.extracting(Person::getFirstName)
			.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(
				Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
					.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B."))),
				Sort.by("lastName").descending()))
			.extracting(Person::getFirstName)
			.containsExactly("Helge", "Bela");
	}

	@Test
	void orderedFindAllWithoutPredicateShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.findAll(new OrderSpecifier(Order.DESC, this.lastNamePath)))
			.extracting(Person::getFirstName)
			.containsExactly("Helge", "B", "A", "Bela");
	}

	@Test
	void pagedFindAllShouldWork(@Autowired QueryDSLPersonRepository repository) {

		Page<Person> people = repository.findAll(
				Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
					.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B."))),
				PageRequest.of(1, 1, Sort.by("lastName").descending()));

		assertThat(people.hasPrevious()).isTrue();
		assertThat(people.hasNext()).isFalse();
		assertThat(people.getTotalElements()).isEqualTo(2);
		assertThat(people).extracting(Person::getFirstName).containsExactly("Bela");
	}

	@Test // GH-2194
	void pagedFindAllShouldWork2(@Autowired QueryDSLPersonRepository repository) {

		Page<Person> people = repository.findAll(
				Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
					.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B."))),
				PageRequest.of(0, 20, Sort.by("lastName").descending()));

		assertThat(people.hasPrevious()).isFalse();
		assertThat(people.hasNext()).isFalse();
		assertThat(people.getTotalElements()).isEqualTo(2);
		assertThat(people).extracting(Person::getFirstName).containsExactly("Helge", "Bela");
	}

	@Test
	void countShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.count(Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("Helge"))
			.or(Expressions.predicate(Ops.EQ, this.lastNamePath, Expressions.asString("B."))))).isEqualTo(2L);
	}

	@Test
	void existsShouldWork(@Autowired QueryDSLPersonRepository repository) {

		assertThat(repository.exists(Expressions.predicate(Ops.EQ, this.firstNamePath, Expressions.asString("A"))))
			.isTrue();
	}

	// tag::sdn-mixins.dynamic-conditions.add-mixin[]
	interface QueryDSLPersonRepository extends Neo4jRepository<Person, Long>, // <.>
			QuerydslPredicateExecutor<Person> {

		// <.>

	}

	static class DtoPersonProjection {

		private final String firstName;

		DtoPersonProjection(String firstName) {
			this.firstName = firstName;
		}

		String getFirstName() {
			return this.firstName;
		}

	}
	// end::sdn-mixins.dynamic-conditions.add-mixin[]

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
