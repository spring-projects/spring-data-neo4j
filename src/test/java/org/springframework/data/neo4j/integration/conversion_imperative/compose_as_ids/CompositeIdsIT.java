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
package org.springframework.data.neo4j.integration.conversion_imperative.compose_as_ids;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
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
class CompositeIdsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	interface ThingWithCompositePropertyRepository extends Neo4jRepository<ThingWithCompositeProperty, Long> {

		Optional<ThingWithCompositeProperty> findByCompositeValue(CompositeValue compositeValue);

		List<ThingWithCompositeProperty> findAllByCompositeValueNot(CompositeValue compositeValue);
	}

	interface ThingWithCompositeIdRepository extends Neo4jRepository<ThingWithCompositeId, CompositeValue> {
	}


	@BeforeEach
	public void prepareDatabase(@Autowired Driver driver, @Autowired BookmarkCapture bookmarkCapture) {

		try (Session session = driver.session()) {
			session.run("MATCH (n:ThingWithCompositeProperty) DETACH DELETE n").consume();
			session.run("MATCH (n:ThingWithCompositeId) DETACH DELETE n").consume();
			bookmarkCapture.seedWith(session.lastBookmark());
		}
	}

	@Test
	void findByCompositeValuesShouldWork(@Autowired ThingWithCompositePropertyRepository repository) {

		ThingWithCompositeProperty thing = new ThingWithCompositeProperty(new CompositeValue("a", 1), "first entity");
		ThingWithCompositeProperty saved = repository.save(thing);

		repository.save(new ThingWithCompositeProperty(new CompositeValue("b", 1), "2nd entity"));

		saved.setName("foobar");
		saved = repository.save(saved);
		assertThat(saved.getName()).isEqualTo("foobar");

		Optional<ThingWithCompositeProperty> reloaded = repository.findByCompositeValue(saved.getCompositeValue());
		assertThat(reloaded).hasValueSatisfying(v -> assertThat(v.getName()).isEqualTo("foobar"));

		assertThat(repository.findAllByCompositeValueNot(saved.getCompositeValue()))
				.hasSize(1)
				.element(0)
				.satisfies(v -> assertThat(v.getCompositeValue()).isEqualTo(new CompositeValue("b", 1)));
	}

	@Test
	void compositeIdsShouldWork(@Autowired ThingWithCompositeIdRepository repository) {

		ThingWithCompositeId thing = new ThingWithCompositeId(new CompositeValue("a,", 1), "first entity");
		ThingWithCompositeId saved = repository.save(thing);
		assertThat(saved.getVersion()).isGreaterThanOrEqualTo(0);

		saved.setName("foobar");
		saved = repository.save(saved);
		assertThat(saved.getVersion()).isGreaterThan(0);
		assertThat(saved.getName()).isEqualTo("foobar");

		Optional<ThingWithCompositeId> reloaded = repository.findById(saved.getId());
		assertThat(reloaded).isPresent();
	}

	@Test
	void findAllByCompositeIdsShouldWork(@Autowired ThingWithCompositeIdRepository repository) {

		int cnt = 0;
		String[] value1Values = {"a", "b"};
		int[] value2Values = {1, 2};
		List<CompositeValue> ids = new ArrayList<>(value1Values.length * value2Values.length);
		for (String value1 : value1Values) {
			for (int value2 : value2Values) {
				CompositeValue id = new CompositeValue(value1, value2);
				ids.add(id);
				ThingWithCompositeId saved = repository.save(new ThingWithCompositeId(id, "Entity " + ++cnt));
				assertThat(saved.getVersion()).isGreaterThanOrEqualTo(0);
			}
		}
		CompositeValue removedId = ids.remove(ids.size() - 1);

		List<ThingWithCompositeId> loadedThings = repository.findAllById(ids);
		Collections.sort(loadedThings, Comparator.comparing(ThingWithCompositeId::getName));
		assertThat(loadedThings)
				.hasSize(ids.size())
				.satisfies(v -> assertThat(v.getName()).isEqualTo("Entity 1"), Index.atIndex(0))
				.satisfies(v -> assertThat(v.getName()).isEqualTo("Entity 3"), Index.atIndex(2));

		assertThat(repository.existsById(removedId)).isTrue();
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(CompositeIdsIT.class.getPackage().getName());
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
