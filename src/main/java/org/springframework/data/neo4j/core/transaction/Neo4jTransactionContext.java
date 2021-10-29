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
import java.util.Objects;

import org.neo4j.driver.Bookmark;
import org.springframework.data.neo4j.core.ImpersonatedUser;
import org.springframework.lang.Nullable;

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
	private @Nullable final String databaseName;

	/**
	 * Will be {@literal null} for the default user, non-null for an impersonated user.
	 */
	private @Nullable final ImpersonatedUser impersonatedUser;

	/**
	 * The bookmarks from which that session was started. Maybe empty but never null.
	 */
	private final Collection<Bookmark> bookmarks;

	Neo4jTransactionContext(@Nullable String databaseName, @Nullable ImpersonatedUser impersonatedUser) {

		this(databaseName, impersonatedUser, Collections.emptyList());
	}

	Neo4jTransactionContext(@Nullable String databaseName, @Nullable ImpersonatedUser impersonatedUser, Collection<Bookmark> bookmarks) {
		this.databaseName = databaseName;
		this.impersonatedUser = impersonatedUser;
		this.bookmarks = bookmarks;
	}

	@Nullable
	String getDatabaseName() {
		return databaseName;
	}

	@Nullable
	ImpersonatedUser getImpersonatedUser() {
		return impersonatedUser;
	}

	Collection<Bookmark> getBookmarks() {
		return bookmarks;
	}

	/**
	 * @param inDatabase Target database
	 * @param user A Neo4j user
	 * @return True if the combination of target database and impersonated user is the same in this context as for the given arguments.
	 */
	boolean isForDatabaseAndUser(@Nullable String inDatabase, @Nullable ImpersonatedUser user) {
		return Neo4jTransactionUtils.namesMapToTheSameDatabase(this.getDatabaseName(), inDatabase) && Objects.equals(this.getImpersonatedUser(), user);
	}
}
