/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
