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

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;
import org.springframework.context.event.ContextRefreshedEvent;

/**
 * Configuration used by @{@link org.springframework.data.neo4j.annotation.EnableBookmarkManagement}
 * <p>
 * Note on bookmark management implementation: The implementation closely follows implementation
 * of @EnableTransactionManagement or @EnableCaching, with simplified pointcut.
 * <p>
 * The bookmark interceptor will set BookmarkInfo thread local when a methods is annotated with @UseBookmark. It is
 * executed before transactional advice (see setOrder(0) ). Neo4j transaction manager then uses {@link BookmarkManager}
 * bean to retrieve currently stored bookmarks and begins new transaction using these bookmarks. After commit new
 * bookmark is stored in the BookmarkManager, replacing the bookmarks used to begin the transaction. The user needs to
 * provide the BookmarkManager bean.
 *
 * @author Frantisek Hartman
 * @author Michael J. Simons
 */
@Configuration
public class BookmarkManagementConfiguration {

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BeanFactoryBookmarkOperationAdvisor bookmarkAdvisor() {
		BeanFactoryBookmarkOperationAdvisor advisor = new BeanFactoryBookmarkOperationAdvisor();
		advisor.setAdvice(bookmarkInterceptor());
		advisor.setOrder(0);
		return advisor;
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BookmarkInterceptor bookmarkInterceptor() {
		return new BookmarkInterceptor();
	}

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public BookmarkManagerContextValidator bookmarkManagerContextValidator(
			ObjectProvider<BookmarkManager> bookmarkManagerProvider) {
		return new BookmarkManagerContextValidator(bookmarkManagerProvider);
	}

	/**
	 * A helper class that asserts the presence of a {@link BookmarkManager} on startup.
	 */
	static class BookmarkManagerContextValidator implements ApplicationListener<ContextRefreshedEvent> {

		private final ObjectProvider<BookmarkManager> bookmarkManagerProvider;

		public BookmarkManagerContextValidator(ObjectProvider<BookmarkManager> bookmarkManagerProvider) {
			this.bookmarkManagerProvider = bookmarkManagerProvider;
		}

		@Override
		public void onApplicationEvent(ContextRefreshedEvent event) {
			try {
				bookmarkManagerProvider.getObject();
			} catch (NoSuchBeanDefinitionException e) {
				throw new IllegalStateException(
						"Bookmark management has been enabled via `@EnableBookmarkManagement` but no bean implementing `org.springframework.data.neo4j.bookmark.BookmarkManager` has been provided.\n"
						+ "Preventing the start of the context as all `@UseBookmark` annotated methods would fail. Please provide a bookmark manager.");
			}
		}
	}
}
