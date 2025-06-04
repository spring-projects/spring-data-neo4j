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
package org.springframework.data.neo4j.core.support;

import java.util.Objects;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;

/**
 * Don't use outside SDN code. You have been warned.
 *
 * @author Michael J. Simons
 */
public final class BookmarkManagerReference implements ApplicationContextAware {

	private final Supplier<Neo4jBookmarkManager> defaultBookmarkManagerSupplier;

	private ObjectProvider<Neo4jBookmarkManager> neo4jBookmarkManagers = new ObjectProvider<>() {
		@Override
		public Neo4jBookmarkManager getObject(Object... args) throws BeansException {
			throw new BeanCreationException("This provider can't create new beans");
		}

		@Override
		@Nullable public Neo4jBookmarkManager getIfAvailable() throws BeansException {
			return null;
		}

		@Override
		@Nullable public Neo4jBookmarkManager getIfUnique() throws BeansException {
			return null;
		}

		@Override
		public Neo4jBookmarkManager getObject() throws BeansException {
			throw new BeanCreationException("This provider can't create new beans");
		}
	};

	@Nullable
	private volatile Neo4jBookmarkManager bookmarkManager;

	@Nullable
	private ApplicationEventPublisher applicationEventPublisher;

	public BookmarkManagerReference(Supplier<Neo4jBookmarkManager> defaultBookmarkManagerSupplier,
			@Nullable Neo4jBookmarkManager bookmarkManager) {
		this.defaultBookmarkManagerSupplier = defaultBookmarkManagerSupplier;
		this.bookmarkManager = bookmarkManager;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		this.neo4jBookmarkManagers = applicationContext.getBeanProvider(Neo4jBookmarkManager.class);
		this.applicationEventPublisher = applicationContext;
		if (this.bookmarkManager != null) {
			Objects.requireNonNull(this.bookmarkManager).setApplicationEventPublisher(this.applicationEventPublisher);
		}
	}

	public Neo4jBookmarkManager resolve() {
		Neo4jBookmarkManager result = this.bookmarkManager;
		if (result == null) {
			synchronized (this) {
				result = this.bookmarkManager;
				if (result == null) {
					this.bookmarkManager = this.neo4jBookmarkManagers
						.getIfAvailable(this.defaultBookmarkManagerSupplier);
					// noinspection DataFlowIssue
					this.bookmarkManager.setApplicationEventPublisher(this.applicationEventPublisher);
					result = this.bookmarkManager;
				}
			}
		}
		return result;
	}

}
