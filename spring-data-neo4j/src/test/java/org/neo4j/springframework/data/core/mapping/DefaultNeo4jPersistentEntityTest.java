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
package org.neo4j.springframework.data.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Property;
import org.neo4j.springframework.data.core.schema.Relationship;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class DefaultNeo4jPersistentEntityTest {

	@Test
	void persistentEntityCreationWorksForCorrectEntity() {
		Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
		neo4jMappingContext.getPersistentEntity(CorrectEntity1.class);
		neo4jMappingContext.getPersistentEntity(CorrectEntity2.class);
	}

	@Nested
	class DuplicateProperties {
		@Test
		void failsOnDuplicatedProperties() {
			assertThatIllegalStateException()
				.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(EntityWithDuplicatedProperties.class))
				.withMessage("Duplicate definition of property [name] in entity class "
					+ "org.neo4j.springframework.data.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithDuplicatedProperties.");
		}

		@Test
		void failsOnMultipleDuplicatedProperties() {
			assertThatIllegalStateException()
				.isThrownBy(
					() -> new Neo4jMappingContext().getPersistentEntity(EntityWithMultipleDuplicatedProperties.class))
				.withMessage("Duplicate definition of properties [foo, name] in entity class "
					+ "org.neo4j.springframework.data.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithMultipleDuplicatedProperties.");
		}
	}

	@Nested
	class Relationships {

		@Test
		void failsOnDynamicRelationshipsWithExplicitType() {
			assertThatIllegalStateException()
				.isThrownBy(
					() -> new Neo4jMappingContext().getPersistentEntity(MixedDynamicAndExplicitRelationship.class))
				.withMessage("Dynamic relationships cannot be used with a fixed type. Omit @Relationship or use @Relationship(direction = INCOMING).");
		}
	}

	@Nested
	class Labels {

		@Test
		void supportDerivedLabel() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(CorrectEntity1.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("CorrectEntity1");
			assertThat(persistentEntity.getAdditionalLabels()).isEmpty();
		}

		@Test
		void supportSingleLabel() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(EntityWithSingleLabel.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("a");
			assertThat(persistentEntity.getAdditionalLabels()).isEmpty();
		}

		@Test
		void supportMultipleLabels() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(EntityWithMultipleLabels.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("a");
			assertThat(persistentEntity.getAdditionalLabels()).containsExactlyInAnyOrder("b", "c");
		}

		@Test
		void supportExplicitPrimaryLabel() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(EntityWithExplicitPrimaryLabel.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("a");
			assertThat(persistentEntity.getAdditionalLabels()).isEmpty();
		}

		@Test
		void supportExplicitPrimaryLabelAndAdditionalLabels() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(EntityWithExplicitPrimaryLabelAndAdditionalLabels.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("a");
			assertThat(persistentEntity.getAdditionalLabels()).containsExactlyInAnyOrder("b", "c");
		}

		@Test
		void supportInheritedPrimaryLabelAndAdditionalLabels() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> parentEntity = neo4jMappingContext
				.getPersistentEntity(BaseClass.class);
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext
				.getPersistentEntity(Child.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("Child");
			assertThat(persistentEntity.getAdditionalLabels()).containsExactlyInAnyOrder("Base", "Bases", "Person");
		}
	}

	@Node
	private static class CorrectEntity1 {

		@Id private Long id;

		private String name;

		private Map<String, CorrectEntity1> dynamicRelationships;
	}

	@Node
	private static class CorrectEntity2 {

		@Id private Long id;

		private String name;

		@Relationship(direction = Relationship.Direction.INCOMING)
		private Map<String, CorrectEntity2> dynamicRelationships;
	}

	@Node
	private static class MixedDynamicAndExplicitRelationship {

		@Id private Long id;

		private String name;

		@Relationship(type = "BAMM", direction = Relationship.Direction.INCOMING)
		private Map<String, MixedDynamicAndExplicitRelationship> dynamicRelationships;
	}

	@Node
	private static class EntityWithDuplicatedProperties {

		@Id private Long id;

		private String name;

		@Property("name") private String alsoName;
	}

	@Node
	private static class EntityWithMultipleDuplicatedProperties {

		@Id private Long id;

		private String name;

		@Property("name") private String alsoName;

		@Property("foo")
		private String somethingElse;

		@Property("foo")
		private String thisToo;
	}

	@Node("a")
	private static class EntityWithSingleLabel {
		@Id private Long id;
	}

	@Node({"a", "b", "c"})
	private static class EntityWithMultipleLabels {
		@Id private Long id;
	}

	@Node(primaryLabel = "a")
	private static class EntityWithExplicitPrimaryLabel {
		@Id private Long id;
	}

	@Node(primaryLabel = "a", labels = {"b", "c"})
	private static class EntityWithExplicitPrimaryLabelAndAdditionalLabels {
		@Id private Long id;
	}

	@Node(primaryLabel = "Base", labels = {"Bases"})
	private static abstract class BaseClass {
		@Id private Long id;
	}

	@Node(primaryLabel = "Child", labels = {"Person"})
	private static class Child extends BaseClass {
		private String name;
	}

}
