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
package org.springframework.data.neo4j.integration.kotlin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.KotlinClub;
import org.springframework.data.neo4j.integration.shared.common.KotlinClubRelationship;
import org.springframework.data.neo4j.integration.shared.common.KotlinPerson;
import org.springframework.data.neo4j.integration.shared.common.KotlinRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension.Neo4jConnectionSupport;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@SpringJUnitConfig(KotlinIT.Config.class)
@Neo4jIntegrationTest
class KotlinIT {

	private final static String PERSON_NAME = "test";

	private static Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	private Driver driver;

	@BeforeEach
	void setup(@Autowired BookmarkCapture bookmarkCapture) {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig());
			 Transaction transaction = session.beginTransaction()
		) {
			transaction.run("MATCH (n) detach delete n").consume();
			transaction.run("CREATE (n:KotlinPerson), "
					+ " (n)-[:WORKS_IN{since: 2019}]->(:KotlinClub{name: 'Golf club'}) SET n.name = $personName",
					Values.parameters("personName", PERSON_NAME))
					.consume();
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test // with addition by DATAGRAPH-1395
	void findAllKotlinPersons(@Autowired KotlinRepository repository) {

		Iterable<KotlinPerson> people = repository.findAll();
		assertThat(people).extracting(KotlinPerson::getName).containsExactly(PERSON_NAME);
		KotlinPerson person = people.iterator().next();
		assertThat(person.getClubs()).extracting(KotlinClubRelationship::getSince).containsExactly(2019);
		assertThat(person.getClubs()).extracting(KotlinClubRelationship::getClub)
				.extracting(KotlinClub::getName).containsExactly("Golf club");
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = KotlinPerson.class)
	@EnableTransactionManagement
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
