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
package org.neo4j.springframework.data.core.transaction;

import java.time.Duration;
import java.util.Collections;
import java.util.function.Consumer;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.SessionParametersTemplate;
import org.neo4j.driver.TransactionConfig;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;

/**
 * Internal use only.
 *
 * @since 1.0
 */
public final class Neo4jTransactionUtils {

	/**
	 * The default session uses {@link AccessMode#WRITE} and an empty list of bookmarks.
	 *
	 * @param databaseName The database to use. May be null, which then designates the default database.
	 * @return Session parameters to configure the default session used
	 */
	public static Consumer<SessionParametersTemplate> defaultSessionParameters(@Nullable String databaseName) {
		return (SessionParametersTemplate t) -> {
			t.withDefaultAccessMode(AccessMode.WRITE)
				.withBookmarks(Collections.EMPTY_LIST);

			if (databaseName != null) {
				t.withDatabase(databaseName);
			}
		};
	}

	/**
	 * Maps a Spring {@link TransactionDefinition transaction definition} to a native Neo4j driver transaction.
	 * Only the default isolation leven ({@link TransactionDefinition#ISOLATION_DEFAULT}) and
	 * {@link TransactionDefinition#PROPAGATION_REQUIRED propagation required} behaviour are supported.
	 *
	 * @param definition The transaction definition passed to a Neo4j transaction manager
	 * @return A Neo4j native transaction configuration
	 */
	static TransactionConfig createTransactionConfigFrom(TransactionDefinition definition) {

		if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {
			throw new InvalidIsolationLevelException(
				"Neo4jTransactionManager is not allowed to support custom isolation levels.");
		}

		if (definition.getPropagationBehavior() != TransactionDefinition.PROPAGATION_REQUIRED) {
			throw new IllegalTransactionStateException("Neo4jTransactionManager only supports 'required' propagation.");
		}

		TransactionConfig.Builder builder = TransactionConfig.builder();
		if (definition.getTimeout() > 0) {
			builder = builder.withTimeout(Duration.ofSeconds(definition.getTimeout()));
		}

		return builder.build();
	}

	static boolean namesMapToTheSameDatabase(@Nullable String name1, @Nullable String name2) {

		return name1 == null && name2 == null || (name1 != null && name1.equals(name2));
	}


	private Neo4jTransactionUtils() {
	}
}
