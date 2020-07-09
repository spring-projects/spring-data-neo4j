/*
 * Copyright 2011-2020 the original author or authors.
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

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Bookmark;

/**
 * @author Gerrit Meier
 */
class Neo4jBookmarkManagerTest {

	private final Neo4jBookmarkManager bookmarkManager = new Neo4jBookmarkManager();

	@Test
	void updatesPreviouslyEmptyBookmarks() {
		BookmarkForTesting bookmark = new BookmarkForTesting(singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), bookmark);

		assertThat(bookmarkManager.getBookmarks()).containsExactly(bookmark);
	}

	@Test
	void returnsUnmodifiableCopyOfBookmarks() {
		BookmarkForTesting bookmark = new BookmarkForTesting(singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), bookmark);

		Collection<Bookmark> bookmarks = bookmarkManager.getBookmarks();
		assertThatThrownBy(() -> bookmarks.remove(bookmark)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void updatesPreviouslySetBookmarks() {
		BookmarkForTesting oldBookmark = new BookmarkForTesting(singleton("a"));
		bookmarkManager.updateBookmarks(new HashSet<>(), oldBookmark);

		BookmarkForTesting newBookmark = new BookmarkForTesting(singleton("b"));
		bookmarkManager.updateBookmarks(singleton(oldBookmark), newBookmark);

		assertThat(bookmarkManager.getBookmarks()).containsExactly(newBookmark);
	}

	@Test
	void updatesPreviouslyUnknownBookmarks() {
		BookmarkForTesting oldBookmark = new BookmarkForTesting(singleton("a"));
		BookmarkForTesting newBookmark = new BookmarkForTesting(singleton("b"));
		bookmarkManager.updateBookmarks(singleton(oldBookmark), newBookmark);

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
