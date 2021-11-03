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
package org.springframework.data.neo4j.core.transaction;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionConfig;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.InvalidIsolationLevelException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.util.ReflectionUtils;

/**
 * Internal use only.
 *
 * @since 6.0
 */
public final class Neo4jTransactionUtils {

	private static final Method WITH_IMPERSONATED_USER
			= ReflectionUtils.findMethod(SessionConfig.Builder.class, "withImpersonatedUser", String.class);

	public static boolean driverSupportsImpersonation() {
		return WITH_IMPERSONATED_USER != null;
	}

	@SuppressWarnings("UnusedReturnValue")
	public static SessionConfig.Builder withImpersonatedUser(SessionConfig.Builder builder, String user) {

		if (driverSupportsImpersonation()) {
			//noinspection ConstantConditions
			return (SessionConfig.Builder) ReflectionUtils.invokeMethod(WITH_IMPERSONATED_USER, builder, user);
		}
		return builder;
	}

	/**
	 * The default session uses {@link AccessMode#WRITE} and an empty list of bookmarks.
	 *
	 * @param databaseSelection The database to use.
	 * @param asUser An impersonated user.
	 * @return Session parameters to configure the default session used
	 */
	public static SessionConfig defaultSessionConfig(DatabaseSelection databaseSelection, UserSelection asUser) {
		return sessionConfig(false, Collections.emptyList(), databaseSelection, asUser);
	}

	public static SessionConfig sessionConfig(boolean readOnly, Collection<Bookmark> bookmarks, DatabaseSelection databaseSelection, UserSelection asUser) {
		SessionConfig.Builder builder = SessionConfig.builder()
				.withDefaultAccessMode(readOnly ? AccessMode.READ : AccessMode.WRITE).withBookmarks(bookmarks);

		if (databaseSelection.getValue() != null) {
			builder.withDatabase(databaseSelection.getValue());
		}

		if (driverSupportsImpersonation() && asUser.getValue() != null) {
			withImpersonatedUser(builder, asUser.getValue());
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

	static String formatOngoingTxInAnotherDbErrorMessage(
			DatabaseSelection currentDb, DatabaseSelection requestedDb,
			UserSelection currentUser, UserSelection requestedUser
	) {
		String defaultDatabase = "the default database";
		String defaultUser = "the default user";

		String _currentDb = currentDb.getValue() == null ? defaultDatabase : String.format("'%s'", currentDb.getValue());
		String _requestedDb = requestedDb.getValue() == null ? defaultDatabase : String.format("'%s'", requestedDb.getValue());

		String _currentUser = currentUser.getValue() == null ? defaultUser : String.format("'%s'", currentUser.getValue());
		String _requestedUser = requestedUser.getValue() == null ? defaultUser : String.format("'%s'", requestedUser.getValue());

		return String.format("There is already an ongoing Spring transaction for %s of %s, but you requested %s of %s", _currentUser, _currentDb,
				_requestedUser, _requestedDb);

	}

	private Neo4jTransactionUtils() {}
}
