/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.transaction;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.neo4j.driver.Bookmark;

/**
 * Responsible for storing, updating and retrieving the bookmarks of Neo4j's transaction.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Death Magnetic
 * @since 1.0
 */
final class Neo4jBookmarkManager {

	private Set<Bookmark> bookmarks = new HashSet<>();

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	Collection<Bookmark> getBookmarks() {

		try {
			read.lock();
			return Collections.unmodifiableSet(new HashSet<>(bookmarks));
		} finally {
			read.unlock();
		}
	}

	void updateBookmarks(Collection<Bookmark> usedBookmarks, Bookmark lastBookmark) {

		try {
			write.lock();
			bookmarks.removeAll(usedBookmarks);
			bookmarks.add(lastBookmark);
		} finally {
			write.unlock();
		}
	}
}
