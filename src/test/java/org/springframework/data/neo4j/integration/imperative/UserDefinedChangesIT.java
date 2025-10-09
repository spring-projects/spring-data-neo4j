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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.support.UserDefinedChangeEvaluator;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityA;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityWithBeanA;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jImperativeTestConfiguration;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gerrit Meier
 */
@Neo4jIntegrationTest
class UserDefinedChangesIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	private final BookmarkCapture bookmarkCapture;

	private final Driver driver;

	@Autowired
	UserDefinedChangesIT(Driver driver, BookmarkCapture bookmarkCapture) {
		this.driver = driver;
		this.bookmarkCapture = bookmarkCapture;
	}

	@BeforeEach
	protected void setupData() {

		try (Session session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			try (Transaction transaction = session.beginTransaction()) {
				transaction.run("MATCH (n) detach delete n").consume();
				transaction.run("""
					CREATE (a:UserDefinedChangeEntityB)<-[:PROPERTY]-(b:UserDefinedChangeEntityA)-[:DIRECT]->(c:UserDefinedChangeEntityB)
					SET a.name= 'viaProperty', b.name = 'changeMeIfYouCan', c.name = 'direct'
					""").consume();
				transaction.run("""
					CREATE (a:UserDefinedChangeEntityWithBeanB)<-[:PROPERTY]-(b:UserDefinedChangeEntityWithBeanA)-[:DIRECT]->(c:UserDefinedChangeEntityWithBeanB)
					SET a.name= 'viaProperty', b.name = 'changeMeIfYouCan', c.name = 'direct'
					""").consume();
				transaction.commit();
			}
			this.bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void entityShouldBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		entity.needsUpdate = true;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("updatedName");
		}
	}

	@Test
	void entityShouldNotBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		entity.needsUpdate = false;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changeMeIfYouCan");
		}
	}

	@Test
	void directRelatedEntityShouldBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.bs.get(0);
		firstB.name = "changed";
		entity.needsUpdate = true;
		firstB.needsUpdate = true;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:DIRECT]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changed");
		}
	}

	@Test
	void directRelatedEntityShouldNotBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.bs.get(0);
		firstB.name = "changed";
		entity.needsUpdate = true;
		firstB.needsUpdate = false;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:DIRECT]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("direct");
		}
	}

	@Test
	void relationshipPropertyRelatedEntityShouldBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.relationshipProperties.get(0).target;
		firstB.name = "changed";
		entity.needsUpdate = true;
		firstB.needsUpdate = true;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:PROPERTY]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changed");
		}
	}

	@Test
	void relationshipPropertyRelatedEntityShouldNotBeUpdated(@Autowired UserDefinedChangeARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.relationshipProperties.get(0).target;
		firstB.name = "changed";
		entity.needsUpdate = true;
		firstB.needsUpdate = false;
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:PROPERTY]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("viaProperty");
		}
	}

	@Test
	void beanControlledEntityShouldBeUpdated(@Autowired UserDefinedChangeWithBeanARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "please update me";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityWithBeanA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void beanControlledEntityShouldNotBeUpdated(@Autowired UserDefinedChangeWithBeanARepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityWithBeanA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.labels()).hasSize(1);
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changeMeIfYouCan");
		}
	}

	interface UserDefinedChangeARepository extends Neo4jRepository<UserDefinedChangeEntityA, String> {
		UserDefinedChangeEntityA findByName(String name);
	}

	interface UserDefinedChangeWithBeanARepository extends Neo4jRepository<UserDefinedChangeEntityWithBeanA, String> {
		UserDefinedChangeEntityWithBeanA findByName(String name);
	}

	@Configuration
	@EnableNeo4jRepositories(considerNestedRepositories = true)
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

		@Bean
		public UserDefinedChangeEvaluator<UserDefinedChangeEntityWithBeanA> udceForUdcewba() {
			return new UserDefinedChangeEvaluator<UserDefinedChangeEntityWithBeanA>() {
				@Override
				public boolean needsUpdate(UserDefinedChangeEntityWithBeanA instance) {
					return instance.name.equals("please update me");
				}

				@Override
				public Class<UserDefinedChangeEntityWithBeanA> getEvaluatingClass() {
					return UserDefinedChangeEntityWithBeanA.class;
				}
			};
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
