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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Bookmark;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class Neo4jBookmarkManagerTest {

	@Test // GH-2245
	void publishesNewBookmarks() {

		BookmarkForTesting bookmark = new BookmarkForTesting("a");
		AtomicBoolean asserted = new AtomicBoolean(false);

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();
		bookmarkManager.setApplicationEventPublisher(event -> {
			assertThat(((Neo4jBookmarksUpdatedEvent) event).getBookmarks()).containsExactly(bookmark);
			asserted.set(true);
		});

		bookmarkManager.updateBookmarks(new HashSet<>(), List.of(bookmark));
		assertThat(asserted).isTrue();
	}

	@Test // GH-2245
	void shouldUseSupplier() {

		AtomicBoolean asserted = new AtomicBoolean(false);

		BookmarkForTesting a = new BookmarkForTesting("a");
		BookmarkForTesting b = new BookmarkForTesting("b");

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create(() -> Collections.singleton(a));
		bookmarkManager.setApplicationEventPublisher(event -> {
			assertThat(((Neo4jBookmarksUpdatedEvent) event).getBookmarks()).containsExactly(b);
			asserted.set(true);
		});

		Collection<Bookmark> bookmarks = bookmarkManager.getBookmarks();
		assertThat(bookmarks).containsExactlyInAnyOrder(a);

		bookmarkManager.updateBookmarks(bookmarks, List.of(b));
		assertThat(asserted).isTrue();
	}

	@Test
	void updatesPreviouslyEmptyBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting bookmark = new BookmarkForTesting("a");
		bookmarkManager.updateBookmarks(new HashSet<>(), List.of(bookmark));

		assertThat(bookmarkManager.getBookmarks()).containsExactly(bookmark);
	}

	@Test
	void returnsUnmodifiableCopyOfBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting bookmark = new BookmarkForTesting("a");
		bookmarkManager.updateBookmarks(new HashSet<>(), List.of(bookmark));

		Collection<Bookmark> bookmarks = bookmarkManager.getBookmarks();
		assertThatThrownBy(() -> bookmarks.remove(bookmark)).isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void updatesPreviouslySetBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting oldBookmark = new BookmarkForTesting("a");
		bookmarkManager.updateBookmarks(new HashSet<>(), List.of(oldBookmark));

		BookmarkForTesting newBookmark = new BookmarkForTesting("b");
		bookmarkManager.updateBookmarks(Collections.singleton(oldBookmark), List.of(newBookmark));

		assertThat(bookmarkManager.getBookmarks()).containsExactly(newBookmark);
	}

	@Test
	void updatesPreviouslyUnknownBookmarks() {

		final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.create();

		BookmarkForTesting oldBookmark = new BookmarkForTesting("a");
		BookmarkForTesting newBookmark = new BookmarkForTesting("b");
		bookmarkManager.updateBookmarks(Collections.singleton(oldBookmark), List.of(newBookmark));

		assertThat(bookmarkManager.getBookmarks()).containsExactly(newBookmark);
	}

	@Nested
	class NoopTests {

		@Test
		void shouldAlwaysReturnEmptyList() {

			Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.noop();
			assertThat(bookmarkManager.getBookmarks())
					.isSameAs(Collections.emptyList()) // Might not be that sane to check that but alas
					.isEmpty();
		}


		@Test
		void shouldNeverAcceptBookmarks() {

			BookmarkForTesting bookmark = new BookmarkForTesting("a");
			AtomicBoolean asserted = new AtomicBoolean(false);

			final Neo4jBookmarkManager bookmarkManager = Neo4jBookmarkManager.noop();
			bookmarkManager.setApplicationEventPublisher(event -> {
				assertThat(((Neo4jBookmarksUpdatedEvent) event).getBookmarks()).containsExactly(bookmark);
				asserted.set(true);
			});

			bookmarkManager.updateBookmarks(new HashSet<>(), List.of(bookmark));
			assertThat(asserted).isFalse();
		}
	}
}
