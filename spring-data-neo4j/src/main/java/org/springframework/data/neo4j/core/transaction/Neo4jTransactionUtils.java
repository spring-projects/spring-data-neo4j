/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.transaction;

import java.util.Collections;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.StatementRunner;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.NodeManagerFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Internal use only.
 */
public final class Neo4jTransactionUtils {

	public static StatementRunner retrieveTransactionalStatementRunner(Driver driver) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return driver.session();
		}

		// Try existing transaction
		Neo4jConnectionHolder connectionHolder = (Neo4jConnectionHolder) TransactionSynchronizationManager
			.getResource(driver);

		if (connectionHolder != null) {

			return connectionHolder.getTransaction();
		}

		// Manually create a new synchronization
		connectionHolder = new Neo4jConnectionHolder(driver.session(AccessMode.WRITE, Collections.emptyList()));
		connectionHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager.registerSynchronization(
			new Neo4jSessionSynchronization(connectionHolder, driver));

		TransactionSynchronizationManager.bindResource(driver, connectionHolder);

		return connectionHolder.getTransaction();
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
