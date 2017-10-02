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

import java.io.Serializable;
import java.util.Collection;

/**
 * Bookmark info stored as thread local
 *
 * @author Frantisek Hartman
 */
public class BookmarkInfo implements Serializable {

	private boolean useBookmark;
	private Collection<String> bookmarks;

	public BookmarkInfo() {}

	public BookmarkInfo(boolean useBookmark) {
		this.useBookmark = true;
	}

	public boolean shouldUseBookmark() {
		return useBookmark;
	}

	public void setUseBookmark(boolean useBookmark) {
		this.useBookmark = useBookmark;
	}

	public Collection<String> getBookmarks() {
		return bookmarks;
	}

	public void setBookmarks(Collection<String> bookmarks) {
		this.bookmarks = bookmarks;
	}
}
