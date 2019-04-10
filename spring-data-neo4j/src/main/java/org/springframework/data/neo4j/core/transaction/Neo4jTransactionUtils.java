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
package org.springframework.data.neo4j.core.transaction;

import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;

import org.neo4j.driver.AccessMode;
import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionParametersTemplate;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.reactive.RxTransaction;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Internal use only.
 */
public final class Neo4jTransactionUtils {

	public static final String DEFAULT_DATABASE_NAME = "";

	/**
	 * The default session uses {@link AccessMode#WRITE} and an empty list of bookmarks.
	 *
	 * @param databaseName The database to use. May be null, which then designates the default database.
	 * @return Session parameters to configure the default session used
	 */
	public static Consumer<SessionParametersTemplate> defaultSessionParameters(@Nullable String databaseName) {
		return t -> {
			t
				.withDefaultAccessMode(AccessMode.WRITE)
				.withBookmarks(Collections.EMPTY_LIST)
				.withDatabase(Optional.ofNullable(databaseName).orElse(DEFAULT_DATABASE_NAME));
		};
	}

	/**
	 * A hook into Springs transaction management to provide managed, native transactions.
	 *
	 * @param driver         The driver that has been used as a synchronization object.
	 * @param targetDatabase The target database
	 * @return An optional containing a managed transaction or an empty optional if the method hasn't been called inside
	 * an ongoing Spring transaction
	 */
	public static Optional<Transaction> retrieveTransaction(final Driver driver, final String targetDatabase) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return Optional.empty();
		}

		// Try existing transaction
		Neo4jConnectionHolder connectionHolder = (Neo4jConnectionHolder) TransactionSynchronizationManager
			.getResource(driver);

		if (connectionHolder != null) {

			return Optional.of(connectionHolder.getTransaction(targetDatabase));
		}

		// Manually create a new synchronization
		connectionHolder = new Neo4jConnectionHolder(targetDatabase,
			driver.session(defaultSessionParameters(targetDatabase)));
		connectionHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager.registerSynchronization(
			new Neo4jSessionSynchronization(connectionHolder, driver));

		TransactionSynchronizationManager.bindResource(driver, connectionHolder);
		return Optional.of(connectionHolder.getTransaction(targetDatabase));
	}

	/**
	 * A stub to retrieve a reactive transaction that may is already synchronized with Springs context.
	 *
	 * @param driver         The driver that has been used as a synchronization object.
	 * @param targetDatabase The target database
	 * @return A Mono containing a managed transaction or an empty Mono if the method hasn't been called inside
	 * an ongoing Spring transaction
	 */
	public static Mono<RxTransaction> retrieveReactiveTransaction(final Driver driver, final String targetDatabase) {

		// TODO Hook into Springs reactive transaction management (Coming 5.2 M2)
		return Mono.empty();
	}

	public static NodeManager retrieveTransactionalNodeManager(NodeManagerFactory nodeManagerFactory) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return nodeManagerFactory.createNodeManager();
		}

		// Try existing transaction
		NodeManagerHolder nodeManagerHolder = (NodeManagerHolder) TransactionSynchronizationManager
			.getResource(nodeManagerFactory);

		if (nodeManagerHolder != null) {

			return nodeManagerHolder.getNodeManager();
		}

		// Manually create a new synchronization
		nodeManagerHolder = new NodeManagerHolder(nodeManagerFactory.createNodeManager());
		nodeManagerHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager.registerSynchronization(
			new NodeManagerSynchronization(nodeManagerHolder, nodeManagerFactory));

		TransactionSynchronizationManager.bindResource(nodeManagerFactory, nodeManagerHolder);

		return nodeManagerHolder.getNodeManager();
	}

	private Neo4jTransactionUtils() {
	}
}
