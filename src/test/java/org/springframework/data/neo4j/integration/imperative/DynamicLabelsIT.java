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
import static org.neo4j.cypherdsl.core.Conditions.not;
import static org.neo4j.cypherdsl.core.Cypher.parameter;
import static org.neo4j.cypherdsl.core.Predicates.exists;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.DynamicLabelsWithMultipleNodeLabels;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.DynamicLabelsWithNodeLabel;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.ExtendedBaseClass1;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.InheritedSimpleDynamicLabels;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SimpleDynamicLabels;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SimpleDynamicLabelsCtor;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SimpleDynamicLabelsWithBusinessId;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SimpleDynamicLabelsWithBusinessIdAndVersion;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SimpleDynamicLabelsWithVersion;
import org.springframework.data.neo4j.integration.shared.common.EntitiesWithDynamicLabels.SuperNode;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michael J. Simons
 * @soundtrack Samy Deluxe - Samy Deluxe
 */
@ExtendWith(Neo4jExtension.class)
public class DynamicLabelsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Nested
	class EntityWithSingleStaticLabelAndGeneratedId extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("CREATE (e:SimpleDynamicLabels:Foo:Bar:Baz:Foobar) RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<SimpleDynamicLabels> optionalEntity = template.findById(existingEntityId, SimpleDynamicLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(
					entity -> assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar"));
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			executeInTransaction(() -> {
				SimpleDynamicLabels entity = template.findById(existingEntityId, SimpleDynamicLabels.class).get();
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			});

			List<String> labels = getLabels(existingEntityId);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			long id = executeInTransaction(() -> {
				SimpleDynamicLabels entity = new SimpleDynamicLabels();
				entity.moreLabels = new HashSet<>();
				entity.moreLabels.add("A");
				entity.moreLabels.add("B");
				entity.moreLabels.add("C");
				return template.save(entity).id;
			});

			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "A", "B", "C");
		}

		@Test
		void shouldWriteDynamicLabelsFromRelatedNodes(@Autowired Neo4jTemplate template) {

			long id = executeInTransaction(() -> {
				SimpleDynamicLabels entity = new SimpleDynamicLabels();
				entity.moreLabels = new HashSet<>();
				entity.moreLabels.add("A");
				entity.moreLabels.add("B");
				entity.moreLabels.add("C");
				SuperNode superNode = new SuperNode();
				superNode.relatedTo = entity;
				return template.save(superNode).relatedTo.id;
			});
			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithInheritedDynamicLabels extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("CREATE (e:InheritedSimpleDynamicLabels:Foo:Bar:Baz:Foobar) RETURN id(e) as existingEntityId")
					.single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<InheritedSimpleDynamicLabels> optionalEntity = template.findById(existingEntityId,
					InheritedSimpleDynamicLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(
					entity -> assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar"));
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			executeInTransaction(() -> {
				InheritedSimpleDynamicLabels entity = template.findById(existingEntityId, InheritedSimpleDynamicLabels.class)
						.get();
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			});

			List<String> labels = getLabels(existingEntityId);
			assertThat(labels).containsExactlyInAnyOrder("InheritedSimpleDynamicLabels", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			Long id = executeInTransaction(() -> {
				InheritedSimpleDynamicLabels entity = new InheritedSimpleDynamicLabels();
				entity.moreLabels = new HashSet<>();
				entity.moreLabels.add("A");
				entity.moreLabels.add("B");
				entity.moreLabels.add("C");
				return template.save(entity).id;
			});

			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("InheritedSimpleDynamicLabels", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelAndAssignedId extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run("" + "CREATE (e:SimpleDynamicLabelsWithBusinessId:Foo:Bar:Baz:Foobar {id: 'E1'}) "
					+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			executeInTransaction(() -> {
				SimpleDynamicLabelsWithBusinessId entity = template.findById("E1", SimpleDynamicLabelsWithBusinessId.class)
						.get();
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			});

			List<String> labels = getLabels(existingEntityId);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessId", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessId result = executeInTransaction(() -> {
				SimpleDynamicLabelsWithBusinessId entity = new SimpleDynamicLabelsWithBusinessId();
				entity.id = UUID.randomUUID().toString();
				entity.moreLabels = new HashSet<>();
				entity.moreLabels.add("A");
				entity.moreLabels.add("B");
				entity.moreLabels.add("C");
				return template.save(entity);
			});

			List<String> labels = getLabels(Cypher.anyNode("n").property("id").isEqualTo(parameter("id")), result.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessId", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelGeneratedIdAndVersion extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run("CREATE (e:SimpleDynamicLabelsWithVersion:Foo:Bar:Baz:Foobar {myVersion: 0}) "
					+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithVersion result = executeInTransaction(() -> {
				SimpleDynamicLabelsWithVersion entity = template
						.findById(existingEntityId, SimpleDynamicLabelsWithVersion.class)
						.get();
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			});

			assertThat(result.myVersion).isNotNull().isEqualTo(1);
			List<String> labels = getLabels(existingEntityId);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithVersion", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithVersion result = executeInTransaction(() -> {
					SimpleDynamicLabelsWithVersion entity = new SimpleDynamicLabelsWithVersion();
					entity.moreLabels = new HashSet<>();
					entity.moreLabels.add("A");
					entity.moreLabels.add("B");
					entity.moreLabels.add("C");
					return template.save(entity);
			});

			assertThat(result.myVersion).isNotNull().isEqualTo(0);
			List<String> labels = getLabels(result.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithVersion", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelAssignedIdAndVersion extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(
					"" + "CREATE (e:SimpleDynamicLabelsWithBusinessIdAndVersion:Foo:Bar:Baz:Foobar {id: 'E2', myVersion: 0}) "
							+ "RETURN id(e) as existingEntityId")
					.single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessIdAndVersion result = executeInTransaction(() -> {
				SimpleDynamicLabelsWithBusinessIdAndVersion entity = template
						.findById("E2", SimpleDynamicLabelsWithBusinessIdAndVersion.class).get();
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			});

			assertThat(result.myVersion).isNotNull().isEqualTo(1);
			List<String> labels = getLabels(existingEntityId);
			assertThat(labels)
					.containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessIdAndVersion", "Fizz", "Bar", "Baz",
							"Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessIdAndVersion result = executeInTransaction(() -> {
				SimpleDynamicLabelsWithBusinessIdAndVersion entity = new SimpleDynamicLabelsWithBusinessIdAndVersion();
				entity.id = UUID.randomUUID().toString();
				entity.moreLabels = new HashSet<>();
				entity.moreLabels.add("A");
				entity.moreLabels.add("B");
				entity.moreLabels.add("C");
				return template.save(entity);
			});

			assertThat(result.myVersion).isNotNull().isEqualTo(0);
			List<String> labels = getLabels(Cypher.anyNode("n").property("id").isEqualTo(parameter("id")), result.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessIdAndVersion", "A", "B", "C");
		}
	}

	@Nested
	class ConstructorInitializedEntity extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("" + "CREATE (e:SimpleDynamicLabelsCtor:Foo:Bar:Baz:Foobar) " + "RETURN id(e) as existingEntityId")
					.single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<SimpleDynamicLabelsCtor> optionalEntity = template.findById(existingEntityId,
					SimpleDynamicLabelsCtor.class);
			assertThat(optionalEntity).hasValueSatisfying(
					entity -> assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar"));
		}
	}

	@Nested
	class ClassesWithAdditionalLabels extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("" + "CREATE (e:SimpleDynamicLabels:Foo:Bar:Baz:Foobar) " + "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabelsOnClassWithSingleNodeLabel(@Autowired Neo4jTemplate template) {
			Optional<DynamicLabelsWithNodeLabel> optionalEntity = template.findById(existingEntityId,
					DynamicLabelsWithNodeLabel.class);
			assertThat(optionalEntity).hasValueSatisfying(entity -> assertThat(entity.moreLabels)
					.containsExactlyInAnyOrder("SimpleDynamicLabels", "Foo", "Bar", "Foobar"));
		}

		@Test
		void shouldReadDynamicLabelsOnClassWithMultipleNodeLabel(@Autowired Neo4jTemplate template) {

			Optional<DynamicLabelsWithMultipleNodeLabels> optionalEntity = template.findById(existingEntityId,
					DynamicLabelsWithMultipleNodeLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(
					entity -> assertThat(entity.moreLabels).containsExactlyInAnyOrder("SimpleDynamicLabels", "Baz", "Foobar"));
		}

	}

	@Nested
	class ClassesWithAdditionalLabelsInInheritanceTree extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(
					"" + "CREATE (e:DynamicLabelsBaseClass:ExtendedBaseClass1:D1:D2:D3) " + "RETURN id(e) as existingEntityId")
					.single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabelsInInheritance(@Autowired Neo4jTemplate template) {

			Optional<ExtendedBaseClass1> optionalEntity = template.findById(existingEntityId, ExtendedBaseClass1.class);
			assertThat(optionalEntity)
					.hasValueSatisfying(entity -> assertThat(entity.moreLabels).containsExactlyInAnyOrder("D1", "D2", "D3"));
		}
	}

	@ExtendWith(SpringExtension.class)
	@ContextConfiguration(classes = SpringTestBase.Config.class)
	@DirtiesContext
	abstract static class SpringTestBase {

		@Autowired protected Driver driver;

		@Autowired protected TransactionTemplate transactionTemplate;

		@Autowired protected BookmarkCapture bookmarkCapture;

		protected Long existingEntityId;

		abstract Long createTestEntity(Transaction t);

		<T> T executeInTransaction(Callable<T> runnable) {
			return transactionTemplate.execute(tx -> {
				try {
					return runnable.call();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
		}

		@BeforeEach
		void setupData() {
			try (Session session = driver.session()) {
				session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n").consume());
				existingEntityId = session.writeTransaction(this::createTestEntity);
				bookmarkCapture.seedWith(session.lastBookmark());
			}
		}

		protected final List<String> getLabels(Long id) {
			return getLabels(Cypher.anyNode().named("n").internalId().isEqualTo(parameter("id")), id);
		}

		protected final List<String> getLabels(Condition idCondition, Object id) {

			Node n = Cypher.anyNode("n");
			String cypher = Renderer.getDefaultRenderer().render(Cypher.match(n).where(idCondition)
					.and(not(exists(n.property("moreLabels")))).returning(n.labels().as("labels")).build());

			try (Session session = driver.session(bookmarkCapture.createSessionConfig())) {
				return session.readTransaction(
						tx -> tx.run(cypher, Collections.singletonMap("id", id)).single().get("labels").asList(Value::asString));
			}
		}

		@Configuration
		@EnableTransactionManagement
		static class Config extends AbstractNeo4jConfig {

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

			@Bean
			public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
				return new TransactionTemplate(transactionManager);
			}
		}
	}
}
