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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
import org.springframework.data.domain.Window;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.reactive.repositories.ReactiveScrollingRepository;
import org.springframework.data.neo4j.integration.shared.common.ScrollingEntity;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.test.Neo4jReactiveTestConfiguration;
import org.springframework.transaction.ReactiveTransactionManager;

import reactor.test.StepVerifier;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ReactiveScrollingIT {

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
	void oneColumnSortNoScroll(@Autowired ReactiveScrollingRepository repository) {

		repository.findTop4ByOrderByB()
				.map(ScrollingEntity::getA)
				.as(StepVerifier::create)
				.expectNext("A0", "B0", "C0", "D0");
	}

	@Test
	void forwardWithDuplicatesManualIteration(@Autowired ReactiveScrollingRepository repository) {

		var duplicates = new ArrayList<ScrollingEntity>();
		repository.findAllByAOrderById("D0").as(StepVerifier::create)
				.recordWith(() -> duplicates)
				.expectNextCount(2)
				.verifyComplete();

		var windowContainer = new AtomicReference<Window<ScrollingEntity>>();
		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, KeysetScrollPosition.initial())
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		var window = windowContainer.get();
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.satisfies(e -> assertThat(e.getId()).isEqualTo(duplicates.get(0).getId()), Index.atIndex(3))
				.extracting(ScrollingEntity::getA)
				.containsExactly("A0", "B0", "C0", "D0");

		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, window.positionAt(window.size() - 1))
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		window = windowContainer.get();
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.satisfies(e -> assertThat(e.getId()).isEqualTo(duplicates.get(1).getId()), Index.atIndex(0))
				.extracting(ScrollingEntity::getA)
				.containsExactly("D0", "E0", "F0", "G0");

		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, window.positionAt(window.size() - 1))
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		window = windowContainer.get();
		assertThat(window.isLast()).isTrue();
		assertThat(window).extracting(ScrollingEntity::getA)
				.containsExactly("H0", "I0");
	}

	@Test
	void backwardWithDuplicatesManualIteration(@Autowired ReactiveScrollingRepository repository) {

		// Recreate the last position
		var last = repository.findFirstByA("I0").block();
		var keys = Map.of(
				"foobar", Values.value(last.getA()),
				"b", Values.value(last.getB()),
				Constants.NAME_OF_ADDITIONAL_SORT, Values.value(last.getId().toString())
		);

		var duplicates = new ArrayList<ScrollingEntity>();
		repository.findAllByAOrderById("D0").as(StepVerifier::create)
				.recordWith(() -> duplicates)
				.expectNextCount(2)
				.verifyComplete();

		var windowContainer = new AtomicReference<Window<ScrollingEntity>>();
		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, KeysetScrollPosition.of(keys, KeysetScrollPosition.Direction.Backward))
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		var window = windowContainer.get();
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(ScrollingEntity::getA)
				.containsExactly("F0", "G0", "H0", "I0");

		var pos = ((KeysetScrollPosition) window.positionAt(0));
		pos = KeysetScrollPosition.of(pos.getKeys(), KeysetScrollPosition.Direction.Backward);
		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, pos)
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		window = windowContainer.get();
		assertThat(window.hasNext()).isTrue();
		assertThat(window)
				.hasSize(4)
				.extracting(Function.identity())
				.extracting(ScrollingEntity::getA)
				.containsExactly("C0", "D0", "D0", "E0");

		pos = ((KeysetScrollPosition) window.positionAt(0));
		pos = KeysetScrollPosition.of(pos.getKeys(), KeysetScrollPosition.Direction.Backward);
		repository.findTop4By(ScrollingEntity.SORT_BY_B_AND_A, pos)
				.as(StepVerifier::create)
				.consumeNextWith(windowContainer::set)
				.verifyComplete();
		window = windowContainer.get();
		assertThat(window.isLast()).isTrue();
		assertThat(window).extracting(ScrollingEntity::getA)
				.containsExactly("A0", "B0");
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableReactiveNeo4jRepositories
	static class Config extends Neo4jReactiveTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider, Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}
}
