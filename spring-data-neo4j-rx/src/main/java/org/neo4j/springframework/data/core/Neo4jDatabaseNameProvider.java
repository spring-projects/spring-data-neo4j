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
package org.neo4j.springframework.data.core;

import java.util.Optional;

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * A provider interface that knows in which database repositories or either the reactive or imperative template should work.
 * <p>An instance of a database name provider is only relevant when SDN-RX is used with a Neo4j 4.0+ cluster or server.
 * <p>To select the default database, return an empty optional. If you return a database name, it must not be empty.
 * The empty optional indicates an unset database name on the client, so that the server can decide on the default to use.
 * <p>The provider is asked before any interaction of a repository or template with the cluster or server. That means you can
 * in theory return different database names for each interaction. Be aware that you might end up with no data on queries
 * or data stored to wrong database if you don't pay meticulously attention to the database you interact with.
 *
 * @author Michael J. Simons
 * @soundtrack N.W.A. - Straight Outta Compton
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
@FunctionalInterface
public interface Neo4jDatabaseNameProvider {

	/**
	 * @return The optional name of the database name to interact with. Use the empty optional to indicate the default database.
	 */
	Optional<String> getCurrentDatabaseName();

	/**
	 * Creates a statically configured database name provider always answering with the configured {@code databaseName}.
	 *
	 * @param databaseName The database name to use, must not be null nor empty.
	 * @return A statically configured database name provider.
	 */
	static Neo4jDatabaseNameProvider createStaticDatabaseNameProvider(String databaseName) {

		Assert.notNull(databaseName, "The database name must not be null.");
		Assert.hasText(databaseName, "The database name must not be empty.");

		return () -> Optional.of(databaseName);
	}

	/**
	 * A database name provider always returning the empty optional.
	 *
	 * @return A provider for the default database name.
	 */
	static Neo4jDatabaseNameProvider getDefaultDatabaseNameProvider() {

		return DefaultNeo4jDatabaseNameProvider.INSTANCE;
	}
}

enum DefaultNeo4jDatabaseNameProvider implements Neo4jDatabaseNameProvider {
	INSTANCE;

	@Override
	public Optional<String> getCurrentDatabaseName() {
		return Optional.empty();
	}
}
