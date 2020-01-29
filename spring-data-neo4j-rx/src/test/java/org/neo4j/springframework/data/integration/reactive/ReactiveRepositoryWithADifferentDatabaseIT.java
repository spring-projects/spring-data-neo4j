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
package org.neo4j.springframework.data.integration.reactive;

import static org.neo4j.springframework.data.test.Neo4jExtension.*;

import reactor.core.publisher.Mono;


import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.springframework.data.core.DatabaseSelection;
import org.neo4j.springframework.data.core.ReactiveDatabaseSelectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Michael J. Simons
 */
@Tag(COMMERCIAL_EDITION_ONLY)
@Tag(REQUIRES + "4.0.0")
class ReactiveRepositoryWithADifferentDatabaseIT extends ReactiveRepositoryIT {

	private static final String TEST_DATABASE_NAME = "aTestDatabase";

	@BeforeAll
	static void createTestDatabase() {

		try (Session session = neo4jConnectionSupport.driverInstance.session(SessionConfig.forDatabase("system"))) {

			session.run("CREATE DATABASE " + TEST_DATABASE_NAME).consume();
		}
	}

	@AfterAll
	static void dropTestDatabase() {

		try (Session session = neo4jConnectionSupport.driverInstance.session(SessionConfig.forDatabase("system"))) {

			session.run("DROP DATABASE " + TEST_DATABASE_NAME).consume();
		}
	}

	@Override
	SessionConfig getSessionConfig() {

		return SessionConfig.forDatabase(TEST_DATABASE_NAME);
	}

	@Configuration
	static class ConfigWithDatabaseNameProviderBean extends ReactiveRepositoryIT.Config {

		@Bean
		ReactiveDatabaseSelectionProvider databaseNameProvider() {
			return () -> Mono.just(DatabaseSelection.byName("aTestDatabase"));
		}
	}
}
