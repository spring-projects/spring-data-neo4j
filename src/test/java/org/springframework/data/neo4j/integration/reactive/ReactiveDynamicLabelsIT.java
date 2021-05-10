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
package org.springframework.data.neo4j.integration.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.neo4j.cypherdsl.core.Conditions.not;
import static org.neo4j.cypherdsl.core.Predicates.exists;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
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
import org.neo4j.driver.reactive.RxSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractReactiveNeo4jConfig;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveNeo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;
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
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.reactive.TransactionalOperator;

/**
 * @author Michael J. Simons
 */
@Tag(Neo4jExtension.NEEDS_REACTIVE_SUPPORT)
@ExtendWith(Neo4jExtension.class)
public class ReactiveDynamicLabelsIT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Nested
	class EntityWithSingleStaticLabelAndGeneratedId extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("" + "CREATE (e:SimpleDynamicLabels:Foo:Bar:Baz:Foobar) " + "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, SimpleDynamicLabels.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Foo", "Foobar").verifyComplete();
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, SimpleDynamicLabels.class).flatMap(entity -> {
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			}).as(transactionalOperator::transactional)
					.thenMany(getLabels(existingEntityId)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Fizz", "Foobar", "SimpleDynamicLabels").verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			SimpleDynamicLabels entity = new SimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");

			template.save(entity).map(SimpleDynamicLabels::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(this::getLabels).sort().as(StepVerifier::create)
					.expectNext("A", "B", "C", "SimpleDynamicLabels").verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabelsFromRelatedNodes(@Autowired ReactiveNeo4jTemplate template) {

			SimpleDynamicLabels entity = new SimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			SuperNode superNode = new SuperNode();
			superNode.relatedTo = entity;

			template.save(superNode).map(SuperNode::getRelatedTo).map(SimpleDynamicLabels::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(this::getLabels)
					.sort().as(StepVerifier::create).expectNext("A", "B", "C", "SimpleDynamicLabels").verifyComplete();
		}
	}

	@Nested
	class EntityWithInheritedDynamicLabels extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction
					.run("" + "CREATE (e:InheritedSimpleDynamicLabels:Foo:Bar:Baz:Foobar) " + "RETURN id(e) as existingEntityId")
					.single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, InheritedSimpleDynamicLabels.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Foo", "Foobar").verifyComplete();
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, InheritedSimpleDynamicLabels.class).flatMap(entity -> {
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			}).as(transactionalOperator::transactional)
					.thenMany(getLabels(existingEntityId)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Fizz", "Foobar", "InheritedSimpleDynamicLabels").verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			InheritedSimpleDynamicLabels entity = new InheritedSimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");

			template.save(entity).map(SimpleDynamicLabels::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(this::getLabels).sort().as(StepVerifier::create)
					.expectNext("A", "B", "C", "InheritedSimpleDynamicLabels").verifyComplete();
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
		void shouldUpdateDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById("E1", SimpleDynamicLabelsWithBusinessId.class).flatMap(entity -> {
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			}).as(transactionalOperator::transactional)
					.thenMany(getLabels(existingEntityId)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Fizz", "Foobar", "SimpleDynamicLabelsWithBusinessId").verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessId entity = new SimpleDynamicLabelsWithBusinessId();
			entity.id = UUID.randomUUID().toString();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");

			template.save(entity).map(SimpleDynamicLabelsWithBusinessId::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(id -> getLabels(Cypher.anyNode("n").property("id").isEqualTo(Cypher.parameter("id")), id)).sort()
					.as(StepVerifier::create).expectNext("A", "B", "C", "SimpleDynamicLabelsWithBusinessId").verifyComplete();
		}
	}

	@Nested
	class EntityWithSingleStaticLabelGeneratedIdAndVersion extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run("" + "CREATE (e:SimpleDynamicLabelsWithVersion:Foo:Bar:Baz:Foobar {myVersion: 0}) "
					+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, SimpleDynamicLabelsWithVersion.class).flatMap(entity -> {
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			}).doOnNext(e -> assertThat(e.myVersion).isNotNull().isEqualTo(1))
					.as(transactionalOperator::transactional)
					.thenMany(getLabels(existingEntityId)).sort()
					.as(StepVerifier::create).expectNext("Bar", "Baz", "Fizz", "Foobar", "SimpleDynamicLabelsWithVersion")
					.verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			SimpleDynamicLabelsWithVersion entity = new SimpleDynamicLabelsWithVersion();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");

			template.save(entity).doOnNext(e -> assertThat(e.myVersion).isNotNull().isEqualTo(0))
					.map(SimpleDynamicLabelsWithVersion::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(this::getLabels).sort().as(StepVerifier::create)
					.expectNext("A", "B", "C", "SimpleDynamicLabelsWithVersion").verifyComplete();
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
		void shouldUpdateDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById("E2", SimpleDynamicLabelsWithBusinessIdAndVersion.class).flatMap(entity -> {
				entity.moreLabels.remove("Foo");
				entity.moreLabels.add("Fizz");
				return template.save(entity);
			}).doOnNext(e -> assertThat(e.myVersion).isNotNull().isEqualTo(1))
					.map(SimpleDynamicLabelsWithBusinessIdAndVersion::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(id -> getLabels(Cypher.anyNode("n").property("id").isEqualTo(Cypher.parameter("id")), id)).sort()
					.as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Fizz", "Foobar", "SimpleDynamicLabelsWithBusinessIdAndVersion").verifyComplete();
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessIdAndVersion entity = new SimpleDynamicLabelsWithBusinessIdAndVersion();
			entity.id = UUID.randomUUID().toString();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");

			template.save(entity).doOnNext(e -> assertThat(e.myVersion).isNotNull().isEqualTo(0))
					.map(SimpleDynamicLabelsWithBusinessIdAndVersion::getId)
					.as(transactionalOperator::transactional)
					.flatMapMany(id -> getLabels(Cypher.anyNode("n").property("id").isEqualTo(Cypher.parameter("id")), id)).sort()
					.as(StepVerifier::create).expectNext("A", "B", "C", "SimpleDynamicLabelsWithBusinessIdAndVersion")
					.verifyComplete();
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
		void shouldReadDynamicLabels(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, SimpleDynamicLabelsCtor.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Baz", "Foo", "Foobar");
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
		void shouldReadDynamicLabelsOnClassWithSingleNodeLabel(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, DynamicLabelsWithNodeLabel.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("Bar", "Foo", "Foobar", "SimpleDynamicLabels");
		}

		@Test
		void shouldReadDynamicLabelsOnClassWithMultipleNodeLabel(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, DynamicLabelsWithMultipleNodeLabels.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("Baz", "Foobar", "SimpleDynamicLabels");
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
		void shouldReadDynamicLabelsInInheritance(@Autowired ReactiveNeo4jTemplate template) {

			template.findById(existingEntityId, ExtendedBaseClass1.class)
					.flatMapMany(entity -> Flux.fromIterable(entity.moreLabels)).sort().as(StepVerifier::create)
					.expectNext("D1", "D2", "D3");
		}
	}

	@ExtendWith(SpringExtension.class)
	@ContextConfiguration(classes = SpringTestBase.Config.class)
	@DirtiesContext
	abstract static class SpringTestBase {

		@Autowired protected Driver driver;

		@Autowired protected TransactionalOperator transactionalOperator;

		@Autowired protected BookmarkCapture bookmarkCapture;

		protected Long existingEntityId;

		abstract Long createTestEntity(Transaction t);

		@BeforeEach
		void setupData() {
			try (Session session = driver.session();) {
				session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n").consume());
				existingEntityId = session.writeTransaction(this::createTestEntity);
				bookmarkCapture.seedWith(session.lastBookmark());
			}
		}

		protected final Flux<String> getLabels(Long id) {
			return getLabels(Cypher.anyNode().named("n").internalId().isEqualTo(Cypher.parameter("id")), id);
		}

		protected final Flux<String> getLabels(Condition idCondition, Object id) {

			Node n = Cypher.anyNode("n");
			String cypher = Renderer.getDefaultRenderer().render(Cypher.match(n).where(idCondition)
					.and(not(exists(n.property("moreLabels")))).unwind(n.labels()).as("label").returning("label").build());
			return Flux
					.usingWhen(Mono.fromSupplier(() -> driver.rxSession(bookmarkCapture.createSessionConfig())),
							s -> s.run(cypher, Collections.singletonMap("id", id)).records(), RxSession::close)
					.map(r -> r.get("label").asString());
		}

		@Configuration
		@EnableTransactionManagement
		static class Config extends AbstractReactiveNeo4jConfig {

			@Bean
			public Driver driver() {
				return neo4jConnectionSupport.getDriver();
			}

			@Bean
			public BookmarkCapture bookmarkCapture() {
				return new BookmarkCapture();
			}

			@Override
			public ReactiveTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseNameProvider) {

				BookmarkCapture bookmarkCapture = bookmarkCapture();
				return new ReactiveNeo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture));
			}

			@Bean
			public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
				return TransactionalOperator.create(transactionManager);
			}
		}
	}

}
