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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
public final class Neo4jBookmarkManager {

	/**
	 * @return A default bookmark manager
	 */
	public static Neo4jBookmarkManager create() {
		return new Neo4jBookmarkManager(null);
	}

	/**
	 * Use this factory method to add supplier of initial "seeding" bookmarks to the transaction managers
	 * <p>
	 * While this class will make sure that the supplier will be accessed in a thread-safe manner,
	 * it is the caller's duty to provide a thread safe supplier (not changing the seed during a call, etc.).
	 * <p>
	 *
	 * @param bookmarksSupplier        A supplier for seeding bookmarks, can be null. The supplier is free to provide different
	 *                                 bookmarks on each call.
	 * @return A bookmark manager
	 */
	public static Neo4jBookmarkManager create(@Nullable Supplier<Set<Bookmark>> bookmarksSupplier) {
		return new Neo4jBookmarkManager(bookmarksSupplier);
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
	public static Neo4jBookmarkManager noop() {
		return new Neo4jBookmarkManager(null, true);
	}

	private final boolean noop;

	private final Set<Bookmark> bookmarks = new HashSet<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	private final Supplier<Set<Bookmark>> bookmarksSupplier;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;

	private Neo4jBookmarkManager(@Nullable Supplier<Set<Bookmark>> bookmarksSupplier) {
		this(bookmarksSupplier, false);
	}

	private Neo4jBookmarkManager(@Nullable Supplier<Set<Bookmark>> bookmarksSupplier, boolean noop) {
		this.bookmarksSupplier = bookmarksSupplier == null ? () -> Collections.emptySet() : bookmarksSupplier;
		this.noop = noop;
	}

	Collection<Bookmark> getBookmarks() {

		if (noop) {
			return Collections.emptyList();
		}

		try {
			read.lock();
			HashSet<Bookmark> bookmarksToUse = new HashSet<>(this.bookmarks);
			bookmarksToUse.addAll(bookmarksSupplier.get());
			return Collections.unmodifiableSet(bookmarksToUse);
		} finally {
			read.unlock();
		}
	}

	void updateBookmarks(Collection<Bookmark> usedBookmarks, Bookmark lastBookmark) {

		if (noop) {
			return;
		}

		try {
			write.lock();
			bookmarks.removeAll(usedBookmarks);
			bookmarks.add(lastBookmark);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new Neo4jBookmarksUpdatedEvent(new HashSet<>(bookmarks)));
			}
		} finally {
			write.unlock();
		}
	}

	void setApplicationEventPublisher(@Nullable ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
