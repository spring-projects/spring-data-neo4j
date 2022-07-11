/*
 * Copyright 2011-2022 the original author or authors.
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
import java.util.Set;
import java.util.function.Supplier;

import org.apiguardian.api.API;
import org.neo4j.driver.Bookmark;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Responsible for storing, updating and retrieving the bookmarks of Neo4j's transaction.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Death Magnetic
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.1.1")
public sealed interface Neo4jBookmarkManager permits AbstractBookmarkManager, NoopBookmarkManager {

	/**
	 * {@return the default bookmark manager}
	 */
	static Neo4jBookmarkManager create() {
		return new DefaultBookmarkManager(null);
	}

	/**
	 * Use this factory method to add supplier of initial "seeding" bookmarks to the transaction managers
	 * <p>
	 * While this class will make sure that the supplier will be accessed in a thread-safe manner,
	 * it is the caller's duty to provide a thread safe supplier (not changing the seed during a call, etc.).
	 *
	 * @param bookmarksSupplier A supplier for seeding bookmarks, can be null. The supplier is free to provide different
	 *                          bookmarks on each call.
	 * @return A bookmark manager
	 */
	static Neo4jBookmarkManager create(@Nullable Supplier<Set<Bookmark>> bookmarksSupplier) {
		return new DefaultBookmarkManager(bookmarksSupplier);
	}

	/**
	 * Use this bookmark manager at your own risk, it will effectively disable any bookmark management by dropping all
	 * bookmarks and never  supplying any. In a cluster you will be at a high risk of experiencing stale reads. In a single
	 * instance it will most likely not make any difference.
	 * <p>
	 * In a cluster this can be a sensible approach only and if only you can tolerate stale reads and are not in danger of
	 * overwriting old data.
	 *
	 * @return A noop bookmark manager, dropping new bookmarks immediately, never supplying bookmarks.
	 * @since 6.1.11
	 */
	@API(status = API.Status.STABLE, since = "6.1.11")
	static Neo4jBookmarkManager noop() {
		return NoopBookmarkManager.INSTANCE;
	}

	/**
	 * No need to introspect this collection ever. The Neo4j driver will together with the cluster figure out which of
	 * the bookmarks is the most recent one.
	 *
	 * @return a collection of currently known bookmarks
	 */
	Collection<Bookmark> getBookmarks();

	/**
	 * Refreshes the bookmark manager with the {@code newBookmarks new bookmarks} received after the last transaction
	 * committed. The collection of {@code usedBookmarks} should be removed from the list of known bookmarks.
	 *
	 * @param usedBookmarks The collection of bookmarks known prior to the end of a transaction
	 * @param newBookmarks The bookmarks received after the end of a transaction
	 * @see #updateBookmarks(Collection, Collection)
	 */
	void updateBookmarks(Collection<Bookmark> usedBookmarks, Collection<Bookmark> newBookmarks);

	/**
	 * A hook for bookmark managers supporting events.
	 *
	 * @param applicationEventPublisher An event publisher. If null, no events will be published.
	 */
	default void setApplicationEventPublisher(@Nullable ApplicationEventPublisher applicationEventPublisher) {
	}
}
