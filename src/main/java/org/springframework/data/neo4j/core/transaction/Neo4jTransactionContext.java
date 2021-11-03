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
package org.springframework.data.neo4j.core.transaction;

import java.util.Collection;
import java.util.Collections;

import org.neo4j.driver.Bookmark;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;

/**
 * Represents the context in which a transaction has been opened. The context consists primarily of the target database
 * and the set of bookmarks used to start the session from.
 *
 * @author Michael J. Simons
 * @soundtrack Evanescence - Fallen
 * @since 6.0
 */
final class Neo4jTransactionContext {

	/**
	 * The target database of the session.
	 */
	private final DatabaseSelection databaseSelection;

	/**
	 * The impersonated or connected user. Will never be {@literal null}.
	 */
	private final UserSelection userSelection;

	/**
	 * The bookmarks from which that session was started. Maybe empty but never null.
	 */
	private final Collection<Bookmark> bookmarks;

	Neo4jTransactionContext(DatabaseSelection databaseSelection, UserSelection userSelection) {

		this(databaseSelection, userSelection, Collections.emptyList());
	}

	Neo4jTransactionContext(DatabaseSelection databaseSelection, UserSelection userSelection, Collection<Bookmark> bookmarks) {
		this.databaseSelection = databaseSelection;
		this.userSelection = userSelection;
		this.bookmarks = bookmarks;
	}

	DatabaseSelection getDatabaseSelection() {
		return databaseSelection;
	}

	UserSelection getUserSelection() {
		return userSelection;
	}

	Collection<Bookmark> getBookmarks() {
		return bookmarks;
	}

	/**
	 * @param inDatabase Target database
	 * @param asUser A Neo4j user
	 * @return True if the combination of target database and impersonated user is the same in this context as for the given arguments.
	 */
	boolean isForDatabaseAndUser(DatabaseSelection inDatabase, UserSelection asUser) {

		return this.databaseSelection.equals(inDatabase) && this.userSelection.equals(asUser);
	}
}
