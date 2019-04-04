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

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Transaction;

/**
 * A shim to decouple the retrieval of managed and unmanaged native transaction from Spring infractructure.
 *
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Die Toten Hosen Live - Der Krach der Republik
 */
@FunctionalInterface
@API(status = API.Status.INTERNAL, since = "1.0")
public interface NativeTransactionProvider {

	/**
	 * Retrieves a transaction from the driver.
	 *
	 * @param driver The driver to be used to retrieve transactions from or create new ones
	 * @param databaseName The name of the database to work with
	 * @return A transaction or an empty optional in case a transaction could not be provided
	 */
	Optional<Transaction> retrieveTransaction(Driver driver, String databaseName);
}
