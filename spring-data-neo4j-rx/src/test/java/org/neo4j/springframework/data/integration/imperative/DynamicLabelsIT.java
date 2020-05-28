/*
 * Copyright (c) 2019-2020 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.imperative;

import static org.assertj.core.api.Assertions.*;
import static org.neo4j.opencypherdsl.Conditions.not;
import static org.neo4j.opencypherdsl.Cypher.*;
import static org.neo4j.opencypherdsl.Predicates.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.Value;
import org.neo4j.opencypherdsl.Condition;
import org.neo4j.opencypherdsl.Cypher;
import org.neo4j.opencypherdsl.Node;
import org.neo4j.opencypherdsl.renderer.Renderer;
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.core.Neo4jTemplate;
import org.neo4j.springframework.data.integration.shared.EntitiesWithDynamicLabels.*;
import org.neo4j.springframework.data.test.Neo4jExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;

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
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabels:Foo:Bar:Baz:Foobar) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<SimpleDynamicLabels> optionalEntity = template
				.findById(existingEntityId, SimpleDynamicLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar")
			);
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabels entity = template
				.findById(existingEntityId, SimpleDynamicLabels.class).get();
			entity.moreLabels.remove("Foo");
			entity.moreLabels.add("Fizz");
			template.save(entity);

			List<String> labels = getLabels(existingEntityId);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabels entity = new SimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			long id = template.save(entity).id;

			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "A", "B", "C");
		}

		@Test
		void shouldWriteDynamicLabelsFromRelatedNodes(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabels entity = new SimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			SuperNode superNode = new SuperNode();
			superNode.relatedTo = entity;
			long id = template.save(superNode).relatedTo.id;
			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabels", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithInheritedDynamicLabels extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:InheritedSimpleDynamicLabels:Foo:Bar:Baz:Foobar) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<InheritedSimpleDynamicLabels> optionalEntity = template
				.findById(existingEntityId, InheritedSimpleDynamicLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar")
			);
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			InheritedSimpleDynamicLabels entity = template
				.findById(existingEntityId, InheritedSimpleDynamicLabels.class).get();
			entity.moreLabels.remove("Foo");
			entity.moreLabels.add("Fizz");
			template.save(entity);

			List<String> labels = getLabels(existingEntityId);
			assertThat(labels)
				.containsExactlyInAnyOrder("InheritedSimpleDynamicLabels", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			InheritedSimpleDynamicLabels entity = new InheritedSimpleDynamicLabels();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			long id = template.save(entity).id;

			List<String> labels = getLabels(id);
			assertThat(labels).containsExactlyInAnyOrder("InheritedSimpleDynamicLabels", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelAndAssignedId extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabelsWithBusinessId:Foo:Bar:Baz:Foobar {id: 'E1'}) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessId entity = template
				.findById("E1", SimpleDynamicLabelsWithBusinessId.class).get();
			entity.moreLabels.remove("Foo");
			entity.moreLabels.add("Fizz");
			template.save(entity);
			List<String> labels = getLabels(existingEntityId);
			assertThat(labels)
				.containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessId", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessId entity = new SimpleDynamicLabelsWithBusinessId();
			entity.id = UUID.randomUUID().toString();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			template.save(entity);
			List<String> labels = getLabels(Cypher.anyNode("n").property("id").isEqualTo(parameter("id")), entity.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessId", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelGeneratedIdAndVersion extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabelsWithVersion:Foo:Bar:Baz:Foobar {myVersion: 0}) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithVersion entity = template
				.findById(existingEntityId, SimpleDynamicLabelsWithVersion.class).get();
			entity.moreLabels.remove("Foo");
			entity.moreLabels.add("Fizz");
			entity = template.save(entity);

			assertThat(entity.myVersion).isNotNull().isEqualTo(1);
			List<String> labels = getLabels(existingEntityId);
			assertThat(labels)
				.containsExactlyInAnyOrder("SimpleDynamicLabelsWithVersion", "Fizz", "Bar", "Baz", "Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithVersion entity = new SimpleDynamicLabelsWithVersion();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			entity = template.save(entity);

			assertThat(entity.myVersion).isNotNull().isEqualTo(0);
			List<String> labels = getLabels(entity.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithVersion", "A", "B", "C");
		}
	}

	@Nested
	class EntityWithSingleStaticLabelAssignedIdAndVersion extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabelsWithBusinessIdAndVersion:Foo:Bar:Baz:Foobar {id: 'E2', myVersion: 0}) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldUpdateDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessIdAndVersion entity = template
				.findById("E2", SimpleDynamicLabelsWithBusinessIdAndVersion.class).get();
			entity.moreLabels.remove("Foo");
			entity.moreLabels.add("Fizz");
			entity = template.save(entity);
			assertThat(entity.myVersion).isNotNull().isEqualTo(1);
			List<String> labels = getLabels(existingEntityId);
			assertThat(labels)
				.containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessIdAndVersion", "Fizz", "Bar", "Baz",
					"Foobar");
		}

		@Test
		void shouldWriteDynamicLabels(@Autowired Neo4jTemplate template) {

			SimpleDynamicLabelsWithBusinessIdAndVersion entity = new SimpleDynamicLabelsWithBusinessIdAndVersion();
			entity.id = UUID.randomUUID().toString();
			entity.moreLabels = new HashSet<>();
			entity.moreLabels.add("A");
			entity.moreLabels.add("B");
			entity.moreLabels.add("C");
			entity = template.save(entity);
			assertThat(entity.myVersion).isNotNull().isEqualTo(0);

			List<String> labels = getLabels(Cypher.anyNode("n").property("id").isEqualTo(parameter("id")), entity.id);
			assertThat(labels).containsExactlyInAnyOrder("SimpleDynamicLabelsWithBusinessIdAndVersion", "A", "B", "C");
		}
	}

	@Nested
	class ConstructorInitializedEntity extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabelsCtor:Foo:Bar:Baz:Foobar) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabels(@Autowired Neo4jTemplate template) {

			Optional<SimpleDynamicLabelsCtor> optionalEntity = template
				.findById(existingEntityId, SimpleDynamicLabelsCtor.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("Foo", "Bar", "Baz", "Foobar")
			);
		}
	}

	@Nested
	class ClassesWithAdditionalLabels extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:SimpleDynamicLabels:Foo:Bar:Baz:Foobar) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabelsOnClassWithSingleNodeLabel(@Autowired Neo4jTemplate template) {
			Optional<DynamicLabelsWithNodeLabel> optionalEntity = template
				.findById(existingEntityId, DynamicLabelsWithNodeLabel.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("SimpleDynamicLabels", "Foo", "Bar", "Foobar")
			);
		}

		@Test
		void shouldReadDynamicLabelsOnClassWithMultipleNodeLabel(@Autowired Neo4jTemplate template) {

			Optional<DynamicLabelsWithMultipleNodeLabels> optionalEntity = template
				.findById(existingEntityId, DynamicLabelsWithMultipleNodeLabels.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("SimpleDynamicLabels", "Baz", "Foobar")
			);
		}

	}

	@Nested
	class ClassesWithAdditionalLabelsInInheritanceTree extends SpringTestBase {

		@Override
		Long createTestEntity(Transaction transaction) {
			Record r = transaction.run(""
				+ "CREATE (e:DynamicLabelsBaseClass:ExtendedBaseClass1:D1:D2:D3) "
				+ "RETURN id(e) as existingEntityId").single();
			long newId = r.get("existingEntityId").asLong();
			transaction.commit();
			return newId;
		}

		@Test
		void shouldReadDynamicLabelsInInheritance(@Autowired Neo4jTemplate template) {

			Optional<ExtendedBaseClass1> optionalEntity = template
				.findById(existingEntityId, ExtendedBaseClass1.class);
			assertThat(optionalEntity).hasValueSatisfying(entity ->
				assertThat(entity.moreLabels).containsExactlyInAnyOrder("D1", "D2", "D3")
			);
		}
	}

	@ExtendWith(SpringExtension.class)
	@ContextConfiguration(classes = SpringTestBase.Config.class)
	abstract static class SpringTestBase {

		@Autowired
		protected Driver driver;

		protected Long existingEntityId;

		abstract Long createTestEntity(Transaction t);

		@BeforeEach
		void setupData() {
			try (Session session = driver.session();) {
				session.writeTransaction(tx -> tx.run("MATCH (n) DETACH DELETE n").consume());
				existingEntityId = session.writeTransaction(this::createTestEntity);
			}
		}

		protected final List<String> getLabels(Long id) {
			return getLabels(Cypher.anyNode().named("n").internalId().isEqualTo(parameter("id")), id);
		}

		protected final List<String> getLabels(Condition idCondition, Object id) {

			Node n = Cypher.anyNode("n");
			String cypher = Renderer.getDefaultRenderer().render(Cypher
				.match(n)
				.where(idCondition).and(not(exists(n.property("moreLabels"))))
				.returning(n.labels().as("labels"))
				.build()
			);

			try (Session session = driver.session()) {
				return session
					.readTransaction(tx -> tx
						.run(cypher, Collections.singletonMap("id", id))
						.single().get("labels")
						.asList(Value::asString)
					);
			}
		}

		@Configuration
		@EnableTransactionManagement
		static class Config extends AbstractNeo4jConfig {

			@Bean
			public Driver driver() {
				return neo4jConnectionSupport.getDriver();
			}

		}
	}

}
