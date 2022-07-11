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

import org.neo4j.driver.Bookmark;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;

/**
 * Default bookmark manager.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - The Last Jazz
 * @since 7.0
 */
final class DefaultBookmarkManager extends AbstractBookmarkManager {

	private final Set<Bookmark> bookmarks = new HashSet<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	private final Supplier<Set<Bookmark>> bookmarksSupplier;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;

	DefaultBookmarkManager(@Nullable Supplier<Set<Bookmark>> bookmarksSupplier) {
		this.bookmarksSupplier = bookmarksSupplier == null ? Collections::emptySet : bookmarksSupplier;
	}

	@Override
	public Collection<Bookmark> getBookmarks() {

		try {
			read.lock();
			HashSet<Bookmark> bookmarksToUse = new HashSet<>(this.bookmarks);
			bookmarksToUse.addAll(bookmarksSupplier.get());
			return Collections.unmodifiableSet(bookmarksToUse);
		} finally {
			read.unlock();
		}
	}

	@Override
	public void updateBookmarks(Collection<Bookmark> usedBookmarks, Collection<Bookmark> newBookmarks) {

		try {
			write.lock();
			bookmarks.removeAll(usedBookmarks);
			bookmarks.addAll(newBookmarks);
			if (applicationEventPublisher != null) {
				applicationEventPublisher.publishEvent(new Neo4jBookmarksUpdatedEvent(new HashSet<>(bookmarks)));
			}
		} finally {
			write.unlock();
		}
	}

	@Override
	public void setApplicationEventPublisher(@Nullable ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}
}
