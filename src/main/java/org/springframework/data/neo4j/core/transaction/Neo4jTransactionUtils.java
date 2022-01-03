/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.core.transaction;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionConfig;
import org.springframework.lang.Nullable;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;

/**
 * Internal use only.
 *
 * @since 6.0
 */
public final class Neo4jTransactionUtils {

	/**
	 * The default session uses {@link AccessMode#WRITE} and an empty list of bookmarks.
	 *
	 * @param databaseName The database to use. May be null, which then designates the default database.
	 * @return Session parameters to configure the default session used
	 */
	public static SessionConfig defaultSessionConfig(@Nullable String databaseName) {
		return sessionConfig(false, Collections.emptyList(), databaseName);
	}

	public static SessionConfig sessionConfig(boolean readOnly, Collection<Bookmark> bookmarks,
			@Nullable String databaseName) {
		SessionConfig.Builder builder = SessionConfig.builder()
				.withDefaultAccessMode(readOnly ? AccessMode.READ : AccessMode.WRITE).withBookmarks(bookmarks);

		if (databaseName != null) {
			builder.withDatabase(databaseName);
		}

		return builder.build();
	}

	/**
	 * Maps a Spring {@link TransactionDefinition transaction definition} to a native Neo4j driver transaction. Only the
	 * default isolation leven ({@link TransactionDefinition#ISOLATION_DEFAULT}) and
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

		int propagationBehavior = definition.getPropagationBehavior();
		if (!(propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRED
				|| propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW)) {
			throw new IllegalTransactionStateException(
					"Neo4jTransactionManager only supports 'required' or 'requires new' propagation.");
		}

		TransactionConfig.Builder builder = TransactionConfig.builder();
		if (definition.getTimeout() > 0) {
			builder = builder.withTimeout(Duration.ofSeconds(definition.getTimeout()));
		}

		return builder.build();
	}

	static boolean namesMapToTheSameDatabase(@Nullable String name1, @Nullable String name2) {
		return Objects.equals(name1, name2);
	}

	static String formatOngoingTxInAnotherDbErrorMessage(String currentDb, String requestedDb) {
		String defaultDatabase = "the default database";
		String _currentDb = currentDb == null ? defaultDatabase : String.format("'%s'", currentDb);
		String _requestedDb = requestedDb == null ? defaultDatabase : String.format("'%s'", requestedDb);

		return String.format("There is already an ongoing Spring transaction for %s, but you request %s", _currentDb,
				_requestedDb);

	}

	private Neo4jTransactionUtils() {}
}
