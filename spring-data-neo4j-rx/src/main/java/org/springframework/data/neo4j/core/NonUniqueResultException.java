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
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.PersistenceException.IllegalResultSizeException;

/**
 * Throw when a query doesn't return a unique result.
 *
 * @author Michael J. Simons
 * @soundtrack Deichkind - Niveau weshalb warum
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public class NonUniqueResultException extends IllegalResultSizeException {

	public NonUniqueResultException(String query, long actualNumberOfResults) {
		super(1L, actualNumberOfResults, query);
	}
}
