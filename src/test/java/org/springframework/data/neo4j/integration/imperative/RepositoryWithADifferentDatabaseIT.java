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
package org.springframework.data.neo4j.integration.imperative;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.test.Neo4jExtension;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.COMMERCIAL_EDITION_ONLY)
@Tag(Neo4jExtension.REQUIRES + "4.0.0")
@Tag(Neo4jExtension.INCOMPATIBLE_WITH_CLUSTERS)
class RepositoryWithADifferentDatabaseIT extends RepositoryIT {

	private static final String TEST_DATABASE_NAME = "aTestDatabase";

	@BeforeAll
	static void createTestDatabase() {

		try (Session session = neo4jConnectionSupport.getDriver().session(SessionConfig.forDatabase("system"))) {

			session.run("CREATE DATABASE " + TEST_DATABASE_NAME).consume();
		}

		databaseSelection.set(DatabaseSelection.byName(TEST_DATABASE_NAME));
	}

	@AfterAll
	static void dropTestDatabase() {

		try (Session session = neo4jConnectionSupport.getDriver().session(SessionConfig.forDatabase("system"))) {

			session.run("DROP DATABASE " + TEST_DATABASE_NAME).consume();
		}

		databaseSelection.set(DatabaseSelection.undecided());
	}
}
