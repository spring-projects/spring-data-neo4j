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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Bookmark;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class Neo4jBookmarkManagerTest {

	@Test // GH-2245
	void publishesNewBookmarks() {

		BookmarkForTesting bookmark = new BookmarkForTesting(Collections.singleton("a"));
		AtomicBoolean asserted = new AtomicBoolean(false);

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();
		bookmarkManager.setApplicationEventPublisher(event -> {
			assertThat(((Neo4jBookmarksUpdatedEvent) event).getBookmarks()).containsExactly(bookmark);
			asserted.set(true);
		});

		bookmarkManager.updateBookmarks(new HashSet<>(), bookmark);
		assertThat(asserted).isTrue();
	}

	@Test // GH-2245
	void shouldUseSupplier() {

		AtomicBoolean asserted = new AtomicBoolean(false);

		BookmarkForTesting a = new BookmarkForTesting(Collections.singleton("a"));
		BookmarkForTesting b = new BookmarkForTesting(Collections.singleton("b"));

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create(() -> Collections.singleton(a));
		bookmarkManager.setApplicationEventPublisher(event -> {
			assertThat(((Neo4jBookmarksUpdatedEvent) event).getBookmarks()).containsExactly(b);
			asserted.set(true);
		});

		Collection<Bookmark> bookmarks = bookmarkManager.getBookmarks();
		assertThat(bookmarks).containsExactlyInAnyOrder(a);

		bookmarkManager.updateBookmarks(bookmarks, b);
		assertThat(asserted).isTrue();
	}

	@Test
	void updatesPreviouslyEmptyBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting bookmark = new BookmarkForTesting(Collections.singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), bookmark);

		assertThat(bookmarkManager.getBookmarks()).containsExactly(bookmark);
	}

	@Test
	void returnsUnmodifiableCopyOfBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting bookmark = new BookmarkForTesting(Collections.singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), bookmark);

		Collection<Bookmark> bookmarks = bookmarkManager.getBookmarks();
		assertThatThrownBy(() -> bookmarks.remove(bookmark)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void updatesPreviouslySetBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting oldBookmark = new BookmarkForTesting(Collections.singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), oldBookmark);

		BookmarkForTesting newBookmark = new BookmarkForTesting(Collections.singleton("b"));
		bookmarkManager.updateBookmarks(Collections.singleton(oldBookmark), newBookmark);

		assertThat(bookmarkManager.getBookmarks()).containsExactly(newBookmark);
	}

	@Test
	void updatesPreviouslyUnknownBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting oldBookmark = new BookmarkForTesting(Collections.singleton("a"));
		BookmarkForTesting newBookmark = new BookmarkForTesting(Collections.singleton("b"));
		bookmarkManager.updateBookmarks(Collections.singleton(oldBookmark), newBookmark);

		assertThat(bookmarkManager.getBookmarks()).containsExactly(newBookmark);
	}

	static private class BookmarkForTesting implements Bookmark {
		private final Set<String> values;

		BookmarkForTesting(Set<String> values) {
			this.values = values;
		}

		@Override
		public Set<String> values() {
			return values;
		}

		@Override
		public boolean isEmpty() {
			return values.isEmpty();
		}
	}
}
