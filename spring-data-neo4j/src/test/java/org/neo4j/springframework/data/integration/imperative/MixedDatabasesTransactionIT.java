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
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.exceptions.DatabaseException;
import org.neo4j.driver.internal.SessionConfig;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jClient;
import org.neo4j.springframework.data.core.transaction.Neo4jTransactionManager;
import org.neo4j.springframework.data.integration.shared.PersonWithAllConstructor;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The goal of this integration tests is to ensure a sensible coexistence of declarative {@link Transactional @Transactional}
 * transaction when the user uses the {@link Neo4jClient} in the same or another database.
 */
@ExtendWith(SpringExtension.class)
@ExtendWith(Neo4jExtension.class)
@ContextConfiguration(classes = MixedDatabasesTransactionIT.Config.class)
public class MixedDatabasesTransactionIT {

	protected static final String DATABASE_NAME = "boom";
	public static final String TEST_QUERY = "MATCH (n:DbTest) RETURN COUNT(n)";

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	private final Neo4jClient neo4jClient;

	private final TransactionTemplate transactionTemplate;

	private final PersonRepository repository;

	@Autowired
	public MixedDatabasesTransactionIT(Driver driver, Neo4jClient neo4jClient,
		Neo4jTransactionManager neo4jTransactionManager,
		PersonRepository repository) {
		this.driver = driver;
		this.neo4jClient = neo4jClient;
		this.transactionTemplate = new TransactionTemplate(neo4jTransactionManager);
		this.repository = repository;
	}

	@BeforeEach
	protected void setupDatabase() {

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("DROP DATABASE " + DATABASE_NAME);
		} catch (DatabaseException e) {
			// Database does probably not exist
		}

		try (Session session = driver.session(SessionConfig.forDatabase("system"))) {
			session.run("CREATE DATABASE " + DATABASE_NAME);
		}

		try (Session session = driver.session(SessionConfig.forDatabase(DATABASE_NAME))) {
			session.run("CREATE (n:DbTest) RETURN n");
		}
	}

	@Test
	void withoutActiveTransactions() {

		Optional<Long> numberOfNodes =
			neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Transactional
	@Test
	void usingTheSameDatabaseDeclarative() {

		Optional<Long> numberOfNodes =
			neo4jClient.query(TEST_QUERY).in(null).fetchAs(Long.class).one();

		assertThat(numberOfNodes).isPresent().hasValue(0L);
	}

	@Test
	void usingSameDatabaseExplizitTx() {
		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(driver, DATABASE_NAME);
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		Optional<Long> numberOfNodes = otherTransactionTemplate.execute(
			tx -> neo4jClient.query(TEST_QUERY).in(DATABASE_NAME).fetchAs(Long.class).one());
		assertThat(numberOfNodes).isPresent().hasValue(1L);
	}

	@Test
	@Transactional
	void usingAnotherDatabaseDeclarative() {

		assertThatIllegalStateException().isThrownBy(
			() -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one())
			.withMessage("There is already an ongoing Spring transaction for the default database, but you request 'boom'");

	}

	@Test
	void usingAnotherDatabaseExplizitTx() {

		assertThatIllegalStateException().isThrownBy(
			() -> transactionTemplate.execute(
				tx -> neo4jClient.query("MATCH (n) RETURN COUNT(n)").in(DATABASE_NAME).fetchAs(Long.class).one()))
			.withMessage("There is already an ongoing Spring transaction for the default database, but you request 'boom'");
	}

	@Test
	void usingAnotherDatabaseDeclarativeFromRepo() {

		Neo4jTransactionManager otherTransactionManger = new Neo4jTransactionManager(driver, DATABASE_NAME);
		TransactionTemplate otherTransactionTemplate = new TransactionTemplate(otherTransactionManger);

		assertThatIllegalStateException().isThrownBy(
			() -> otherTransactionTemplate.execute(
				tx -> repository.save(new PersonWithAllConstructor(null, "Mercury", "Freddie", "Queen", true, 1509L,
					LocalDate.of(1946, 9, 15), null, Collections.emptyList(), null))))
			.withMessage("There is already an ongoing Spring transaction for 'boom', but you request the default database");
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.openConnection();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singletonList(PersonWithAllConstructor.class.getPackage().getName());
		}
	}
}
