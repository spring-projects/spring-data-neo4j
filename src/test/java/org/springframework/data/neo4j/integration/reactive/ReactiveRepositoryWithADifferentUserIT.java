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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assumptions.assumeThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.COMMERCIAL_EDITION_ONLY)
@Tag(Neo4jExtension.REQUIRES + "4.4.0")
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
class ReactiveRepositoryWithADifferentUserIT extends ReactiveRepositoryIT {

	private static final String TEST_USER = "sdn62";
	private static final String TEST_DATABASE_NAME = "sdn62db";

	@BeforeAll
	static void createTestDatabase() {

		assumeThat(Neo4jTransactionUtils.driverSupportsImpersonation()).isTrue();

		try (Session session = neo4jConnectionSupport.getDriver().session(SessionConfig.forDatabase("system"))) {

			session.run("CREATE DATABASE $db", Values.parameters("db", TEST_DATABASE_NAME)).consume();
			session.run("CREATE USER $user SET PASSWORD $password CHANGE NOT REQUIRED SET HOME DATABASE $database",
							Values.parameters("user", TEST_USER, "password", TEST_USER, "database", TEST_DATABASE_NAME))
					.consume();
			session.run("GRANT ROLE publisher TO $user", Values.parameters("user", TEST_USER)).consume();
			session.run("GRANT IMPERSONATE ($targetUser) ON DBMS TO admin", Values.parameters("targetUser", TEST_USER))
					.consume();
		}

		userSelection.set(UserSelection.impersonate(TEST_USER));
	}

	@AfterAll
	static void dropTestDatabase() {

		try (Session session = neo4jConnectionSupport.getDriver().session(SessionConfig.forDatabase("system"))) {

			session.run("REVOKE IMPERSONATE ($targetUser) ON DBMS FROM admin",
					Values.parameters("targetUser", TEST_USER)).consume();
			session.run("DROP USER $user", Values.parameters("user", TEST_USER)).consume();
			session.run("DROP DATABASE $db", Values.parameters("db", TEST_DATABASE_NAME)).consume();
		}

		userSelection.set(UserSelection.connectedUser());
	}
}
