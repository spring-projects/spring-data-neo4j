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
package org.springframework.data.neo4j.integration.imperative;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.imperative.repositories.ThingRepository;
import org.springframework.data.neo4j.integration.shared.common.CallbacksITBase;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAssignedId;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Michael J. Simons
 */
class CallbacksIT extends CallbacksITBase {

	@Autowired
	CallbacksIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void onBeforeBindShouldBeCalledForSingleEntity(@Autowired ThingRepository repository) {

		ThingWithAssignedId thing = new ThingWithAssignedId("aaBB", "A name");
		thing.setRandomValue("a");
		thing = repository.save(thing);

		assertThat(thing.getName()).isEqualTo("A name (Edited)");
		assertThat(thing.getRandomValue()).isEqualTo(null);

		verifyDatabase(Collections.singletonList(thing));
	}

	@Test // GH-2499
	void onAfterConvertShouldBeCalledForSingleEntity(@Autowired ThingRepository repository) {

		Optional<ThingWithAssignedId> optionalThing = repository.findById("E1");
		assertThat(optionalThing).hasValueSatisfying(thingWithAssignedId -> {
			assertThat(thingWithAssignedId.getTheId()).isEqualTo("E1");
			assertThat(thingWithAssignedId.getRandomValue()).isNotNull()
				.satisfies(v -> assertThatNoException().isThrownBy(() -> UUID.fromString(v)));
			assertThat(thingWithAssignedId.getAnotherRandomValue()).isNotNull()
				.satisfies(v -> assertThatNoException().isThrownBy(() -> UUID.fromString(v)));
		});
	}

	@Test // GH-2499
	void postLoadShouldBeInvokedForSingleEntity(@Autowired ThingRepository repository) {

		Optional<ThingWithAssignedId> optionalThing = repository.findById("E1");
		assertThat(optionalThing).hasValueSatisfying(
				thingWithAssignedId -> assertThat(thingWithAssignedId.getAnotherRandomValue()).isNotNull()
					.satisfies(v -> assertThatNoException().isThrownBy(() -> UUID.fromString(v))));
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntities(@Autowired ThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1", "A name");
		thing1.setRandomValue("a");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2", "Another name");
		thing2.setRandomValue("b");

		var unsaved = Arrays.asList(thing1, thing2);
		Iterable<ThingWithAssignedId> savedThings = repository.saveAll(unsaved);

		assertThat(unsaved).allMatch(v -> v.getRandomValue() != null);
		assertThat(unsaved).noneMatch(v -> v.getAnotherRandomValue() != null);

		assertThat(savedThings).extracting(ThingWithAssignedId::getName)
			.containsExactlyInAnyOrder("A name (Edited)", "Another name (Edited)");
		assertThat(savedThings).hasSize(2).extracting(ThingWithAssignedId::getRandomValue).allMatch(Objects::isNull);

		// Assert the onAfterConvert
		var ids = StreamSupport.stream(savedThings.spliterator(), false).map(ThingWithAssignedId::getTheId).toList();
		var reloaded = repository.findAllById(ids);
		assertThat(reloaded).allMatch(v -> v.getRandomValue() != null);

		verifyDatabase(savedThings);
	}

	@Test // GH-2499
	void onAfterConvertShouldBeCalledForAllEntities(@Autowired ThingRepository repository) {

		Iterable<ThingWithAssignedId> optionalThing = repository.findAllById(Arrays.asList("E1", "E2"));
		assertThat(optionalThing).hasSize(2).allSatisfy(thingWithAssignedId -> {
			assertThat(thingWithAssignedId.getTheId()).startsWith("E");
			assertThat(thingWithAssignedId.getRandomValue()).isNotNull()
				.satisfies(v -> assertThatNoException().isThrownBy(() -> UUID.fromString(v)));
		});
	}

	@Test // GH-2499
	void postLoadShouldBeInvokedForAllEntities(@Autowired ThingRepository repository) {

		Iterable<ThingWithAssignedId> optionalThing = repository.findAllById(Arrays.asList("E1", "E2"));
		assertThat(optionalThing).hasSize(2)
			.allSatisfy(thingWithAssignedId -> assertThat(thingWithAssignedId.getAnotherRandomValue()).isNotNull()
				.satisfies(v -> assertThatNoException().isThrownBy(() -> UUID.fromString(v))));
	}

	@Configuration
	@Import(CallbacksConfig.class)
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		BookmarkCapture bookmarkCapture() {
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

	}

}
