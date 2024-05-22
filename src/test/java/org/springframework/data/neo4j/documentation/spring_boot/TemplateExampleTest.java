/*
 * Copyright 2011-2024 the original author or authors.
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
package org.springframework.data.neo4j.documentation.spring_boot;

// tag::faq.template-imperative-pt1[]

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;

// end::faq.template-imperative-pt1[]
import org.junit.jupiter.api.BeforeEach;
// tag::faq.template-imperative-pt1[]
import org.junit.jupiter.api.Test;
// end::faq.template-imperative-pt1[]
import org.neo4j.driver.Driver;
// tag::faq.template-imperative-pt1[]
import org.springframework.beans.factory.annotation.Autowired;
// end::faq.template-imperative-pt1[]
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
// tag::faq.template-imperative-pt1[]
import org.springframework.data.neo4j.core.Neo4jTemplate;
// end::faq.template-imperative-pt1[]
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
// tag::faq.template-imperative-pt1[]
import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.data.neo4j.documentation.domain.PersonEntity;
import org.springframework.data.neo4j.documentation.domain.Roles;
// end::faq.template-imperative-pt1[]
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
// tag::faq.template-imperative-pt1[]

// end::faq.template-imperative-pt1[]

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
// tag::faq.template-imperative-pt2[]
public class TemplateExampleTest {

	// end::faq.template-imperative-pt2[]

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	void setup(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (var session = driver.session(bookmarkCapture.createSessionConfig()); var transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n").consume();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	// tag::faq.template-imperative-pt2[]
	@Test
	void shouldSaveAndReadEntities(@Autowired Neo4jTemplate neo4jTemplate) {

		MovieEntity movie = new MovieEntity("The Love Bug",
				"A movie that follows the adventures of Herbie, Herbie's driver, "
						+ "Jim Douglas (Dean Jones), and Jim's love interest, " + "Carole Bennett (Michele Lee)");

		Roles roles1 = new Roles(new PersonEntity(1931, "Dean Jones"), Collections.singletonList("Didi"));
		Roles roles2 = new Roles(new PersonEntity(1942, "Michele Lee"), Collections.singletonList("Michi"));
		movie.getActorsAndRoles().add(roles1);
		movie.getActorsAndRoles().add(roles2);

		MovieEntity result = neo4jTemplate.save(movie);
		// end::mapping.relationship.properties[]
		assertThat(result.getActorsAndRoles()).allSatisfy(relationship -> assertThat(relationship.getId()).isNotNull());
		// tag::mapping.relationship.properties[]

		Optional<PersonEntity> person = neo4jTemplate.findById("Dean Jones", PersonEntity.class);
		assertThat(person).map(PersonEntity::getBorn).hasValue(1931);

		assertThat(neo4jTemplate.count(PersonEntity.class)).isEqualTo(2L);
	}

	// end::faq.template-imperative-pt2[]
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
	// tag::faq.template-imperative-pt2[]
}
// end::faq.template-imperative-pt2[]
