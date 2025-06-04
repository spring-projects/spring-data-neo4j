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
import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.Bookmark;

/**
 * Avoids mocking / spying the thing.
 *
 * @author Michael J. Simons
 */
final class AssertableBookmarkManager extends AbstractBookmarkManager {

	final Map<Collection<Bookmark>, Boolean> updateBookmarksCalled = new HashMap<>();

	boolean getBookmarksCalled = false;

	@Override
	public Collection<Bookmark> getBookmarks() {
		this.getBookmarksCalled = true;
		return Collections.emptyList();
	}

	@Override
	public void updateBookmarks(Collection<Bookmark> usedBookmarks, Collection<Bookmark> newBookmarks) {
		this.updateBookmarksCalled.put(newBookmarks, true);
	}

}
