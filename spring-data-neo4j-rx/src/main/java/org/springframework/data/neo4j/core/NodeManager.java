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
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;
import org.neo4j.driver.v1.Transaction;
import org.springframework.lang.Nullable;

/**
 * Entry point for creating queries that return managed Nodes. The node manager is not supposed to be kept around
 * for longer than necessary. Try to keep your transactions short to avoid memory pressure due to keeping track of
 * managed nodes.
 *
 * @author Michael J. Simons
 */
@API(status = API.Status.STABLE, since = "1.0")
public interface NodeManager {

	/**
	 * Clears all managed entities and flushes any open state to the underlying storage.
	 */
	default void flush() {
	}

	@Nullable
	Transaction getTransaction();

	@API(status = API.Status.EXPERIMENTAL)
	Object executeQuery(String query);
}
