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
package org.springframework.data.neo4j.bookmark;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Implementation of the bookmark manager using Caffeine cache
 * <p>
 * When using this class you must ensure that caffeine dependency is on your class path.
 *
 * @author Frantisek Hartman
 */
public class CaffeineBookmarkManager implements BookmarkManager {

	private final Cache<String, String> cache;

	public CaffeineBookmarkManager() {
		cache = Caffeine.newBuilder().maximumSize(10_000).expireAfterWrite(1, TimeUnit.MINUTES).build();
	}

	/**
	 * Create instance of {@link CaffeineBookmarkManager} with provided cache
	 *
	 * @param cache cache
	 */
	public CaffeineBookmarkManager(Cache<String, String> cache) {
		this.cache = cache;
	}

	@Override
	public Collection<String> getBookmarks() {
		// return defensive copy
		return new HashSet<>(cache.asMap().values());
	}

	@Override
	public void storeBookmark(String bookmark, Collection<String> previous) {
		// first invalidate previous bookmarks, because bookmark may be in `previous` collection for
		// transaction, which didn't make any changes

		cache.invalidateAll(previous);
		cache.put(bookmark, bookmark);
	}
}
