/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.examples.spring_boot;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.springframework.boot.test.autoconfigure.Neo4jTestHarnessAutoConfiguration;
import org.neo4j.springframework.boot.test.autoconfigure.data.DataNeo4jTest;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * This test uses the default configuration, in which the database is statically configured as {@literal "movies"}. In the
 * test setup, we create the database in a {@link org.junit.jupiter.api.BeforeAll @BeforeAll} method.
 *
 * @author Michael J. Simons
 */
@ContextConfiguration(initializers = TestContainerInitializer.class)
@DataNeo4jTest(excludeAutoConfiguration = Neo4jTestHarnessAutoConfiguration.class)
class ExampleUsingStaticDatabaseNameTest {

	@BeforeAll
	static void createDatabase(@Autowired Driver driver) {

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("CREATE DATABASE movies");
		}
	}

	@Autowired
	private PersonRepository repositoryUnderTest;

	@Test
	void storeNewPersonShouldUseTheCorrectDatabase(@Autowired Neo4jClient client) {

		PersonEntity personEntity = new PersonEntity(1929L, "Carlo Pedersoli");
		repositoryUnderTest.save(personEntity).getName();

		// It is easier to test using the Neo4j client, as that one participates in the Spring managed transaction
		// For purity, I would prefer using the driver (as above), but Spring will rollback the transaction
		// in a test before the driver is able to verify the data.
		Optional<String> name = client
			.query("MATCH (p:Person) RETURN p.name")
			.in("movies")
			.fetchAs(String.class).one();
		assertThat(name).hasValue(personEntity.getName());

		// For comparision, in the default database
		// Do to this, we have to end the ongoing test transaction, otherwise SDN-RX will rightfully fail with
		// java.lang.IllegalStateException: There is already an ongoing Spring transaction for 'movies', but you request the default database
		TestTransaction.end();

		name = client
			.query("MATCH (p:Person) RETURN p.name")
			.fetchAs(String.class).one();
		assertThat(name).isEmpty();
	}
}
