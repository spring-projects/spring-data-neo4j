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

import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.neo4j.driver.v1.AccessMode;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.StatementRunner;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @author Michael J. Simons
 */
@RequiredArgsConstructor
public class DefaultNeo4jStatementRunnerSupplier implements StatementRunnerSupplier<StatementRunner> {

	private final Driver driver;

	@Override
	public StatementRunner get() {

		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return driver.session();
		}

		// Try existing transaction
		Neo4jResourceHolder resourceHolder = (Neo4jResourceHolder) TransactionSynchronizationManager
			.getResource(driver);

		if (resourceHolder != null) {

			return resourceHolder.getTransaction();
		}

		// Manually create a new synchronization
		resourceHolder = new Neo4jResourceHolder(driver.session(AccessMode.WRITE, Collections.emptyList()));

		TransactionSynchronizationManager.registerSynchronization(
			new Neo4jSessionSynchronization(resourceHolder, driver));
		resourceHolder.setSynchronizedWithTransaction(true);
		TransactionSynchronizationManager.bindResource(driver, resourceHolder);

		return resourceHolder.getTransaction();
	}
}
