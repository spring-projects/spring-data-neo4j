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
package org.springframework.data.neo4j.integration.lite;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Neo4jIntegrationTest
class LightweightMappingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			// language=cypher
			session.run(
					"CREATE (u1:User {login: 'michael', id: randomUUID()})\n" +
					"CREATE (u2:User {login: 'gerrit', id: randomUUID()})\n" +
					"CREATE (so1:SomeDomainObject {name: 'name1', id: randomUUID()})\n" +
					"CREATE (so2:SomeDomainObject {name: 'name2', id: randomUUID()})\n" +
					"CREATE (so1)<-[:OWNS]-(u1)-[:OWNS]->(so2)\n"
			);
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void getAllFlatShouldWork(@Autowired SomeDomainRepository repository) {

		Collection<MyDTO> dtos = repository.getAllFlat();
		assertThat(dtos).hasSize(10)
				.allSatisfy(dto -> {
					assertThat(dto.counter).isGreaterThan(0);
					assertThat(dto.resyncId).isNotNull();
				});
	}

	@Test
	void getOneFlatShouldWork(@Autowired SomeDomainRepository repository) {

		Optional<MyDTO> dtos = repository.getOneFlat();
		assertThat(dtos).hasValueSatisfying(dto -> {
			assertThat(dto.counter).isEqualTo(4711L);
			assertThat(dto.resyncId).isNotNull();
		});
	}

	@Test
	void getAllNestedShouldWork(@Autowired SomeDomainRepository repository) {

		Collection<MyDTO> dtos = repository.getNestedStuff();
		assertThat(dtos).hasSize(1)
				.first()
				.satisfies(dto -> {
					assertThat(dto.counter).isEqualTo(4711L);
					assertThat(dto.resyncId).isNotNull();
					assertThat(dto.user)
							.isNotNull()
							.extracting(User::getLogin)
							.isEqualTo("michael");
					assertThat(dto.user.getOwnedObjects())
							.hasSize(2);

				});
	}


	@Test
	void getTestedDTOsShouldWork(@Autowired SomeDomainRepository repository) {

		Optional<A> dto = repository.getOneNestedDTO();
		assertThat(dto).hasValueSatisfying(v -> {
			assertThat(v.getOuter()).isEqualTo("av");
			assertThat(v.getNested()).isNotNull()
					.extracting(B::getInner).isEqualTo("bv");
		});

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
