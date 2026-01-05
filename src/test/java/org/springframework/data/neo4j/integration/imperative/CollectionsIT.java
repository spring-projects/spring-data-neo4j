/*
 * Copyright 2011-present the original author or authors.
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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
public class CollectionsIT {

	private static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final Driver driver;

	private final BookmarkCapture bookmarkCapture;

	@Autowired
	CollectionsIT(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@Test // GH-2236
	void loadingOfRelPropertiesInSetsShouldWork(@Autowired Neo4jTemplate repository) {

		Long id;
		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			id = session.run(
					"CREATE (c:CollectionChildNodeA {name: 'The Child'}) <- [:CHILDREN_WITH_PROPERTIES {prop: 'The Property'}] - (p:CollectionParentNode {name: 'The Parent'}) RETURN id(p)")
				.single()
				.get(0)
				.asLong();
			this.bookmarkCapture.seedWith(session.lastBookmarks());
		}

		Optional<CollectionParentNode> optionalParent = repository.findById(id, CollectionParentNode.class);

		assertThat(optionalParent).hasValueSatisfying(parent -> {
			assertThat(parent.id).isNotNull();
			assertThat(parent.name).isEqualTo("The Parent");
			assertThat(parent.childrenWithProperties).isNotNull();
			assertThat(parent.childrenWithProperties).first().satisfies(p -> {
				assertThat(p.target.id).isNotNull();
				assertThat(p.target.name).isEqualTo("The Child");
				assertThat(p.prop).isEqualTo("The Property");
			});
		});
	}

	@Test // GH-2236
	void storingOfRelPropertiesInSetsShouldWork(@Autowired Neo4jTemplate template) {

		CollectionParentNode parent = new CollectionParentNode("parent");
		parent.childrenWithProperties.add(new RelProperties(new CollectionChildNodeA("child"), "a property"));

		parent = template.save(parent);
		assertThat(parent.id).isNotNull();
		assertThat(parent.childrenWithProperties).isNotNull();
		assertThat(parent.childrenWithProperties).first().satisfies(p -> {
			assertThat(p.target.id).isNotNull();
			assertThat(p.target.name).isEqualTo("child");
			assertThat(p.prop).isEqualTo("a property");
		});

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			long cnt = session.run(
					"MATCH (c:CollectionChildNodeA) <- [:CHILDREN_WITH_PROPERTIES] - (p:CollectionParentNode) WHERE id(p) = $id RETURN count(c) ",
					Collections.singletonMap("id", parent.id))
				.single()
				.get(0)
				.asLong();
			assertThat(cnt).isEqualTo(1L);
			this.bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Node
	static class CollectionParentNode {

		final String name;

		@Id
		@GeneratedValue
		Long id;

		Set<RelProperties> childrenWithProperties = new HashSet<>();

		CollectionParentNode(String name) {
			this.name = name;
		}

	}

	@Node
	static class CollectionChildNodeA {

		final String name;

		@Id
		@GeneratedValue
		Long id;

		CollectionChildNodeA(String name) {
			this.name = name;
		}

	}

	@RelationshipProperties
	static class RelProperties {

		@TargetNode
		final CollectionChildNodeA target;

		final String prop;

		@RelationshipId
		Long id;

		RelProperties(CollectionChildNodeA target, String prop) {
			this.target = target;
			this.prop = prop;
		}

	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends Neo4jImperativeTestConfiguration {

		@Bean
		@Override
		public Driver driver() {
			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions)
				throws ClassNotFoundException {

			// Don't create repositories for the entities, otherwise they must be moved
			// to a public reachable place. I didn't want that as the mapping context is
			// polluted already
			// enough with the shared package of nodes.
			Neo4jMappingContext ctx = new Neo4jMappingContext(neo4JConversions);
			ctx.setInitialEntitySet(new HashSet<>(
					Arrays.asList(CollectionParentNode.class, CollectionChildNodeA.class, RelProperties.class)));
			return ctx;
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
