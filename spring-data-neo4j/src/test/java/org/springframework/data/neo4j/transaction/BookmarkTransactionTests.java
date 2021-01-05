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
package org.springframework.data.neo4j.transaction;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Bookmark;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.internal.InternalBookmark;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.neo4j.annotation.EnableBookmarkManagement;
import org.springframework.data.neo4j.bookmark.BookmarkManager;
import org.springframework.data.neo4j.bookmark.CaffeineBookmarkManager;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Frantisek Hartman
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = BookmarkTransactionTests.BookmarkConfiguration.class)
@RunWith(SpringRunner.class)
public class BookmarkTransactionTests {

	@Autowired private BookmarkManager bookmarkManager;

	@Autowired private Driver nativeDriver;

	@Autowired private UserService userService;

	@Test
	public void operationsShouldStoreBookmarkAndUseBookmarkShouldReuseBookmark() throws Exception {

		ArgumentCaptor<SessionConfig> configArgumentCaptor = ArgumentCaptor.forClass(SessionConfig.class);

		userService.saveWithTxAnnotationOnInterface(new User());

		Collection<String> bookmarks = bookmarkManager.getBookmarks();
		assertThat(bookmarks).isNotEmpty();

		@SuppressWarnings("unused")
		Collection<User> users = userService.getAllUsersWithBookmark();

		Mockito.verify(nativeDriver, times(2)).session(configArgumentCaptor.capture());
		List<Bookmark> bookmarksInDriver = configArgumentCaptor.getAllValues().stream()
				.flatMap(sc -> StreamSupport.stream(sc.bookmarks().spliterator(), false))
				.collect(Collectors.toList());

		assertThat(bookmarksInDriver).containsAll(
				bookmarks.stream().map(InternalBookmark::parse).collect(Collectors.toList()));

	}

	@Test
	public void operationsShouldReplaceOlderBookmarks() throws Exception {
		userService.saveWithTxAnnotationOnInterface(new User());
		Collection<String> bookmarks = bookmarkManager.getBookmarks();
		assertThat(bookmarks).isNotEmpty();

		userService.saveWithTxAnnotationOnInterface(new User());
		userService.getAllUsersWithBookmark();

		assertThat(bookmarkManager.getBookmarks()).isNotEmpty();
		assertThat(bookmarkManager.getBookmarks()).doesNotContainAnyElementsOf(bookmarks);
	}

	@Configuration
	@EnableTransactionManagement
	@EnableBookmarkManagement
	@EnableNeo4jRepositories(basePackages = "org.springframework.data.neo4j.examples.movies.repo")
	@ComponentScan(value = "org.springframework.data.neo4j.examples.movies.service")
	static class BookmarkConfiguration {

		@Bean
		ServerControls neo4jTestServer() {
			return TestServerBuilders.newInProcessBuilder().newServer();
		}

		@Bean
		public Driver nativeDriver(ServerControls neo4jTestServer) {
			return spy(GraphDatabase.driver(neo4jTestServer.boltURI(), AuthTokens.none()));
		}

		@Bean
		org.neo4j.ogm.driver.Driver neo4jOGMDriver(Driver nativeDriver) {
			return new BoltDriver(nativeDriver);
		}

		@Bean
		SessionFactory sessionFactory(org.neo4j.ogm.driver.Driver neo4jOGMDriver) {
			return new SessionFactory(neo4jOGMDriver, "org.springframework.data.neo4j.examples.movies.domain");
		}

		@Bean
		public PlatformTransactionManager transactionManager(SessionFactory sessionFactory) {
			return new Neo4jTransactionManager(sessionFactory);
		}

		@Bean
		@Scope
		public BookmarkManager bookmarkManager() {
			return new CaffeineBookmarkManager();
		}
	}
}
