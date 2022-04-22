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
package org.springframework.data.neo4j.integration.conversion_imperative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.types.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.conversion.CompositePropertiesITBase;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCompositeProperties;
import org.springframework.data.neo4j.integration.shared.conversion.ThingWithCustomTypes;
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
 * @soundtrack Die Toten Hosen - Learning English, Lesson Two
 */
@Neo4jIntegrationTest
class ImperativeCompositePropertiesIT extends CompositePropertiesITBase {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Autowired ImperativeCompositePropertiesIT(Driver driver, BookmarkCapture bookmarkCapture) {
		super(driver, bookmarkCapture);
	}

	@Test
	void compositePropertiesOnRelationshipsShouldBeWritten(@Autowired Repository repository) {

		ThingWithCompositeProperties t = repository.save(newEntityWithRelationshipWithCompositeProperties());
		assertRelationshipPropertiesInGraph(t.getId());
	}

	@Test
	void compositePropertiesOnRelationshipsShouldBeRead(@Autowired Repository repository) {

		Long id = createRelationshipWithCompositeProperties();
		assertThat(repository.findById(id)).isPresent().hasValueSatisfying(this::assertRelationshipPropertiesOn);
	}

	@Test
	void compositePropertiesOnNodesShouldBeWritten(@Autowired Repository repository) {

		ThingWithCompositeProperties t = repository.save(newEntityWithCompositeProperties());
		assertNodePropertiesInGraph(t.getId());
	}

	@Test
	void compositePropertiesOnNodesShouldBeRead(@Autowired Repository repository) {

		Long id = createNodeWithCompositeProperties();
		assertThat(repository.findById(id)).isPresent().hasValueSatisfying(this::assertNodePropertiesOn);
	}

	@Test // GH-2280
	void compositePropertiesOnNodesShouldBeDeleted(@Autowired Repository repository) {

		Long id = createNodeWithCompositeProperties();
		ThingWithCompositeProperties thing = repository.findById(id).get();
		thing.setDatesWithTransformedKey(Collections.singletonMap("Test", null));
		thing.setSomeDatesByEnumA(Collections.singletonMap(ThingWithCompositeProperties.EnumA.VALUE_AA, null));
		thing.setSomeOtherDTO(null);
		repository.save(thing);

		try (Session session = driver.session()) {
			Record r = session.readTransaction(tx -> tx.run("MATCH (t:CompositeProperties) WHERE id(t) = $id RETURN t",
					Collections.singletonMap("id", id)).single());
			Node n = r.get("t").asNode();
			assertThat(n.asMap()).doesNotContainKeys(
					"someDatesByEnumA.VALUE_AA",
					"datesWithTransformedKey.test",
					"dto.x", "dto.y", "dto.z"
			);
		}
	}

	public interface ThingProjection {

		ThingWithCompositeProperties.SomeOtherDTO getSomeOtherDTO();
	}

	@Test // GH-2451
	void compositePropertiesShouldBeFilterableEvenOnNonMapTypes(@Autowired Repository repository, @Autowired Neo4jTemplate template) {

		Long id = createNodeWithCompositeProperties();
		ThingWithCompositeProperties thing = repository.findById(id).get();
		thing.setDatesWithTransformedKey(Collections.singletonMap("Test", null));
		thing.setSomeDatesByEnumA(Collections.singletonMap(ThingWithCompositeProperties.EnumA.VALUE_AA, null));
		thing.setSomeOtherDTO(null);
		template.saveAs(thing, ThingProjection.class);

		try (Session session = driver.session()) {
			Record r = session.readTransaction(tx -> tx.run("MATCH (t:CompositeProperties) WHERE id(t) = $id RETURN t",
					Collections.singletonMap("id", id)).single());
			Node n = r.get("t").asNode();
			assertThat(n.asMap())
					.containsKeys(
							"someDatesByEnumA.VALUE_AA",
							"datesWithTransformedKey.test"
					)
					.doesNotContainKeys("dto.x", "dto.y", "dto.z");
		}
	}

	public interface Repository extends Neo4jRepository<ThingWithCompositeProperties, Long> {
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

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
