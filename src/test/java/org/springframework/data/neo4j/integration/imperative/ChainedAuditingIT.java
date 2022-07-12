/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.config.EnableNeo4jAuditing;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.callback.AuditingBeforeBindCallback;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.Book;
import org.springframework.data.neo4j.integration.shared.common.Editor;
import org.springframework.data.neo4j.integration.shared.common.ImmutableAuditableThing;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class ChainedAuditingIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeEach
	protected void setupData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (var session = driver.session(bookmarkCapture.createSessionConfig());
				var transaction = session.beginTransaction()) {
			transaction.run("MATCH (n) detach delete n");
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void auditingCallbacksShouldBeCombinableWithOtherCallbacks(@Autowired BookRepository bookRepository) {

		var book = new Book("Dune");
		book = bookRepository.save(book);

		assertThat(book.getCreatedAt()).isNotNull();
		assertThat(book.getModifiedAt()).isNotNull();
		assertThat(book.getCreatedBy()).isNotNull();
		assertThat(book.getModifiedBy()).isNotNull();

		for (int i = 1; i <= 5; ++i) {
			book.setContent(String.format("Content was edited %d times", i));
			book = bookRepository.save(book);
		}

		assertThat(book.getModifiedBy()).isEqualTo("User 6");

		var names = Stream.of(5, 3, 2, 1).map(i -> "User " + i).toArray(String[]::new);
		var editor = book.getPreviousEditor();
		for (int i = 0; i < names.length; i++) {
			assertThat(editor).isNotNull();
			assertThat(editor.getName()).isEqualTo(names[i]);
			editor = editor.getPredecessor();
		}
	}

	interface BookRepository extends Neo4jRepository<Book, UUID> {
	}

	static class BookEditorHistorian implements BeforeBindCallback<Book>, Ordered {

		@Override
		public Book onBeforeBind(Book entity) {
			if (entity.getModifiedBy() != null) {
				var previousEditor = entity.getPreviousEditor();
				if (previousEditor == null || !previousEditor.getName().equals(entity.getModifiedBy())) {
					previousEditor = new Editor(entity.getModifiedBy(), previousEditor);
				}
				entity.setPreviousEditor(previousEditor);
			}
			return entity;
		}

		@Override
		public int getOrder() {
			return AuditingBeforeBindCallback.NEO4J_AUDITING_ORDER - 50;
		}
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableNeo4jAuditing(auditorAwareRef = "auditorProvider")
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(ImmutableAuditableThing.class.getPackage().getName());
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver,
				DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

		@Bean
		public AuditorAware<String> auditorProvider() {
			var state = new AtomicInteger(0);
			return () -> {
				int i = state.compareAndSet(3, 4) ? 3 : state.incrementAndGet();
				return Optional.of("User " + i);
			};
		}

		@Bean
		public BeforeBindCallback<Book> bookEditorHistorian() {
			return new BookEditorHistorian();
		}

	}
}
