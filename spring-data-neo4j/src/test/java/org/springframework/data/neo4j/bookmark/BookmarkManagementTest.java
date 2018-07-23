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

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
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
@ContextConfiguration(classes = BookmarkManagementTest.BookmarkManagementConfiguration.class)
public class BookmarkManagementTest {

	@Autowired private UseBookmarkOnMethodBean useBookmarkOnMethodBean;

	@Autowired private UseBookmarkOnClassBean useBookmarkOnClassBean;

	@Test
	public void givenUseBookmarkOnMethod_whenRun_thenShouldUseBookmarkIsSet() throws Exception {
		useBookmarkOnMethodBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkOnMethod_whenRun_thenRemoveBookmarkInfoAfterRun() throws Exception {
		useBookmarkOnMethodBean.runWithBookmark(() -> {});

		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		assertThat(bookmarkInfo).isNull();
	}

	@Test
	public void givenMethodWithoutUseBookmark_whenRun_thenNoBookmarkInfoSet() throws Exception {
		useBookmarkOnMethodBean.runWithoutBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	@Test
	public void givenUseBookmarkOnClass_whenRunMethod_thenShouldUseBookmarkIsSet() throws Exception {
		useBookmarkOnClassBean.runWithBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNotNull();
			assertThat(bookmarkInfo.shouldUseBookmark()).isTrue();
		});
	}

	@Test
	public void givenUseBookmarkOnClassAndMethodFalse_whenRunMethod_thenShouldUseBookmarkIsSetToFalse() throws Exception {
		useBookmarkOnClassBean.runWithoutBookmark(() -> {
			BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
			assertThat(bookmarkInfo).isNull();
		});
	}

	@Test
	public void givenUseBookmarkOnClass_whenRun_thenRemoveBookmarkInfoAfterRun() throws Exception {
		useBookmarkOnClassBean.runWithBookmark(() -> {});

		BookmarkInfo bookmarkInfo = BookmarkSupport.currentBookmarkInfo();
		assertThat(bookmarkInfo).isNull();
	}

	@Configuration
	@EnableBookmarkManagement
	@EnableTransactionManagement()
	static class BookmarkManagementConfiguration {

		@Bean
		public UseBookmarkOnMethodBean testBean() {
			return new UseBookmarkOnMethodBean();
		}

		@Bean
		public UseBookmarkOnClassBean useBookmarkOnClassBean() {
			return new UseBookmarkOnClassBean();
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

}
