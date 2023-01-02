/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.issues.gh2459;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensure the right mapping from a result containing a mixture of abstract and concrete classes.
 * reference: https://github.com/spring-projects/spring-data-neo4j/issues/2459
 */
@Neo4jIntegrationTest
public class GH2459IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	Driver driver;

	@Autowired
	BookmarkCapture bookmarkCapture;

	@BeforeEach
	void setupData() {
		try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
			session.run("MATCH (n) DETACH DELETE n").consume();
			session.run("" +
					"CREATE(po1:Boy:PetOwner {name: 'Boy1', uuid: '10'})\n" +
					"CREATE(a1:Dog:Animal {name: 'Dog1',uuid: '11'})\n" +
					"CREATE (po1)-[:hasPet]->(a1)\n" +
					"CREATE(a3:Cat:Animal {name: 'Cat1',uuid: '12'})\n" +
					"CREATE (po1)-[:hasPet]->(a3)").consume();
		}
	}

	@Test
	void dontOverrideAbstractMappedData(@Autowired PetOwnerRepository repository) {
		PetOwner petOwner = repository.findById("10").get();
		assertThat(petOwner.pets).hasSize(2);
	}

	interface PetOwnerRepository extends Neo4jRepository<PetOwner, String> {}

	/**
	 * PetOwner
	 */
	@Node("PetOwner")
	public static abstract class PetOwner {
		@Id
		private String uuid;
		@Relationship(type = "hasPet")
		private List<Animal> pets;
	}

	/**
	 * Boy
	 */
	@Node("Boy")
	public static class Boy extends PetOwner {}

	/**
	 * Girl
	 */
	@Node("Girl")
	public static class Girl extends PetOwner {}

	/**
	 * Animal
	 */
	@Node("Animal")
	public static abstract class Animal {
		@Id
		private String uuid;
	}

	/**
	 * Dog
	 */
	@Node("Dog")
	public static class Dog extends Animal {}

	/**
	 * Cat
	 */
	@Node("Cat")
	public static class Cat extends Animal {}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}
	}
}
