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
package org.springframework.data.neo4j.integration.issues.gh2210;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2210IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	static final Long numberA = 1L;
	static final Long numberB = 2L;
	static final Long numberC = 3L;
	static final Long numberD = 4L;

	@BeforeAll
	protected static void setupData(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = neo4jConnectionSupport.getDriver().session(bookmarkCapture.createSessionConfig());
			 Transaction transaction = session.beginTransaction();
		 ) {
			transaction.run("MATCH (n) detach delete n");
			Map<String, Object> params = new HashMap<>();
			params.put("numberA", numberA);
			params.put("numberB", numberB);
			params.put("numberC", numberC);
			params.put("numberD", numberD);
			transaction.run("create (a:SomeEntity {number: $numberA, name: \"A\"})\n"
					+ "create (b:SomeEntity {number: $numberB, name: \"B\"})\n"
					+ "create (c:SomeEntity {number: $numberC, name: \"C\"})\n"
					+ "create (d:SomeEntity {number: $numberD, name: \"D\"})\n"
					+ "create (a) -[:SOME_RELATION_TO {someData: \"d1\"}] -> (b)\n"
					+ "create (b) <-[:SOME_RELATION_TO {someData: \"d2\"}] - (c)\n"
					+ "create (c) <-[:SOME_RELATION_TO {someData: \"d3\"}] - (d)\n"
					+ "return * ", params);
			transaction.commit();
		}
	}

	@Test // GH-2210
	void standardFinderShouldWork(@Autowired Neo4jTemplate template) {

		assertA(template.findById(numberA, SomeEntity.class));

		assertB(template.findById(numberB, SomeEntity.class));

		assertD(template.findById(numberD, SomeEntity.class));
	}

	@Test // GH-2210
	void pathsBasedQueryShouldWork(@Autowired Neo4jTemplate template) {

		String query = "MATCH p = (leaf:SomeEntity {number: $a})-[:SOME_RELATION_TO*]-(:SomeEntity) RETURN leaf, collect(nodes(p)), collect(relationships(p))";
		assertA(template.findOne(query, Collections.singletonMap("a", numberA), SomeEntity.class));

		assertB(template.findOne(query, Collections.singletonMap("a", numberB), SomeEntity.class));

		assertD(template.findOne(query, Collections.singletonMap("a", numberD), SomeEntity.class));
	}

	@Test // GH-2210
	void aPathReturnedShouldPopulateAllNodes(@Autowired Neo4jTemplate template) {

		String query = "MATCH p = (leaf:SomeEntity {number: $a})-[:SOME_RELATION_TO*]-(:SomeEntity) RETURN p";
		assertAll(template.findAll(query, Collections.singletonMap("a", numberA), SomeEntity.class));
	}

	@Test // GH-2210
	void standardFindAllShouldWork(@Autowired Neo4jTemplate template) {

		assertAll(template.findAll(SomeEntity.class));
	}

	void assertAll(List<SomeEntity> entities) {

		assertThat(entities).hasSize(4);
		assertThat(entities).allSatisfy(v -> {
			switch (v.getName()) {
				case "A":
					assertA(Optional.of(v));
					break;
				case "B":
					assertB(Optional.of(v));
					break;
				case "D":
					assertD(Optional.of(v));
					break;
			}
		});
	}

	void assertA(Optional<SomeEntity> a) {

		assertThat(a).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("A");
			assertThat(s.getSomeRelationsOut())
					.hasSize(1)
					.first().satisfies(b -> {
				assertThat(b.getSomeData()).isEqualTo("d1");
				assertThat(b.getTargetPerson().getName()).isEqualTo("B");
				assertThat(b.getTargetPerson().getSomeRelationsOut()).isEmpty();
			});
		});
	}

	void assertD(Optional<SomeEntity> d) {

		assertThat(d).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("D");
			assertThat(s.getSomeRelationsOut())
					.hasSize(1)
					.first().satisfies(c -> {
				assertThat(c.getSomeData()).isEqualTo("d3");
				assertThat(c.getTargetPerson().getName()).isEqualTo("C");
				assertThat(c.getTargetPerson().getSomeRelationsOut())
						.hasSize(1)
						.first().satisfies(b -> {
					assertThat(b.getSomeData()).isEqualTo("d2");
					assertThat(b.getTargetPerson().getName()).isEqualTo("B");
					assertThat(b.getTargetPerson().getSomeRelationsOut()).isEmpty();
				});
			});
		});
	}

	void assertB(Optional<SomeEntity> b) {

		assertThat(b).hasValueSatisfying(s -> {
			assertThat(s.getName()).isEqualTo("B");
			assertThat(s.getSomeRelationsOut()).isEmpty();
		});
	}

	// tag::custom-query.paths.dm[]
	@Node
	static class SomeEntity {

		@Id
		private final Long number;

		private String name;

		@Relationship(type = "SOME_RELATION_TO", direction = Relationship.Direction.OUTGOING)
		private Set<SomeRelation> someRelationsOut = new HashSet<>();
		// end::custom-query.paths.dm[]

		public Long getNumber() {
			return number;
		}

		public String getName() {
			return name;
		}

		public Set<SomeRelation> getSomeRelationsOut() {
			return someRelationsOut;
		}

		SomeEntity(Long number) {
			this.number = number;
		}
		// tag::custom-query.paths.dm[]
	}

	@RelationshipProperties
	static class SomeRelation {

		@Id @GeneratedValue
		private Long id;

		private String someData;

		@TargetNode
		private SomeEntity targetPerson;
		// end::custom-query.paths.dm[]

		public Long getId() {
			return id;
		}

		public String getSomeData() {
			return someData;
		}

		public SomeEntity getTargetPerson() {
			return targetPerson;
		}
		// tag::custom-query.paths.dm[]
	}
	// end::custom-query.paths.dm[]

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions) throws ClassNotFoundException {

			Neo4jMappingContext ctx = new Neo4jMappingContext(neo4JConversions);
			ctx.setInitialEntitySet(new HashSet<>(Arrays.asList(SomeEntity.class, SomeRelation.class)));
			return ctx;
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
