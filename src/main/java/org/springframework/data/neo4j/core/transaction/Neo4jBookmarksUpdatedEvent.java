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
package org.springframework.data.neo4j.core.transaction;

import java.util.Collections;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.driver.Bookmark;
import org.springframework.context.ApplicationEvent;

/**
 * This event will be published after a Neo4j transaction manager physically committed a transaction without errors
 * and reveiced a new set of bookmarks from the cluster.
 *
 * @author Michael J. Simons
 * @soundtrack Black Sabbath - Master Of Reality
 * @since 6.1.1
 */
@API(status = API.Status.STABLE, since = "6.1.1")
public final class Neo4jBookmarksUpdatedEvent extends ApplicationEvent {

	private final Set<Bookmark> bookmarks;

	Neo4jBookmarksUpdatedEvent(Set<Bookmark> bookmarks) {
		super(bookmarks);
		this.bookmarks = bookmarks;
	}

	/**
	 * @return An unmodifiable views of the new bookmarks.
	 */
	public Set<Bookmark> getBookmarks() {

		return Collections.unmodifiableSet(this.bookmarks);
	}
}
