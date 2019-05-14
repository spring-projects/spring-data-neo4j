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

/**
 * Shared base class for exceptions during persistence operations of a {@link NodeManager}.
 *
 * @author Michael J. Simons
 * @soundtrack Deichkind - Niveau weshalb warum
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public abstract class PersistenceException extends RuntimeException {

	protected PersistenceException(String message) {
		super(message);
	}

	protected PersistenceException(String message, Throwable cause) {
		super(message, cause);
	}

	abstract static class IllegalResultSizeException extends PersistenceException {

		private final long expectedNumberOfResults;
		private final long actualNumberOfResults;
		private final String query;

		IllegalResultSizeException(long expectedNumberOfResults, long actualNumberOfResults, String query) {

			super(String.format("Expected %d results, got %d", expectedNumberOfResults, actualNumberOfResults));

			this.query = query;
			this.expectedNumberOfResults = expectedNumberOfResults;
			this.actualNumberOfResults = actualNumberOfResults;
		}

		public long getExpectedNumberOfResults() {
			return expectedNumberOfResults;
		}

		public long getActualNumberOfResults() {
			return actualNumberOfResults;
		}

		public String getQuery() {
			return query;
		}
	}
}
