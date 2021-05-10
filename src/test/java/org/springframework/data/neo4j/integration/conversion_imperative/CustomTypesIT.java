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
package org.springframework.data.neo4j.integration.conversion_imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionWork;
import org.neo4j.driver.Values;
import org.neo4j.driver.summary.ResultSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.conversion.PersonWithCustomId;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
public class CustomTypesIT {

	protected static Neo4jConnectionSupport neo4jConnectionSupport;

	private final AtomicLong customIdValueGenerator = new AtomicLong();

	private final Driver driver;

	private final Neo4jOperations neo4jOperations;

	private final BookmarkCapture bookmarkCapture;

	@Autowired
	public CustomTypesIT(Driver driver, Neo4jOperations neo4jOperations, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.neo4jOperations = neo4jOperations;
		this.bookmarkCapture = bookmarkCapture;
	}

	TransactionWork<ResultSummary> createPersonWithCustomId(PersonWithCustomId.PersonId assignedId) {

		return tx -> tx.run("CREATE (n:PersonWithCustomId) SET n.id = $id ",
				Values.parameters("id", assignedId.getId())).consume();
	}

	@BeforeEach
	void setupData() {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.writeTransaction(transaction -> {
				transaction.run("MATCH (n) detach delete n").consume();
				transaction.run("CREATE (:CustomTypes{customType:'XYZ'})").consume();
				return null;
			});
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void deleteByCustomId() {

		PersonWithCustomId.PersonId id = new PersonWithCustomId.PersonId(customIdValueGenerator.incrementAndGet());
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.writeTransaction(createPersonWithCustomId(id));
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		assertThat(neo4jOperations.count(PersonWithCustomId.class)).isEqualTo(1L);
		neo4jOperations.deleteById(id, PersonWithCustomId.class);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithCustomId) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void deleteAllByCustomId() {

		List<PersonWithCustomId.PersonId> ids = Stream.generate(customIdValueGenerator::incrementAndGet)
				.map(PersonWithCustomId.PersonId::new)
				.limit(2)
				.collect(Collectors.toList());
		try (
				Session session = driver.session(bookmarkCapture.createSessionConfig());
		) {
			ids.forEach(id -> session.writeTransaction(createPersonWithCustomId(id)));
			bookmarkCapture.seedWith(session.lastBookmark());
		}

		assertThat(neo4jOperations.count(PersonWithCustomId.class)).isEqualTo(2L);
		neo4jOperations.deleteAllById(ids, PersonWithCustomId.class);

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			Result result = session.run("MATCH (p:PersonWithCustomId) return count(p) as count");
			assertThat(result.single().get("count").asLong()).isEqualTo(0);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void findByConvertedCustomType(@Autowired EntityWithCustomTypePropertyRepository repository) {

		assertThat(repository.findByCustomType(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
	}

	@Test
	void findByConvertedCustomTypeWithCustomQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

		assertThat(repository.findByCustomTypeCustomQuery(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
	}

	@Test
	void findByConvertedCustomTypeWithSpELPropertyAccessQuery(
			@Autowired EntityWithCustomTypePropertyRepository repository) {

		assertThat(repository.findByCustomTypeCustomSpELPropertyAccessQuery(ThingWithCustomTypes.CustomType.of("XYZ")))
				.isNotNull();
	}

	@Test
	void findByConvertedCustomTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

		assertThat(repository.findByCustomTypeSpELObjectQuery(ThingWithCustomTypes.CustomType.of("XYZ"))).isNotNull();
	}

	@Test
	void findByConvertedDifferentTypeWithSpELObjectQuery(@Autowired EntityWithCustomTypePropertyRepository repository) {

		assertThat(repository.findByDifferentTypeCustomQuery(ThingWithCustomTypes.DifferentType.of("XYZ"))).isNotNull();
	}

	interface EntityWithCustomTypePropertyRepository extends Neo4jRepository<ThingWithCustomTypes, Long> {

		ThingWithCustomTypes findByCustomType(ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $customType return c")
		ThingWithCustomTypes findByCustomTypeCustomQuery(@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = $differentType return c")
		ThingWithCustomTypes findByDifferentTypeCustomQuery(
				@Param("differentType") ThingWithCustomTypes.DifferentType differentType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType.value} return c")
		ThingWithCustomTypes findByCustomTypeCustomSpELPropertyAccessQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);

		@Query("MATCH (c:CustomTypes) WHERE c.customType = :#{#customType} return c")
		ThingWithCustomTypes findByCustomTypeSpELObjectQuery(
				@Param("customType") ThingWithCustomTypes.CustomType customType);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		@Override
		public Neo4jConversions neo4jConversions() {
			Set<GenericConverter> additionalConverters = new HashSet<>();
			additionalConverters.add(new ThingWithCustomTypes.CustomTypeConverter());
			additionalConverters.add(new ThingWithCustomTypes.DifferentTypeConverter());
			additionalConverters.add(new PersonWithCustomId.CustomPersonIdConverter());

			return new Neo4jConversions(additionalConverters);
		}

		@Override // needed here because there is no implicit registration of entities upfront some methods under test
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(ThingWithCustomTypes.class.getPackage().getName());
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
