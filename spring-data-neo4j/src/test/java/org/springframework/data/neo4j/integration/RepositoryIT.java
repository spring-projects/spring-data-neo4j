/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RepositoryIT.Config.class)
@Testcontainers
class RepositoryIT {

	@Container
	private static Neo4jContainer neo4jContainer = new Neo4jContainer().withoutAuthentication();

	private final PersonRepository repository;
	private final Driver driver;

	@Autowired
	RepositoryIT(PersonRepository repository, Driver driver) {

		this.repository = repository;
		this.driver = driver;
	}

	@BeforeEach
	void setupDb() {

		Transaction transaction = driver.session().beginTransaction();
		transaction.run("MATCH (n) detach delete n");
		transaction.run("CREATE (n:PersonWithAllConstructor) SET n.name = 'Test'");
		transaction.run("CREATE (n:PersonWithNoConstructor) SET n.name = 'Test'");
		transaction.run("CREATE (n:PersonWithWither) SET n.name = 'Test'");
		transaction.success();
		transaction.close();

	}

	@Test
	void loadAllPersonsWithAllConstructor() {
		List<PersonWithAllConstructor> persons = repository.getAllPersonsViaQuery();

		assertThat(persons).anyMatch(person -> person.getName().equals("Test"));
	}

	@Test
	void loadOnePersonWithAllConstructor() {
		PersonWithAllConstructor person = repository.getOnePersonViaQuery();
		assertThat(person.getName()).isEqualTo("Test");
	}

	@Test
	void loadOptionalPersonWithAllConstructor() {
		Optional<PersonWithAllConstructor> person = repository.getOptionalPersonsViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo("Test");
	}

	@Test
	void loadAllPersonsWithNoConstructor() {
		List<PersonWithNoConstructor> persons = repository.getAllPersonsWithNoConstructorViaQuery();

		assertThat(persons).anyMatch(person -> person.getName().equals("Test"));
	}

	@Test
	void loadOnePersonWithNoConstructor() {
		PersonWithNoConstructor person = repository.getOnePersonWithNoConstructorViaQuery();
		assertThat(person.getName()).isEqualTo("Test");
	}

	@Test
	void loadOptionalPersonWithNoConstructor() {
		Optional<PersonWithNoConstructor> person = repository.getOptionalPersonsWithNoConstructorViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo("Test");
	}

	@Test
	void loadAllPersonsWithWither() {
		List<PersonWithWither> persons = repository.getAllPersonsWithWitherViaQuery();

		assertThat(persons).anyMatch(person -> person.getName().equals("Test"));
	}

	@Test
	void loadOnePersonWithWither() {
		PersonWithWither person = repository.getOnePersonWithWitherViaQuery();
		assertThat(person.getName()).isEqualTo("Test");
	}

	@Test
	void loadOptionalPersonWithWither() {
		Optional<PersonWithWither> person = repository.getOptionalPersonsWithWitherViaQuery();
		assertThat(person).isPresent();
		assertThat(person.get().getName()).isEqualTo("Test");
	}

	@Test
	void repositoryCallFailsBecauseOfUnsupportedOperationException() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findAll());
	}

	@Test
	void callCustomCypher() {
		Long fixedLong = repository.customQuery();
		assertThat(fixedLong).isEqualTo(1L);
	}

	@Test
	void findBySimpleProperty() {
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> repository.findByName("Test"));
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config {

		@Bean
		public Driver driver() {

			String boltUrl = neo4jContainer.getBoltUrl();
			return GraphDatabase.driver(boltUrl, AuthTokens.none());
		}

		@Bean
		public NodeManagerFactory nodeManagerFactory(Driver driver) {

			return new NodeManagerFactory(driver, PersonWithAllConstructor.class, PersonWithNoConstructor.class,
					PersonWithWither.class);
		}

		@Bean
		public PlatformTransactionManager transactionManager(Driver driver) {

			return new Neo4jTransactionManager(driver);
		}
	}
}
