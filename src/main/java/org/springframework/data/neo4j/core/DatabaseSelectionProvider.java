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

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * A provider interface that knows in which database repositories or either the reactive or imperative template should
 * work.
 * <p>
 * An instance of a database name provider is only relevant when SDN is used with a Neo4j 4.0+ cluster or server.
 * <p>
 * To select the default database, return an empty optional. If you return a database name, it must not be empty. The
 * empty optional indicates an unset database name on the client, so that the server can decide on the default to use.
 * <p>
 * The provider is asked before any interaction of a repository or template with the cluster or server. That means you
 * can in theory return different database names for each interaction. Be aware that you might end up with no data on
 * queries or data stored to wrong database if you don't pay meticulously attention to the database you interact with.
 *
 * @author Michael J. Simons
 * @soundtrack N.W.A. - Straight Outta Compton
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@FunctionalInterface
public interface DatabaseSelectionProvider {

	/**
	 * @return The selected database me to interact with. Use {@link DatabaseSelection#undecided()} to indicate the
	 *         default database.
	 */
	DatabaseSelection getDatabaseSelection();

	/**
	 * Creates a statically configured database selection provider always selecting the database with the given name
	 * {@code databaseName}.
	 *
	 * @param databaseName The database name to use, must not be null nor empty.
	 * @return A statically configured database name provider.
	 */
	static DatabaseSelectionProvider createStaticDatabaseSelectionProvider(String databaseName) {

		Assert.notNull(databaseName, "The database name must not be null.");
		Assert.hasText(databaseName, "The database name must not be empty.");

		return () -> DatabaseSelection.byName(databaseName);
	}

	/**
	 * A database selection provider always returning the default selection.
	 *
	 * @return A provider for the default database name.
	 */
	static DatabaseSelectionProvider getDefaultSelectionProvider() {

		return DefaultDatabaseSelectionProvider.INSTANCE;
	}
}

enum DefaultDatabaseSelectionProvider implements DatabaseSelectionProvider {
	INSTANCE;

	@Override
	public DatabaseSelection getDatabaseSelection() {
		return DatabaseSelection.undecided();
	}
}
