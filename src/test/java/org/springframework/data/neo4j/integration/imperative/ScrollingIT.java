/*
 * Copyright 2011-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Function;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.WindowIterator;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.imperative.repositories.ScrollingRepository;
import org.springframework.data.neo4j.integration.shared.common.ScrollingEntity;
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
class ScrollingIT {

	@SuppressWarnings("unused")
	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@BeforeAll
	static void setupTestData(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {
		try (
				var session = driver.session(bookmarkCapture.createSessionConfig());
				var transaction = session.beginTransaction()
		) {
			ScrollingEntity.createTestData(transaction);
			transaction.commit();
			bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void oneColumnSortNoScroll(@Autowired ScrollingRepository repository) {

		var topN = repository.findTop4ByOrderByB();
		assertThat(topN)
				.hasSize(4)
				.extracting(ScrollingEntity::getA)
				.containsExactly("A0", "B0", "C0", "D0");
	}

	@Test
	void forwardWithDuplicatesManualIteration(@Autowired ScrollingRepository repository) {

		var duplicates = repository.findAllByAOrderById("D0");
		assertThat(duplicates).hasSize(2);

		var window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, KeysetScrollPosition.initial());
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.satisfies(e -> assertThat(e.getId()).isEqualTo(duplicates.get(0).getId()), Index.atIndex(3))
				.extracting(ScrollingEntity::getA)
				.containsExactly("A0", "B0", "C0", "D0");

		window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, window.positionAt(window.size() - 1));
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.satisfies(e -> assertThat(e.getId()).isEqualTo(duplicates.get(1).getId()), Index.atIndex(0))
				.extracting(ScrollingEntity::getA)
				.containsExactly("D0", "E0", "F0", "G0");

		window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, window.positionAt(window.size() - 1));
		assertThat(window.isLast()).isTrue();
		assertThat(window).extracting(ScrollingEntity::getA)
				.containsExactly("H0", "I0");
	}

	@Test
	void forwardWithDuplicatesIteratorIteration(@Autowired ScrollingRepository repository) {

		var it = WindowIterator.of(pos -> repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, pos))
				.startingAt(KeysetScrollPosition.initial());
		var content = new ArrayList<ScrollingEntity>();
		while (it.hasNext()) {
			var next = it.next();
			content.add(next);
		}

		assertThat(content).hasSize(10);
		assertThat(content.stream().map(ScrollingEntity::getId)
				.distinct().toList()).hasSize(10);
	}

	@Test
	void backwardWithDuplicatesManualIteration(@Autowired ScrollingRepository repository) {

		// Recreate the last position
		var last = repository.findFirstByA("I0");
		var keys = Map.of(
				"foobar", Values.value(last.getA()),
				"b", Values.value(last.getB()),
				Constants.NAME_OF_ADDITIONAL_SORT, Values.value(last.getId().toString())
		);

		var duplicates = repository.findAllByAOrderById("D0");
		assertThat(duplicates).hasSize(2);

		var window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, KeysetScrollPosition.of(keys, KeysetScrollPosition.Direction.Backward));
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(ScrollingEntity::getA)
				.containsExactly("F0", "G0", "H0", "I0");

		var pos = ((KeysetScrollPosition) window.positionAt(0));
		pos = KeysetScrollPosition.of(pos.getKeys(), KeysetScrollPosition.Direction.Backward);
		window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, pos);
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.extracting(ScrollingEntity::getA)
				.containsExactly("C0", "D0", "D0", "E0");

		pos = ((KeysetScrollPosition) window.positionAt(0));
		pos = KeysetScrollPosition.of(pos.getKeys(), KeysetScrollPosition.Direction.Backward);
		window = repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, pos);
		assertThat(window.isLast()).isTrue();
		assertThat(window).extracting(ScrollingEntity::getA)
				.containsExactly("A0", "B0");
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}
}
