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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.EnableBookmarkManagement;
import org.springframework.data.neo4j.annotation.UseBookmark;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Frantisek Hartman
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = BookmarkManagementTests.BookmarkManagementConfiguration.class)
public class BookmarkManagementTests {

	@Autowired private UseBookmarkOnMethodBean useBookmarkOnMethodBean;

	@Autowired private UseBookmarkOnClassBean useBookmarkOnClassBean;

	@Autowired private UseNoBookmarkBean useNoBookmarkBean;

	@Autowired private UseBookmarkWrapperOnClassBean useBookmarkWrapperOnClassBean;

	@Autowired private UseBookmarkWrapperOnMethodBean useBookmarkWrapperOnMethodBean;

	// Bookmark on method
	@Test
	public void givenUseBookmarkOnMethod_whenRun_thenShouldUseBookmarkIsSet() {
		useBookmarkOnMethodBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkOnMethod_whenRun_thenRemoveBookmarkInfoAfterRun() {
		useBookmarkOnMethodBean.runWithBookmark(() -> {});

		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		assertThat(bookmarkInfo).isNull();
	}

	@Test
	public void givenMethodWithoutUseBookmark_whenRun_thenNoBookmarkInfoSet() {
		useBookmarkOnMethodBean.runWithoutBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	// BookmarkWrapper on method
	@Test
	public void givenUseBookmarkWrapperOnMethod_whenRun_thenShouldUseBookmarkIsSet() {
		useBookmarkWrapperOnMethodBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkWrapperOnMethod_whenRun_thenRemoveBookmarkInfoAfterRun() {
		useBookmarkWrapperOnMethodBean.runWithBookmark(() -> {});

		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		assertThat(bookmarkInfo).isNull();
	}

	// Bookmark on class
	@Test
	public void givenUseBookmarkOnClass_whenRunMethod_thenShouldUseBookmarkIsSet() {
		useBookmarkOnClassBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkOnClassAndMethodFalse_whenRunMethod_thenShouldUseBookmarkIsSetToFalse() {
		useBookmarkOnClassBean.runWithoutBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	@Test // DATAGRAPH-1115
	public void givenUseBookmarkWrapperOnClass_whenRunMethod_thenShouldUseBookmarkIsSet() {
		useBookmarkWrapperOnClassBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkWrapperOnClassAndMethodFalse_whenRunMethod_thenShouldUseBookmarkIsSetToFalse() {
		useBookmarkWrapperOnClassBean.runWithoutBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	@Test
	public void givenUseBookmarkWrapperOnClass_whenRun_thenRemoveBookmarkInfoAfterRun() {
		useBookmarkWrapperOnClassBean.runWithBookmark(() -> {});

		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		assertThat(bookmarkInfo).isNull();
	}

	// No Bookmark
	@Test
	public void givenNoBookmark_whenRunMethod_thenShouldUseBookmarkIsSet() {
		useNoBookmarkBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	@Configuration
	@EnableBookmarkManagement
	@EnableTransactionManagement
	static class BookmarkManagementConfiguration {

		@Bean
		public UseBookmarkOnMethodBean testBean() {
			return new UseBookmarkOnMethodBean();
		}

		@Bean
		public UseBookmarkOnClassBean useBookmarkOnClassBean() {
			return new UseBookmarkOnClassBean();
		}

		@Bean
		public UseNoBookmarkBean useNoBookmarkBean() {
			return new UseNoBookmarkBean();
		}

		@Bean
		public UseBookmarkWrapperOnClassBean useBookmarkWrapperOnClassBean() {
			return new UseBookmarkWrapperOnClassBean();
		}

		@Bean
		public BookmarkManager bookmarkManager() {
			return new CaffeineBookmarkManager();
		}

		@Bean
		public UseBookmarkWrapperOnMethodBean useBookmarkWrapperOnMethodBean() {
			return new UseBookmarkWrapperOnMethodBean();
		}
	}

	static class UseBookmarkOnMethodBean {

		@UseBookmark
		public void runWithBookmark(Runnable runnable) {
			runnable.run();
		}

		public void runWithoutBookmark(Runnable runnable) {
			runnable.run();
		}

	}

	@UseBookmark
	static class UseBookmarkOnClassBean {

		public void runWithBookmark(Runnable runnable) {
			runnable.run();
		}

		@UseBookmark(false)
		public void runWithoutBookmark(Runnable runnable) {
			runnable.run();
		}

	}

	@BookmarkWrapper
	static class UseBookmarkWrapperOnClassBean {

		public void runWithBookmark(Runnable runnable) {
			runnable.run();
		}

		@BookmarkWrapper(false)
		public void runWithoutBookmark(Runnable runnable) {
			runnable.run();
		}

	}

	static class UseBookmarkWrapperOnMethodBean {

		@BookmarkWrapper
		public void runWithBookmark(Runnable runnable) {
			runnable.run();
		}

	}

	static class UseNoBookmarkBean {

		public void runWithBookmark(Runnable runnable) {
			runnable.run();
		}
	}

}
