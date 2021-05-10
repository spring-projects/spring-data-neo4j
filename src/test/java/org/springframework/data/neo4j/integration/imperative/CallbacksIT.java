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
package org.springframework.data.neo4j.integration.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.imperative.repositories.ThingRepository;
import org.springframework.data.neo4j.integration.shared.common.CallbacksITBase;
import org.springframework.data.neo4j.integration.shared.common.ThingWithAssignedId;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
		thing = repository.save(thing);

		assertThat(thing.getName()).isEqualTo("A name (Edited)");

		verifyDatabase(Collections.singletonList(thing));
	}

	@Test
	void onBeforeBindShouldBeCalledForAllEntities(@Autowired ThingRepository repository) {

		ThingWithAssignedId thing1 = new ThingWithAssignedId("id1", "A name");
		ThingWithAssignedId thing2 = new ThingWithAssignedId("id2", "Another name");
		Iterable<ThingWithAssignedId> savedThings = repository.saveAll(Arrays.asList(thing1, thing2));

		assertThat(savedThings).extracting(ThingWithAssignedId::getName).containsExactlyInAnyOrder("A name (Edited)",
				"Another name (Edited)");

		verifyDatabase(savedThings);
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		BeforeBindCallback<ThingWithAssignedId> nameChanger() {
			return entity -> {
				ThingWithAssignedId updatedThing = new ThingWithAssignedId(entity.getTheId(), entity.getName() + " (Edited)");
				return updatedThing;
			};
		}

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

	}
}
