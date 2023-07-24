/*
 * Copyright 2011-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Bookmark;

/**
 * @author Dmitriy Tverdiakov
 */
class ReactiveDefaultBookmarkManagerTest {
	@Test
	void shouldReturnBookmarksCopy() {
		var manager = new ReactiveDefaultBookmarkManager(null);
		var initialBookmarks = new HashSet<>(Arrays.asList(Bookmark.from("bookmark 1"), null));
		manager.updateBookmarks(Collections.emptyList(), initialBookmarks);

		var bookmarks = manager.getBookmarks();
		manager.updateBookmarks(initialBookmarks, Set.of(Bookmark.from("bookmark2")));

		assertEquals(initialBookmarks, bookmarks);
	}

	@Test
	void shouldReturnUnmodifiableBookmarks() {
		var manager = new ReactiveDefaultBookmarkManager(null);
		var initialBookmarks = Set.of(Bookmark.from("bookmark1"));
		manager.updateBookmarks(Collections.emptyList(), initialBookmarks);
		var bookmarks = manager.getBookmarks();

		assertThrows(UnsupportedOperationException.class, () -> bookmarks.add(Bookmark.from("bookmark 2")));
		assertThrows(UnsupportedOperationException.class, () -> bookmarks.remove(Bookmark.from("bookmark 1")));
		assertThrows(UnsupportedOperationException.class, bookmarks::clear);
	}
}
