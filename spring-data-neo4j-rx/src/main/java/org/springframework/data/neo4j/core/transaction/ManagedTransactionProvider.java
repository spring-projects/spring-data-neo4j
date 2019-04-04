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

import static org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils.*;

import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * A hook into Springs transaction management to provide managed, native transactions.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Die Toten Hosen Live - Der Krach der Republik
 */
public class ManagedTransactionProvider implements NativeTransactionProvider {

	/**
	 * @param databaseName
	 * @param driver The driver whos transactions are bound from withing {@link TransactionSynchronizationManager}.
	 * @return An optional containing a managed transaction or an empty optional if the method hasn't been called inside
	 * an ongoing Spring transaction
	 */
	@Override
	public Optional<Transaction> retrieveTransaction(final Driver driver, String databaseName) {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return Optional.empty();
		}

		// TODO Check existing transaction wether
		// Try existing transaction
		Neo4jConnectionHolder connectionHolder = (Neo4jConnectionHolder) TransactionSynchronizationManager
			.getResource(driver);

		if (connectionHolder != null) {

			return Optional.of(connectionHolder.getTransaction(databaseName));
		}

		// Manually create a new synchronization
		connectionHolder = new Neo4jConnectionHolder(databaseName, driver.session(defaultSessionParameters(databaseName)));
		connectionHolder.setSynchronizedWithTransaction(true);

		TransactionSynchronizationManager.registerSynchronization(
			new Neo4jSessionSynchronization(connectionHolder, driver));

		TransactionSynchronizationManager.bindResource(driver, connectionHolder);
		return Optional.of(connectionHolder.getTransaction(databaseName));
	}
}
