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
package org.springframework.boot.autoconfigure.neo4j;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@ContextConfiguration(initializers = Neo4jDriverAutoConfigurationIT.Neo4jContainerBasedTestPropertyProvider.class)
class Neo4jDriverAutoConfigurationIT {

	@Container
	private static Neo4jContainer neo4jServer = new Neo4jContainer<>();

	private final Driver driver;

	@Autowired Neo4jDriverAutoConfigurationIT(Driver driver) {
		this.driver = driver;
	}

	@Test
	void ensureDriverIsOpen() {

		try (
			Session session = driver.session();
			Transaction tx = session.beginTransaction()
		) {
			StatementResult statementResult = tx.run("MATCH (n:Thing) RETURN n LIMIT 1");
			assertThat(statementResult.hasNext()).isFalse();
			tx.success();
		}
	}

	static class Neo4jContainerBasedTestPropertyProvider
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			TestPropertyValues.of(
				"spring.neo4j.uris = " + neo4jServer.getBoltUrl(),
				"spring.neo4j.authentication.username = neo4j",
				"spring.neo4j.authentication.password = " + neo4jServer.getAdminPassword()
			).applyTo(applicationContext.getEnvironment());
		}
	}

	@Configuration
	@ImportAutoConfiguration(Neo4jDriverAutoConfiguration.class)
	static class TestConfiguration {
	}
}
