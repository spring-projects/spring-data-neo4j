/*
 * Copyright 2011-2025 the original author or authors.
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.Bookmark;

/**
 * @author Dmitriy Tverdiakov
 * @author Michael J. Simons
 */
class BookmarkManagerTest {

	@ParameterizedTest
	@ValueSource(classes = {DefaultBookmarkManager.class, ReactiveDefaultBookmarkManager.class})
	void shouldReturnBookmarksCopy(Class<? extends Neo4jBookmarkManager> bookmarkManagerType) throws Exception {

		var manager = newBookmarkManager(bookmarkManagerType);
		var bm1 = Bookmark.from("bookmark 1");
		var initialBookmarks = new HashSet<>(Arrays.asList(bm1, null));
		manager.updateBookmarks(Collections.emptyList(), initialBookmarks);

		var bookmarks = manager.getBookmarks();
		manager.updateBookmarks(initialBookmarks, Set.of(Bookmark.from("bookmark2")));

		assertThat(bookmarks).containsExactly(bm1);
	}

	@ParameterizedTest
	@ValueSource(classes = {DefaultBookmarkManager.class, ReactiveDefaultBookmarkManager.class})
	void shouldReturnUnmodifiableBookmarks(Class<? extends Neo4jBookmarkManager> bookmarkManagerType) throws Exception {

		var manager = newBookmarkManager(bookmarkManagerType);
		var initialBookmarks = Set.of(Bookmark.from("bookmark1"));
		manager.updateBookmarks(Collections.emptyList(), initialBookmarks);
		var bookmarks = manager.getBookmarks();

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> bookmarks.add(Bookmark.from("bookmark 2")));
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> bookmarks.remove(Bookmark.from("bookmark 1")));
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(bookmarks::clear);
	}

	static Neo4jBookmarkManager newBookmarkManager(Class<? extends Neo4jBookmarkManager> type) throws Exception {
		return type.getDeclaredConstructor(Supplier.class).newInstance((Supplier<?>) null);
	}
}
