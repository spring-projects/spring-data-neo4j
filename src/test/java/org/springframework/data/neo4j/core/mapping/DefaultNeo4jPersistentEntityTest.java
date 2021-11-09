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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.annotation.ReadOnlyProperty;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.AssociationHandler;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.neo4j.core.schema.DynamicLabels;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

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
	class ReadOnlyProperties {

		private final Neo4jMappingContext neo4jMappingContext;

		ReadOnlyProperties() {

			neo4jMappingContext = new Neo4jMappingContext();
			neo4jMappingContext.setInitialEntitySet(Collections.singleton(WithAnnotatedProperties.class));
		}

		@ParameterizedTest // GH-2376
		@ValueSource(strings = {"defaultProperty", "defaultAnnotatedProperty", "writableProperty"})
		void propertiesShouldBeWritable(String propertyName) {

			Neo4jPersistentProperty property = neo4jMappingContext.getPersistentEntity(WithAnnotatedProperties.class)
					.getRequiredPersistentProperty(propertyName);
			assertThat(property.isReadOnly()).isFalse();
		}

		@ParameterizedTest // GH-2376, GH-2294
		@ValueSource(strings = {"readOnlyProperty", "usingSpringsAnnotation"})
		void propertiesShouldBeReadOnly(String propertyName) {

			Neo4jPersistentProperty property = neo4jMappingContext.getPersistentEntity(WithAnnotatedProperties.class)
					.getRequiredPersistentProperty(propertyName);
			assertThat(property.isReadOnly()).isTrue();
		}
	}

	@Nested
	class DuplicateProperties {
		@Test
		void failsOnDuplicatedProperties() {
			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(EntityWithDuplicatedProperties.class))
					.withMessage("Duplicate definition of property [name] in entity class "
							+ "org.springframework.data.neo4j.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithDuplicatedProperties.");
		}

		@Test
		void failsOnMultipleDuplicatedProperties() {
			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(EntityWithMultipleDuplicatedProperties.class))
					.withMessage("Duplicate definition of properties [foo, name] in entity class "
							+ "org.springframework.data.neo4j.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithMultipleDuplicatedProperties.");
		}

		@Test // GH-1903
		void failsOnMultipleInheritedDuplicatedProperties() {
			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(
							EntityWithInheritedMultipleDuplicatedProperties.class))
					.withMessage("Duplicate definition of property [name] in entity class "
								 + "org.springframework.data.neo4j.core.mapping.DefaultNeo4jPersistentEntityTest$EntityWithInheritedMultipleDuplicatedProperties.");
		}

		@Test // GH-1903
		void doesNotFailOnTransientInheritedDuplicatedProperties() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(
					EntityWithNotInheritedTransientProperties.class);

			assertThat(persistentEntity).isNotNull();
			assertThat(persistentEntity.getPersistentProperty("name")).isNotNull();
		}
	}

	@Nested
	class Relationships {

		@ParameterizedTest
		@ValueSource(classes = { MixedDynamicAndExplicitRelationship1.class, MixedDynamicAndExplicitRelationship2.class })
		void failsOnDynamicRelationshipsWithExplicitType(Class<?> entityToTest) {

			String expectedMessage = "Dynamic relationships cannot be used with a fixed type\\. Omit @Relationship or use @Relationship\\(direction = (OUTGOING|INCOMING)\\) without a type in class .*MixedDynamicAndExplicitRelationship\\d on field dynamicRelationships\\.";
			assertThatIllegalStateException().isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(entityToTest))
					.withMessageMatching(expectedMessage);
		}

		@ParameterizedTest // GH-216
		@ValueSource(classes = { TypeWithInvalidDynamicRelationshipMappings1.class,
				TypeWithInvalidDynamicRelationshipMappings2.class, TypeWithInvalidDynamicRelationshipMappings3.class })
		void multipleDynamicAssociationsToTheSameEntityAreNotAllowed(Class<?> entityToTest) {

			String expectedMessage = ".*TypeWithInvalidDynamicRelationshipMappings\\d already contains a dynamic relationship to class org\\.springframework\\.data\\.neo4j\\.core\\.mapping\\.Neo4jMappingContextTest\\$BikeNode. Only one dynamic relationship between to entities is permitted\\.";
			Neo4jMappingContext schema = new Neo4jMappingContext();
			schema.setInitialEntitySet(new HashSet<>(Arrays.asList(entityToTest)));
			assertThatIllegalStateException().isThrownBy(() -> schema.initialize()).withMessageMatching(expectedMessage);
		}

		@Test // DATAGRAPH-1420
		void doesNotFailOnCorrectRelationshipProperties() {
			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
					.getPersistentEntity(EntityWithCorrectRelationshipProperties.class);

			assertThat(persistentEntity).isNotNull();
		}

		@Test // DATAGRAPH-1420
		void doesFailOnRelationshipPropertiesWithMissingTargetNode() {

			assertThatExceptionOfType(MappingException.class)
					.isThrownBy(() -> new Neo4jMappingContext()
							.getPersistentEntity(EntityWithInCorrectRelationshipProperties.class))
					.withMessageContaining("Missing @TargetNode declaration in");
		}

		@Test // GH-2289
		void correctlyFindRelationshipObverseSameEntity() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(EntityWithBidirectionalRelationship.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
		}

		@Test // GH-2289
		void correctlyFindRelationshipObverse() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(EntityWithBidirectionalRelationshipToOtherEntity.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
			persistentEntity = neo4jMappingContext.getPersistentEntity(OtherEntityWithBidirectionalRelationship.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
		}

		@Test // GH-2289
		void correctlyFindRelationshipObverseWithRelationshipProperties() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(EntityWithBidirectionalRelationshipToOtherEntityWithRelationshipProperties.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
			persistentEntity = neo4jMappingContext.getPersistentEntity(OtherEntityWithBidirectionalRelationship.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
		}

		@Test // GH-2289
		void correctlyFindSameEntityRelationshipObverseWithRelationshipProperties() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(EntityWithBidirectionalRelationshipProperties.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNotNull();
			});
		}

		@Test // GH-2289
		void correctlyDontFindRelationshipObverse() {
			Neo4jMappingContext neo4jMappingContext = new Neo4jMappingContext();
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(EntityLooksLikeHasObserve.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNull();
			});
			persistentEntity = neo4jMappingContext.getPersistentEntity(OtherEntityLooksLikeHasObserve.class);
			persistentEntity.doWithAssociations((AssociationHandler<Neo4jPersistentProperty>) a -> {
				RelationshipDescription rd = (RelationshipDescription) a;
				assertThat(rd.getRelationshipObverse()).isNull();
			});
		}
	}

	@Nested
	class Labels {

		@Test
		void supportDerivedLabel() {

			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(CorrectEntity1.class);

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
			Neo4jPersistentEntity<?> parentEntity = neo4jMappingContext.getPersistentEntity(BaseClass.class);
			Neo4jPersistentEntity<?> persistentEntity = neo4jMappingContext.getPersistentEntity(Child.class);

			assertThat(persistentEntity.getPrimaryLabel()).isEqualTo("Child");
			assertThat(persistentEntity.getAdditionalLabels()).containsExactlyInAnyOrder("Base", "Bases", "Person");
		}

		@Test
		void validDynamicLabels() {

			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
					.getPersistentEntity(NodeWithDynamicLabels.class);

			assertThat(persistentEntity.getGraphProperties()).hasSize(2);
			assertThat(persistentEntity.getPersistentProperty("id").isIdProperty()).isTrue();

			assertThat(persistentEntity.getPersistentProperty("relatedTo").isDynamicLabels()).isFalse();
			assertThat(persistentEntity.getPersistentProperty("relatedTo").isAssociation()).isTrue();
			assertThat(persistentEntity.getPersistentProperty("relatedTo").isIdProperty()).isFalse();
			Assertions.assertThat(persistentEntity.getPersistentProperty("relatedTo").isRelationship()).isTrue();

			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isDynamicLabels()).isTrue();
			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isAssociation()).isFalse();
			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isIdProperty()).isFalse();
			Assertions.assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isRelationship()).isFalse();

			assertThat(persistentEntity.getDynamicLabelsProperty())
					.hasValueSatisfying(p -> p.getFieldName().equals("dynamicLabels"));
		}

		@Test
		void shouldDetectValidInheritedDynamicLabels() {

			Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
					.getPersistentEntity(ValidInheritedDynamicLabels.class);

			assertThat(persistentEntity.getGraphProperties()).hasSize(2);
			assertThat(persistentEntity.getPersistentProperty("id").isIdProperty()).isTrue();

			assertThat(persistentEntity.getPersistentProperty("relatedTo").isDynamicLabels()).isFalse();
			assertThat(persistentEntity.getPersistentProperty("relatedTo").isAssociation()).isTrue();
			assertThat(persistentEntity.getPersistentProperty("relatedTo").isIdProperty()).isFalse();
			Assertions.assertThat(persistentEntity.getPersistentProperty("relatedTo").isRelationship()).isTrue();

			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isDynamicLabels()).isTrue();
			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isAssociation()).isFalse();
			assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isIdProperty()).isFalse();
			Assertions.assertThat(persistentEntity.getPersistentProperty("dynamicLabels").isRelationship()).isFalse();

			assertThat(persistentEntity.getDynamicLabelsProperty())
					.hasValueSatisfying(p -> p.getFieldName().equals("dynamicLabels"));
		}

		@Test
		void shouldDetectInvalidInheritedDynamicLabels() {

			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(InvalidInheritedDynamicLabels.class))
					.withMessageMatching(
							"Multiple properties in entity class .*DefaultNeo4jPersistentEntityTest\\$InvalidInheritedDynamicLabels are annotated with @DynamicLabels: \\[dynamicLabels, localDynamicLabels\\].");
		}

		@Test
		void shouldDetectInvalidDynamicLabels() {

			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(NodeWithInvalidDynamicLabels.class))
					.withMessageMatching(
							"Multiple properties in entity class .*DefaultNeo4jPersistentEntityTest\\$NodeWithInvalidDynamicLabels are annotated with @DynamicLabels: \\[dynamicLabels, moarDynamicLabels\\].");
		}

		@Test
		void shouldDetectInvalidDynamicLabelsTarget() {

			assertThatIllegalStateException()
					.isThrownBy(() -> new Neo4jMappingContext().getPersistentEntity(InvalidDynamicLabels.class))
					.withMessageMatching(
							"Property dynamicLabels on class .*DefaultNeo4jPersistentEntityTest\\$InvalidDynamicLabels must extends java\\.util\\.Collection.");
		}
	}

	@Node
	private static class SomeOtherNode {
		@Id Long id;
	}

	@Node
	private static class NodeWithDynamicLabels {

		@Id @GeneratedValue Long id;

		List<SomeOtherNode> relatedTo;

		@DynamicLabels List<String> dynamicLabels;
	}

	@Node
	private static class NodeWithInvalidDynamicLabels {

		@Id @GeneratedValue Long id;

		@DynamicLabels List<String> dynamicLabels;

		@DynamicLabels List<String> moarDynamicLabels;
	}

	@Node
	private static class ValidInheritedDynamicLabels extends NodeWithDynamicLabels {}

	@Node
	private static class InvalidInheritedDynamicLabels extends NodeWithDynamicLabels {

		@DynamicLabels List<String> localDynamicLabels;
	}

	@Node
	private static class InvalidDynamicLabels {

		@Id @GeneratedValue Long id;

		@DynamicLabels String dynamicLabels;
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

		@Relationship(direction = Relationship.Direction.INCOMING) private Map<String, CorrectEntity2> dynamicRelationships;
	}

	@Node
	private static class MixedDynamicAndExplicitRelationship1 {

		@Id private Long id;

		private String name;

		@Relationship(type = "BAMM") private Map<String, MixedDynamicAndExplicitRelationship1> dynamicRelationships;
	}

	@Node
	private static class MixedDynamicAndExplicitRelationship2 {

		@Id private Long id;

		private String name;

		@Relationship(type = "BAMM",
				direction = Relationship.Direction.INCOMING) private Map<String, List<MixedDynamicAndExplicitRelationship2>> dynamicRelationships;
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

		@Property("foo") private String somethingElse;

		@Property("foo") private String thisToo;
	}

	private abstract static class BaseClassWithPrivatePropertyUnsafe {

		@Id @GeneratedValue
		private Long id;

		private String name;
	}

	@Node
	private static class EntityWithInheritedMultipleDuplicatedProperties extends BaseClassWithPrivatePropertyUnsafe {

		private String name;
	}

	private abstract static class BaseClassWithPrivatePropertySafe {

		@Id @GeneratedValue
		private Long id;

		private @Transient String name;
	}

	@Node
	private static class EntityWithNotInheritedTransientProperties extends BaseClassWithPrivatePropertySafe {

		private String name;
	}

	@Node("a")
	private static class EntityWithSingleLabel {
		@Id private Long id;
	}

	@Node({ "a", "b", "c" })
	private static class EntityWithMultipleLabels {
		@Id private Long id;
	}

	@Node(primaryLabel = "a")
	private static class EntityWithExplicitPrimaryLabel {
		@Id private Long id;
	}

	@Node(primaryLabel = "a", labels = { "b", "c" })
	private static class EntityWithExplicitPrimaryLabelAndAdditionalLabels {
		@Id private Long id;
	}

	@Node(primaryLabel = "Base", labels = { "Bases" })
	private static abstract class BaseClass {
		@Id private Long id;
	}

	@Node(primaryLabel = "Child", labels = { "Person" })
	private static class Child extends BaseClass {
		private String name;
	}

	@Node
	static class TypeWithInvalidDynamicRelationshipMappings1 {

		@Id private String id;

		private Map<String, Neo4jMappingContextTest.BikeNode> bikes1;

		private Map<String, Neo4jMappingContextTest.BikeNode> bikes2;
	}

	@Node
	static class TypeWithInvalidDynamicRelationshipMappings2 {

		@Id private String id;

		private Map<String, Neo4jMappingContextTest.BikeNode> bikes1;

		private Map<String, List<Neo4jMappingContextTest.BikeNode>> bikes2;
	}

	@Node
	static class TypeWithInvalidDynamicRelationshipMappings3 {

		@Id private String id;

		private Map<String, List<Neo4jMappingContextTest.BikeNode>> bikes1;

		private Map<String, List<Neo4jMappingContextTest.BikeNode>> bikes2;
	}

	@Node
	static class EntityWithCorrectRelationshipProperties {
		@Id private String id;
		@Relationship HasTargetNodeRelationshipProperties rel;
	}

	@Node
	static class EntityWithInCorrectRelationshipProperties {
		@Id private String id;
		@Relationship HasNoTargetNodeRelationshipProperties rel;
	}

	@RelationshipProperties
	static class HasTargetNodeRelationshipProperties {

		@RelationshipId
		private Long id;

		@TargetNode
		EntityWithExplicitPrimaryLabel entity;
	}

	@RelationshipProperties
	static class HasNoTargetNodeRelationshipProperties {

		@RelationshipId
		private Long id;
	}

	@Node
	static class EntityWithBidirectionalRelationship {

		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		List<EntityWithBidirectionalRelationship> knows;

		@Relationship(type = "KNOWS", direction = Relationship.Direction.INCOMING)
		List<EntityWithBidirectionalRelationship> knownBy;

	}

	@Node
	static class EntityWithBidirectionalRelationshipToOtherEntity {

		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		List<OtherEntityWithBidirectionalRelationship> knows;

	}

	@Node
	static class OtherEntityWithBidirectionalRelationship {

		@Id @GeneratedValue
		private Long id;

		@Relationship(type = "KNOWS", direction = Relationship.Direction.INCOMING)
		List<EntityWithBidirectionalRelationshipToOtherEntity> knownBy;

	}

	@Node
	static class EntityWithBidirectionalRelationshipToOtherEntityWithRelationshipProperties {

		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		List<OtherEntityWithBidirectionalRelationshipWithRelationshipPropertiesProperties> knows;

	}

	@Node
	static class OtherEntityWithBidirectionalRelationshipWithRelationshipProperties {

		@Id @GeneratedValue
		private Long id;

		@Relationship(type = "KNOWS", direction = Relationship.Direction.INCOMING)
		List<EntityWithBidirectionalRelationshipWithRelationshipPropertiesProperties> knownBy;

	}

	@RelationshipProperties
	static class OtherEntityWithBidirectionalRelationshipWithRelationshipPropertiesProperties {
		@RelationshipId
		private Long id;

		@TargetNode
		OtherEntityWithBidirectionalRelationshipWithRelationshipProperties target;
	}

	@RelationshipProperties
	static class EntityWithBidirectionalRelationshipWithRelationshipPropertiesProperties {
		@RelationshipId
		private Long id;

		@TargetNode
		EntityWithBidirectionalRelationshipToOtherEntityWithRelationshipProperties target;
	}

	@Node
	static class EntityWithBidirectionalRelationshipProperties {

		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		List<BidirectionalRelationshipProperties> knows;

		@Relationship(type = "KNOWS", direction = Relationship.Direction.INCOMING)
		List<BidirectionalRelationshipProperties> knownBy;

	}

	@RelationshipProperties
	static class BidirectionalRelationshipProperties {

		@RelationshipId
		private Long id;

		@TargetNode
		EntityWithBidirectionalRelationshipProperties target;
	}

	@Node
	static class EntityLooksLikeHasObserve {
		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		private List<OtherEntityLooksLikeHasObserve> knows;
	}

	@Node
	static class OtherEntityLooksLikeHasObserve {
		@Id @GeneratedValue
		private Long id;

		@Relationship("KNOWS")
		private List<EntityLooksLikeHasObserve> knows;
	}

	@Node
	static class WithAnnotatedProperties {

		@Id @GeneratedValue
		private Long id;

		private String defaultProperty;

		@Property
		private String defaultAnnotatedProperty;

		@Property(readOnly = true)
		private String readOnlyProperty;

		@ReadOnlyProperty
		private String usingSpringsAnnotation;

		@SuppressWarnings("DefaultAnnotationParam")
		@Property(readOnly = false)
		private String writableProperty;
	}
}
