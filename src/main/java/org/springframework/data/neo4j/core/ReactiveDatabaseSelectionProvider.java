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

import reactor.core.publisher.Mono;

import org.apiguardian.api.API;
import org.springframework.util.Assert;

/**
 * This is the reactive version of a the {@link DatabaseSelectionProvider} and it works in the same way but uses
 * reactive return types containing the target database name. An empty mono indicates the default database.
 *
 * @author Michael J. Simons
 * @soundtrack Rage - Reign Of Fear
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@FunctionalInterface
public interface ReactiveDatabaseSelectionProvider {

	/**
	 * @return The selected database to interact with.
	 */
	Mono<DatabaseSelection> getDatabaseSelection();

	/**
	 * Creates a statically configured database selection provider always selecting the database with the given name
	 * {@code databaseName}.
	 *
	 * @param databaseName The database name to use, must not be null nor empty.
	 * @return A statically configured database name provider.
	 */
	static ReactiveDatabaseSelectionProvider createStaticDatabaseSelectionProvider(String databaseName) {

		Assert.notNull(databaseName, "The database name must not be null.");
		Assert.hasText(databaseName, "The database name must not be empty.");

		return () -> Mono.just(DatabaseSelection.byName(databaseName));
	}

	/**
	 * A database selector always selecting the default database.
	 *
	 * @return A provider for the default database name.
	 */
	static ReactiveDatabaseSelectionProvider getDefaultSelectionProvider() {

		return DefaultReactiveDatabaseSelectionProvider.INSTANCE;
	}
}

enum DefaultReactiveDatabaseSelectionProvider implements ReactiveDatabaseSelectionProvider {
	INSTANCE;

	@Override
	public Mono<DatabaseSelection> getDatabaseSelection() {
		return Mono.empty();
	}
}
