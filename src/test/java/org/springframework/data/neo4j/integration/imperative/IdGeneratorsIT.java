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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.schema.IdGenerator;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.IdGeneratorsITBase;
import org.springframework.data.neo4j.integration.shared.common.ThingWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.ThingWithIdGeneratedByBean;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
class IdGeneratorsIT extends IdGeneratorsITBase {

	@Autowired
	IdGeneratorsIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void idGenerationWithNewEntityShouldWork(@Autowired ThingWithGeneratedIdRepository repository) {

		ThingWithGeneratedId t = new ThingWithGeneratedId("Foobar");
		t.setName("Foobar");
		t = repository.save(t);
		assertThat(t.getTheId()).isNotBlank().matches("thingWithGeneratedId-\\d+");

		verifyDatabase(t.getTheId(), t.getName());
	}

	@Test
	void idGenerationByBeansShouldWorkWork(@Autowired ThingWithIdGeneratedByBeanRepository repository) {

		ThingWithIdGeneratedByBean t = new ThingWithIdGeneratedByBean("Foobar");
		t.setName("Foobar");
		t = repository.save(t);
		assertThat(t.getTheId()).isEqualTo("ImperativeID.");

		verifyDatabase(t.getTheId(), t.getName());
	}

	@Test
	void idGenerationWithNewEntitiesShouldWork(@Autowired ThingWithGeneratedIdRepository repository) {

		List<ThingWithGeneratedId> things = IntStream.rangeClosed(1, 10).mapToObj(i -> new ThingWithGeneratedId("name" + i))
				.collect(Collectors.toList());

		Iterable<ThingWithGeneratedId> savedThings = repository.saveAll(things);
		assertThat(savedThings).hasSize(things.size()).extracting(ThingWithGeneratedId::getTheId)
				.allMatch(s -> s.matches("thingWithGeneratedId-\\d+"));

		Set<String> distinctIds = StreamSupport.stream(savedThings.spliterator(), false).map(ThingWithGeneratedId::getTheId)
				.collect(Collectors.toSet());

		assertThat(distinctIds).hasSize(things.size());
	}

	@Test
	void shouldNotOverwriteExistingId(@Autowired ThingWithGeneratedIdRepository repository) {

		ThingWithGeneratedId t = repository.findById(ID_OF_EXISTING_THING).get();
		t.setName("changed");
		t = repository.save(t);

		assertThat(t.getTheId()).isNotBlank().isEqualTo(ID_OF_EXISTING_THING);

		verifyDatabase(t.getTheId(), t.getName());
	}

	interface ThingWithGeneratedIdRepository extends CrudRepository<ThingWithGeneratedId, String> {}

	interface ThingWithIdGeneratedByBeanRepository extends CrudRepository<ThingWithIdGeneratedByBean, String> {}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Bean
		public IdGenerator<String> aFancyIdGenerator() {
			return (label, entity) -> "ImperativeID.";
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
