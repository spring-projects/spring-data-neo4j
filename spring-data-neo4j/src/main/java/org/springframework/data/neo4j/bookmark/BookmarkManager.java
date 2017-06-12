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

/**
 * Bookmark manager keeps track of Neo4j transaction bookmarks
 *
 * @author Frantisek Hartman
 * @see org.springframework.data.neo4j.annotation.EnableBookmarkManagement
 */
public interface BookmarkManager {

	/**
	 * Return stored bookmarks
	 *
	 * @return bookmarks
	 */
	Collection<String> getBookmarks();

	/**
	 * Stores bookmark to this manager
	 *
	 * @param bookmark new bookmark to store
	 * @param previous previous bookmarks that are to be replaced by new bookmark, may be empty
	 */
	void storeBookmark(String bookmark, Collection<String> previous);
}
