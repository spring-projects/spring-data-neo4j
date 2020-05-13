/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.opencypherdsl.renderer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.neo4j.opencypherdsl.Statement;

/**
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @since 1.0
 */
enum CypherRenderer implements Renderer {

	INSTANCE;

	private final int STATEMENT_CACHE_SIZE = 128;
	private final LinkedHashMap<Integer, String> renderedStatementCache = new LRUCache<>(STATEMENT_CACHE_SIZE);

	private final ReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock read = lock.readLock();
	private final Lock write = lock.writeLock();

	@Override
	public String render(Statement statement) {

		int key = Objects.hashCode(statement);

		String renderedContent;
		try {
			read.lock();
			renderedContent = renderedStatementCache.get(key);
		} finally {
			read.unlock();
		}

		if (renderedContent == null) {
			try {
				write.lock();

				RenderingVisitor renderingVisitor = new RenderingVisitor();
				statement.accept(renderingVisitor);
				renderedContent = renderingVisitor.getRenderedContent().trim();

				renderedStatementCache.put(key, renderedContent);
			} finally {
				write.unlock();
			}
		}

		return renderedContent;
	}

	private static class LRUCache<K, V> extends LinkedHashMap<K, V> {
		private int cacheSize;

		LRUCache(int cacheSize) {
			super(cacheSize / 4, 0.75f, true);
			this.cacheSize = cacheSize;
		}

		@Override
		protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
			return size() >= cacheSize;
		}
	}
}
