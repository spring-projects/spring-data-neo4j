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
package org.springframework.data.neo4j.core;

import java.util.Objects;

import org.apiguardian.api.API;
import org.springframework.lang.Nullable;

/**
 * A value holder indicating a database selection based on an optional name. {@literal null} indicates to let the server
 * decide.
 *
 * @author Michael J. Simons
 * @soundtrack Rage - Reign Of Fear
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class DatabaseSelection {

	private static final DatabaseSelection DEFAULT_DATABASE_NAME = new DatabaseSelection(null);

	@Nullable private final String value;

	public static DatabaseSelection undecided() {

		return DEFAULT_DATABASE_NAME;
	}

	/**
	 * Create a new database selection by the given databaseName.
	 *
	 * @param databaseName The database name to select the database with.
	 * @return A database selection
	 */
	public static DatabaseSelection byName(String databaseName) {

		return new DatabaseSelection(databaseName);
	}

	private DatabaseSelection(@Nullable String value) {
		this.value = value;
	}

	@Nullable
	public String getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DatabaseSelection that = (DatabaseSelection) o;
		return Objects.equals(value, that.value);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value);
	}
}
