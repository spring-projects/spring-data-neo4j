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
package org.neo4j.springframework.data.core;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 * @soundtrack Dr. Dre - The Chronic
 */
class Neo4jDatabaseNameProviderTest {

	@Test
	void defaultProviderShallDefaultToNullDatabase() {

		assertThat(Neo4jDatabaseNameProvider.getDefaultDatabaseNameProvider().getCurrentDatabaseName()).isEmpty();
	}

	@Nested
	class StaticDatabaseNameProvider {

		@Test
		void databaseNameMustNotBeNull() {

			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jDatabaseNameProvider.createStaticDatabaseNameProvider(null))
				.withMessage("The database name must not be null.");
		}

		@Test
		void databaseNameMustNotBeEmpty() {

			assertThatIllegalArgumentException()
				.isThrownBy(() -> Neo4jDatabaseNameProvider.createStaticDatabaseNameProvider(" \t"))
				.withMessage("The database name must not be empty.");
		}

		@Test
		void shouldReturnConfiguredName() {

			Neo4jDatabaseNameProvider provider = Neo4jDatabaseNameProvider.createStaticDatabaseNameProvider("foobar");
			assertThat(provider.getCurrentDatabaseName()).hasValue("foobar");
		}
	}
}
