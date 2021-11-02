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
package org.springframework.data.neo4j.integration.conversion_reactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.integration.shared.conversion.CompositePropertiesITBase;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCompositeProperties;
import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 * @soundtrack Die Toten Hosen - Learning English, Lesson Two
 */
@Neo4jIntegrationTest
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
class ReactiveCompositePropertiesIT extends CompositePropertiesITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired
	ReactiveCompositePropertiesIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void compositePropertiesOnRelationshipsShouldBeWritten(@Autowired Repository repository) {

		List<ThingWithCompositeProperties> recorded = new ArrayList<>();
		repository.save(newEntityWithRelationshipWithCompositeProperties())
				.as(StepVerifier::create)
				.recordWith(() -> recorded).expectNextCount(1L)
				.verifyComplete();

		assertThat(recorded).hasSize(1);
		assertRelationshipPropertiesInGraph(recorded.get(0).getId());
	}

	@Test
	void compositePropertiesOnRelationshipsShouldBeRead(@Autowired Repository repository) {

		Long id = createRelationshipWithCompositeProperties();
		repository.findById(id)
				.as(StepVerifier::create)
				.consumeNextWith(this::assertRelationshipPropertiesOn)
				.verifyComplete();
	}

	@Test
	void compositePropertiesOnNodesShouldBeWritten(@Autowired Repository repository) {

		List<ThingWithCompositeProperties> recorded = new ArrayList<>();
		repository.save(newEntityWithCompositeProperties())
				.as(StepVerifier::create).recordWith(() -> recorded)
				.expectNextCount(1L)
				.verifyComplete();

		assertThat(recorded).hasSize(1);
		assertNodePropertiesInGraph(recorded.get(0).getId());
	}

	@Test
	void compositePropertiesOnNodesShouldBeRead(@Autowired Repository repository) {

		Long id = createNodeWithCompositeProperties();
		repository.findById(id)
				.as(StepVerifier::create)
				.consumeNextWith(this::assertNodePropertiesOn)
				.verifyComplete();
	}

	public interface Repository extends ReactiveNeo4jRepository<ThingWithCompositeProperties, Long> {
	}

	@Configuration
	@EnableReactiveNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends AbstractReactiveNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jConversions neo4jConversions() {
			return new Neo4jConversions(Collections.singleton(new ThingWithCustomTypes.CustomTypeConverter()));
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
	}
}
