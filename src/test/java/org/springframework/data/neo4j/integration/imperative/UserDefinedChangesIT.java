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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.support.NeedsUpdateEvaluator;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityA;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityAWithGeneratedId;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityB;
import org.springframework.data.neo4j.integration.shared.common.UserDefinedChangeEntityBWithGeneratedId;
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
				transaction
					.run("""
							CREATE (a:UserDefinedChangeEntityB)<-[:PROPERTY]-(b:UserDefinedChangeEntityA)-[:DIRECT]->(c:UserDefinedChangeEntityB)
							SET a.name= 'viaProperty', b.name = 'changeMeIfYouCan', c.name = 'direct'
							""")
					.consume();
				transaction
					.run("""
							CREATE (a:UserDefinedChangeEntityBWithGeneratedId)<-[:PROPERTY]-(b:UserDefinedChangeEntityAWithGeneratedId)-[:DIRECT]->(c:UserDefinedChangeEntityBWithGeneratedId)
							SET a.name= 'viaProperty', b.name = 'changeMeIfYouCan', b.id=randomUUID(), c.name = 'direct'
							""")
					.consume();
				transaction.commit();
			}
			this.bookmarkCapture.seedWith(session.lastBookmarks());
		}
	}

	@Test
	void entityShouldBeUpdated(@Autowired UserDefinedChangeRepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "please update me";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void entityShouldNotBeUpdated(@Autowired UserDefinedChangeRepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changeMeIfYouCan");
		}
	}

	@Test
	void newEntityShouldAlwaysBeUpdated(@Autowired UserDefinedChangeRepository repository, @Autowired Driver driver) {
		var entity = new UserDefinedChangeEntityA("should be saved");
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA{name:'should be saved'}) return n").single();
			var node = result.get("n").asNode();
			assertThat(node.keys()).hasSize(1);
		}
	}

	@Test
	void entityUpdatedWithSaveAllShouldBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "please update me";
		repository.saveAll(List.of(entity));

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void entityUpdatedWithSaveAllShouldNotBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		repository.saveAll(List.of(entity));

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("changeMeIfYouCan");
		}
	}

	@Test
	void newEntityUpdatedWithSaveAllShouldAlwaysBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = new UserDefinedChangeEntityA("should be saved");
		repository.saveAll(List.of(entity));

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityA{name:'should be saved'}) return n").single();
			var node = result.get("n").asNode();
			assertThat(node.keys()).hasSize(1);
		}
	}

	@Test
	void entityWithGeneratedIdUpdatedWithSaveAllShouldBeUpdated(
			@Autowired UserDefinedChangeWithGeneratedIdRepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "please update me";
		repository.saveAll(List.of(entity));

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityAWithGeneratedId) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(2);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void entityWithGeneratedIdUpdatedWithSaveAllShouldNotBeUpdated(
			@Autowired UserDefinedChangeWithGeneratedIdRepository repository, @Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		entity.name = "updatedName";
		var allProcessedEntities = repository.saveAll(List.of(entity));

		assertThat(allProcessedEntities).hasSize(1);
		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (n:UserDefinedChangeEntityAWithGeneratedId) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(2);
			assertThat(node.get("name").asString()).isEqualTo("changeMeIfYouCan");
		}
	}

	@Test
	void newEntityWithGeneratedIdUpdatedWithSaveAllShouldAlwaysBeUpdated(
			@Autowired UserDefinedChangeWithGeneratedIdRepository repository, @Autowired Driver driver) {
		var entity = new UserDefinedChangeEntityAWithGeneratedId("should be saved");
		repository.saveAll(List.of(entity));

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session
				.run("MATCH (n:UserDefinedChangeEntityAWithGeneratedId{name:'should be saved'}) return n")
				.single();
			var node = result.get("n").asNode();
			assertThat(node.keys()).hasSize(2);
		}
	}

	@Test
	void directRelatedEntityShouldBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.bs.get(0);
		entity.name = "please update me";
		firstB.name = "please update me";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:DIRECT]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void directRelatedEntityShouldNotBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.bs.get(0);
		entity.name = "please update me";
		firstB.name = "changed";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:DIRECT]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("direct");
		}
	}

	@Test
	void relationshipPropertyRelatedEntityShouldBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.relationshipProperties.get(0).target;
		entity.name = "please update me";
		firstB.name = "please update me";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:PROPERTY]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("please update me");
		}
	}

	@Test
	void relationshipPropertyRelatedEntityShouldNotBeUpdated(@Autowired UserDefinedChangeRepository repository,
			@Autowired Driver driver) {
		var entity = repository.findByName("changeMeIfYouCan");
		var firstB = entity.relationshipProperties.get(0).target;
		entity.name = "please update me";
		firstB.name = "changed";
		repository.save(entity);

		try (var session = this.driver.session(this.bookmarkCapture.createSessionConfig())) {
			var result = session.run("MATCH (:UserDefinedChangeEntityA)-[:PROPERTY]->(n) return n").list();
			assertThat(result.size()).isEqualTo(1);
			var node = result.get(0).get("n").asNode();
			assertThat(node.keys()).hasSize(1);
			assertThat(node.get("name").asString()).isEqualTo("viaProperty");
		}
	}

	interface UserDefinedChangeRepository extends Neo4jRepository<UserDefinedChangeEntityA, String> {

		UserDefinedChangeEntityA findByName(String name);

	}

	interface UserDefinedChangeWithGeneratedIdRepository
			extends Neo4jRepository<UserDefinedChangeEntityAWithGeneratedId, String> {

		UserDefinedChangeEntityAWithGeneratedId findByName(String name);

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
		NeedsUpdateEvaluator<UserDefinedChangeEntityA> udceForUdcewba() {
			return new NeedsUpdateEvaluator<UserDefinedChangeEntityA>() {
				@Override
				public boolean needsUpdate(UserDefinedChangeEntityA instance) {
					return instance.name.equals("please update me");
				}

				@Override
				public Class<UserDefinedChangeEntityA> getEvaluatingClass() {
					return UserDefinedChangeEntityA.class;
				}
			};
		}

		@Bean
		NeedsUpdateEvaluator<UserDefinedChangeEntityB> udceForUdcewbb() {
			return new NeedsUpdateEvaluator<UserDefinedChangeEntityB>() {
				@Override
				public boolean needsUpdate(UserDefinedChangeEntityB instance) {
					return instance.name.equals("please update me");
				}

				@Override
				public Class<UserDefinedChangeEntityB> getEvaluatingClass() {
					return UserDefinedChangeEntityB.class;
				}
			};
		}

		@Bean
		NeedsUpdateEvaluator<UserDefinedChangeEntityAWithGeneratedId> udceForUdcewbaWgId() {
			return new NeedsUpdateEvaluator<UserDefinedChangeEntityAWithGeneratedId>() {
				@Override
				public boolean needsUpdate(UserDefinedChangeEntityAWithGeneratedId instance) {
					return instance.name.equals("please update me");
				}

				@Override
				public Class<UserDefinedChangeEntityAWithGeneratedId> getEvaluatingClass() {
					return UserDefinedChangeEntityAWithGeneratedId.class;
				}
			};
		}

		@Bean
		NeedsUpdateEvaluator<UserDefinedChangeEntityBWithGeneratedId> udceForUdcewbbWgId() {
			return new NeedsUpdateEvaluator<UserDefinedChangeEntityBWithGeneratedId>() {
				@Override
				public boolean needsUpdate(UserDefinedChangeEntityBWithGeneratedId instance) {
					return instance.name.equals("please update me");
				}

				@Override
				public Class<UserDefinedChangeEntityBWithGeneratedId> getEvaluatingClass() {
					return UserDefinedChangeEntityBWithGeneratedId.class;
				}
			};
		}

		@Override
		public boolean isCypher5Compatible() {
			return neo4jConnectionSupport.isCypher5SyntaxCompatible();
		}

	}

}
