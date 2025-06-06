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

import java.io.Serial;
import java.util.Collections;
import java.util.Set;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Bookmark;

import org.springframework.context.ApplicationEvent;

/**
 * This event will be published after a Neo4j transaction manager physically committed a
 * transaction without errors and received a new set of bookmarks from the cluster.
 *
 * @author Michael J. Simons
 * @since 6.1.1
 */
@API(status = API.Status.STABLE, since = "6.1.1")
public final class Neo4jBookmarksUpdatedEvent extends ApplicationEvent {

	@Serial
	private static final long serialVersionUID = 2143476552056698819L;

	private final transient @Nullable Set<Bookmark> bookmarks;

	Neo4jBookmarksUpdatedEvent(Set<Bookmark> bookmarks) {
		super(bookmarks);
		this.bookmarks = bookmarks;
	}

	/**
	 * Retrieves the set of bookmarks associated with this event.
	 * @return an unmodifiable views of the new bookmarks
	 */
	public Set<Bookmark> getBookmarks() {

		return (this.bookmarks != null) ? Collections.unmodifiableSet(this.bookmarks) : Set.of();
	}

}
