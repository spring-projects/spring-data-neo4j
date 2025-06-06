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
package org.springframework.data.neo4j.test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

import org.neo4j.driver.Bookmark;
import org.neo4j.driver.SessionConfig;

import org.springframework.context.ApplicationListener;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarksUpdatedEvent;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;
import org.springframework.util.StringUtils;

/**
 * This is a utility class that captures the most recent bookmarks after any of the Spring
 * Data Neo4j transaction managers commits a transaction. It also can preload the
 * bookmarks;
 *
 * @author Michael J. Simons
 */
public final class BookmarkCapture implements Supplier<Set<Bookmark>>, ApplicationListener<Neo4jBookmarksUpdatedEvent> {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final Lock read = this.lock.readLock();

	private final Lock write = this.lock.writeLock();

	private final Set<Bookmark> nextBookmarks = new HashSet<>();

	private Set<Bookmark> latestBookmarks;

	public SessionConfig createSessionConfig() {
		return createSessionConfig(null, null);
	}

	public SessionConfig createSessionConfig(String databaseName, String impersonatedUser) {
		try {
			this.read.lock();
			SessionConfig.Builder builder = SessionConfig.builder()
				.withBookmarks((this.latestBookmarks != null) ? this.latestBookmarks : Collections.emptyList());
			if (StringUtils.hasText(databaseName)) {
				builder.withDatabase(databaseName);
			}
			if (Neo4jTransactionUtils.driverSupportsImpersonation() && StringUtils.hasText(impersonatedUser)) {
				Neo4jTransactionUtils.withImpersonatedUser(builder, impersonatedUser);
			}
			return builder.build();
		}
		finally {
			this.read.unlock();
		}
	}

	public void seedWith(Bookmark bookmark) {

		seedWith(List.of(bookmark));
	}

	public void seedWith(Collection<Bookmark> bookmarks) {

		try {
			this.write.lock();
			this.nextBookmarks.addAll(bookmarks);
		}
		finally {
			this.write.unlock();
		}
	}

	@Override
	public void onApplicationEvent(Neo4jBookmarksUpdatedEvent event) {
		try {
			this.write.lock();
			this.latestBookmarks = event.getBookmarks();
			this.nextBookmarks.clear();
		}
		finally {
			this.write.unlock();
		}
	}

	@Override
	public Set<Bookmark> get() {
		try {
			this.read.lock();
			return this.nextBookmarks;
		}
		finally {
			this.read.unlock();
		}
	}

}
