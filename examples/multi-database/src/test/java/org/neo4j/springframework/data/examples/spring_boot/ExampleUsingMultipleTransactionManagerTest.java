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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * This test uses the default configuration, in which the database is statically configured as {@literal "movies"}. In the
 * test setup, we create the database in a {@link BeforeAll @BeforeAll} method.
 *
 * @author Michael J. Simons
 */
@ContextConfiguration(initializers = TestContainerInitializer.class)
@DataNeo4jTest(excludeAutoConfiguration = Neo4jTestHarnessAutoConfiguration.class, includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = Neo4jConfig.class))
@ActiveProfiles("multiple-transaction-manager")
class ExampleUsingMultipleTransactionManagerTest {

	@BeforeAll
	static void createDatabase(@Autowired Driver driver) {

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("CREATE DATABASE movies");
			session.run("CREATE DATABASE otherDb");
		}
	}

	@Autowired
	private PersonRepository repositoryUnderTest;

	/**
	 * The test here already runs {@link org.springframework.transaction.annotation.Transactional} and thus
	 * uses the default transaction manager (the one configured for the movies db).
	 * <p>We inject one for "otherDb" as well for creating a transaction template and do some additional work.
	 *
	 * @param client
	 * @param transactionManagerForOtherDb
	 */
	@Test
	void storeNewPersonShouldUseTheCorrectDatabase(
		@Autowired Neo4jClient client,
		@Autowired @Qualifier("transactionManagerForOtherDb") PlatformTransactionManager transactionManagerForOtherDb
	) {
		PersonEntity personEntity = new PersonEntity(1929, "Carlo Pedersoli");
		repositoryUnderTest.save(personEntity).getName();

		// Verify with the client, for reasoning have a look at ExampleUsingStaticDatabaseNameTest
		Optional<String> name = client
			.query("MATCH (p:Person) RETURN p.name")
			.in("movies")
			.fetchAs(String.class).one();
		assertThat(name).hasValue(personEntity.getName());

		// Build the new transaction template. That would typically be a bean.
		// As we are gonna used that inside an ongoing transaction and the ongoing transaction
		// is already tied to a database, we must spawn a new one with PROPAGATION_REQUIRES_NEW
		TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManagerForOtherDb);
		transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		name = transactionTemplate.execute(t -> client
			.query("MATCH (p:Person) RETURN p.name")
			.in("otherDb")
			.fetchAs(String.class).one());
		assertThat(name).isEmpty();
	}
}
