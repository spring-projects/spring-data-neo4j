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

import java.util.Collection;
import java.util.Collections;

import org.neo4j.driver.Bookmark;

/**
 * A bookmark manager that drops all bookmarks and never provides any bookmarks.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - The Last Jazz
 * @since 7.0
 */
enum NoopBookmarkManager implements Neo4jBookmarkManager {

	INSTANCE;

	@Override
	public Collection<Bookmark> getBookmarks() {
		return Collections.emptyList();
	}

	@Override
	public void updateBookmarks(Collection<Bookmark> usedBookmarks, Collection<Bookmark> newBookmarks) {
	}
}
