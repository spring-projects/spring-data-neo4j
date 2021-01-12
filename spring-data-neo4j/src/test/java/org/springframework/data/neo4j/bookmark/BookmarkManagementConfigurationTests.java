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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Collection;
import java.util.Collections;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.EnableBookmarkManagement;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
public class BookmarkManagementConfigurationTests {

	@Test // GH-1691
	public void bookmarkManagerShouldBeRequired() {

		assertThatIllegalStateException().isThrownBy(() -> new AnnotationConfigApplicationContext(BookmarkManagementConfiguration.class))
				.withMessageStartingWith("Bookmark management has been enabled via `@EnableBookmarkManagement` but no bean implementing `org.springframework.data.neo4j.bookmark.BookmarkManager` has been provided.");
	}

	@Test // GH-1691
	public void bookmarkManagerShouldBeRecognized() {

		try(ConfigurableApplicationContext ctx = new AnnotationConfigApplicationContext(BookmarkManagementConfiguration.class, BookmarkManagerConfiguration.class)) {
			assertThat(ctx.getBean(BookmarkInterceptor.class)).isNotNull();
			assertThat(ctx.getBean(BeanFactoryBookmarkOperationAdvisor.class)).isNotNull();
			assertThat(ctx.getBean(
					org.springframework.data.neo4j.bookmark.BookmarkManagementConfiguration.BookmarkManagerContextValidator.class)).isNotNull();
		}
	}

	@Configuration
	@EnableBookmarkManagement
	@EnableTransactionManagement
	static class BookmarkManagementConfiguration {
	}

	@Configuration
	static class BookmarkManagerConfiguration {
		@Bean
		public BookmarkManager bookmarkManager() {
			return new BookmarkManager() {
				@Override
				public Collection<String> getBookmarks() {
					return Collections.emptyList();
				}

				@Override
				public void storeBookmark(String bookmark, Collection<String> previous) {
				}
			};
		}
	}
}
