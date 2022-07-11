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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Davide Fantuzzi
 * @author Andrea Santurbano
 * @author Michael J. Simons
 */
class CypherGeneratorTest {

	@Test
	void shouldCreateRelationshipCreationQueryWithLabelIfPresent() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(Entity1.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		when(relationshipDescription.isDynamic()).thenReturn(true);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL");

		String expectedQuery = "MATCH (startNode:`Entity1`) WHERE startNode.id = $fromId MATCH (endNode)"
							   + " WHERE id(endNode) = $toId MERGE (startNode)<-[relProps:`REL`]-(endNode) RETURN id(relProps) AS __internalNeo4jId__, toString(id(relProps)) AS __elementId__";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void shouldCreateRelationshipCreationQueryWithMultipleLabels() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity1.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		when(relationshipDescription.isDynamic()).thenReturn(true);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL");

		String expectedQuery =
				"MATCH (startNode:`Entity1`:`MultipleLabel`) WHERE startNode.id = $fromId MATCH (endNode)"
				+ " WHERE id(endNode) = $toId MERGE (startNode)<-[relProps:`REL`]-(endNode) RETURN id(relProps) AS __internalNeo4jId__, toString(id(relProps)) AS __elementId__";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void shouldCreateRelationshipCreationQueryWithoutUsingInternalIds() {
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		Neo4jPersistentEntity<?> persistentEntity = Mockito.mock(Neo4jPersistentEntity.class);
		Neo4jPersistentProperty persistentProperty = Mockito.mock(Neo4jPersistentProperty.class);

		when(relationshipDescription.isDynamic()).thenReturn(true);
		when(persistentEntity.isUsingInternalIds()).thenReturn(true);
		when(persistentEntity.getRequiredIdProperty()).thenReturn(persistentProperty);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL");

		String expectedQuery = "MATCH (startNode) WHERE id(startNode) = $fromId MATCH (endNode)"
							   + " WHERE id(endNode) = $toId MERGE (startNode)<-[relProps:`REL`]-(endNode) RETURN id(relProps) AS __internalNeo4jId__, toString(id(relProps)) AS __elementId__";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void shouldCreateRelationshipRemoveQueryWithLabelIfPresent() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(Entity1.class);
		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext().getPersistentEntity(Entity2.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode:`Entity1`)<-[rel]-(:`Entity2`) WHERE (startNode.id = $fromId AND NOT (id(rel) IN $__knownRelationShipIds__)) DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void shouldCreateRelationshipRemoveQueryWithMultipleLabels() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity1.class);
		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity2.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode:`Entity1`:`MultipleLabel`)<-[rel]-(:`Entity2`:`MultipleLabel`) WHERE (startNode.id = $fromId AND NOT (id(rel) IN $__knownRelationShipIds__)) DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void shouldCreateRelationshipRemoveQueryWithoutUsingInternalIds() {

		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext().getPersistentEntity(Entity2.class);

		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		Neo4jPersistentEntity<?> persistentEntity = Mockito.mock(Neo4jPersistentEntity.class);
		Neo4jPersistentProperty persistentProperty = Mockito.mock(Neo4jPersistentProperty.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		when(relationshipDescription.isDynamic()).thenReturn(true);
		when(persistentEntity.isUsingInternalIds()).thenReturn(true);
		when(persistentEntity.getRequiredIdProperty()).thenReturn(persistentProperty);

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode)<-[rel]-(:`Entity2`) WHERE (id(startNode) = $fromId AND NOT (id(rel) IN $__knownRelationShipIds__)) DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	private static Stream<Arguments> pageables() {
		return Stream.of(
				Arguments.of(Sort.by("a", "b").and(
						Sort.by(Sort.Order.asc("foo"), Sort.Order.desc("bar"))),
						Optional.of("ORDER BY a ASC, b ASC, foo ASC, bar DESC")),
				Arguments.of(null, Optional.empty()),
				Arguments.of(Sort.unsorted(), Optional.empty()),
				Arguments.of(Sort.by("n.a").ascending(), Optional.of("ORDER BY n.a ASC"))
		);
	}

	@ParameterizedTest // DATAGRAPH-1440
	@MethodSource("pageables")
	void shouldRenderOrderByFragment(Sort sort, Optional<String> expectValue) {

		Optional<String> fragment = Optional.ofNullable(CypherGenerator.INSTANCE.createOrderByFragment(sort));
		assertThat(fragment).isEqualTo(expectValue);
	}

	@Test
	void shouldFailOnInvalidPath() {

		assertThatIllegalArgumentException().isThrownBy(() -> CypherGenerator.INSTANCE.createOrderByFragment(Sort.by("n.")))
				.withMessageMatching("Cannot handle order property `.*`, it must be a simple property or one-hop path");
	}

	@Test
	void shouldFailOnInvalidPathWithMultipleHops() {

		assertThatIllegalArgumentException().isThrownBy(() -> CypherGenerator.INSTANCE.createOrderByFragment(Sort.by("n.n.n")))
				.withMessageMatching("Cannot handle order property `.*`, it must be a simple property or one-hop path");
	}

	@Test // GH-2474
	void shouldNotFailOnMultipleEscapedHops() {

		Optional<String> fragment = Optional.ofNullable(CypherGenerator.INSTANCE.createOrderByFragment(Sort.by("n.`a.b.c`")));
		assertThat(fragment).hasValue("ORDER BY n.`a.b.c` ASC");
	}

	@CsvSource(delimiterString = "|", value = {
			"apoc.text.clean(department.name)   |false| ORDER BY apoc.text.clean(department.name) ASC",
			"apoc.text.clean(department.name)   |true | ORDER BY apoc.text.clean(department.name) DESC",
			"apoc.text.clean()                  |true | ORDER BY apoc.text.clean() DESC",
			"date()                             |false| ORDER BY date() ASC",
			"date({year:1984, month:10, day:11})|false| ORDER BY date({year:1984, month:10, day:11}) ASC",
			"round(3.141592, 3)                 |false| ORDER BY round(3.141592, 3) ASC"
	})
	@ParameterizedTest // GH-2273
	void functionCallsShouldWork(String input, boolean descending, String expected) {

		Sort sort = Sort.by(input);
		if (descending) {
			sort = sort.descending();
		}
		String orderByFragment = CypherGenerator.INSTANCE.createOrderByFragment(sort);
		assertThat(orderByFragment).isEqualTo(expected);
	}

	@Test
	void shouldFailOnInvalidSymbolicNames() {

		assertThatIllegalArgumentException().isThrownBy(() -> CypherGenerator.INSTANCE.createOrderByFragment(Sort.by("()")))
				.withMessage("Name must be a valid identifier");
	}

	@Test
	void shouldCreateDynamicRelationshipPathQueryForEnumsWithoutWildcardRelationships() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(CyclicEntityWithEnumeratedDynamicRelationship1.class);

		org.neo4j.cypherdsl.core.Node rootNode = Cypher.anyNode(Constants.NAME_OF_ROOT_NODE);
		Collection<RelationshipDescription> relationships = persistentEntity.getRelationships();
		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(
				persistentEntity, relationships.iterator().next(), null, null).returning(rootNode).build();

		// we want to ensure that the pattern occurs three times but do not care about the order
		// of the relationship types
		Pattern relationshipTypesPattern =
				Pattern.compile("\\[__sr__:(`CORNERED`\\|`ROUND`|`ROUND`\\|`CORNERED`)]");

		Pattern untypedRelationshipsPattern = Pattern.compile("\\[__sr__]");

		String renderedStatement = Renderer.getDefaultRenderer().render(statement);
		assertThat(renderedStatement).containsPattern(relationshipTypesPattern);
		assertThat(renderedStatement).doesNotContainPattern(untypedRelationshipsPattern);
	}

	@Test
	void shouldCreateDynamicRelationshipPathQueryForStringsWithWildcardRelationships() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(CyclicEntityWithStringDynamicRelationship1.class);

		org.neo4j.cypherdsl.core.Node rootNode = Cypher.anyNode(Constants.NAME_OF_ROOT_NODE);
		Collection<RelationshipDescription> relationships = persistentEntity.getRelationships();
		Statement statement = CypherGenerator.INSTANCE.prepareMatchOf(
				persistentEntity, relationships.iterator().next(), null, null).returning(rootNode).build();

		Pattern untypedRelationshipsPattern = Pattern.compile("\\[__sr__]");
		Pattern typedRelationshipsPattern =	Pattern.compile("\\[__sr__:(`.*`)]");

		String renderedStatement = Renderer.getDefaultRenderer().render(statement);
		assertThat(renderedStatement).containsPattern(untypedRelationshipsPattern);
		assertThat(renderedStatement).doesNotContainPattern(typedRelationshipsPattern);
	}

	@Node
	private static class Entity1 {

		@Id private Long id;

		private String name;

		private Map<String, Entity1> dynamicRelationships;
	}

	@Node({ "Entity1", "MultipleLabel" })
	private static class MultipleLabelEntity1 {

		@Id private Long id;

		private String name;

		private Map<String, MultipleLabelEntity1> dynamicRelationships;
	}

	@Node
	private static class Entity2 {

		@Id private Long id;

		private String name;

		private Map<String, Entity2> dynamicRelationships;
	}

	@Node({ "Entity2", "MultipleLabel" })
	private static class MultipleLabelEntity2 {

		@Id private Long id;

		private String name;

		private Map<String, MultipleLabelEntity2> dynamicRelationships;
	}

	enum CyclicRelationship {
		ROUND,
		CORNERED
	}

	@Node
	private static class CyclicEntityWithEnumeratedDynamicRelationship1 {

		@Id private Long id;

		private Map<CyclicRelationship, CyclicEntityWithEnumeratedDynamicRelationship2> dynamicRelationship;
	}

	@Node
	private static class CyclicEntityWithEnumeratedDynamicRelationship2 {

		@Id private Long id;

		private Map<CyclicRelationship, CyclicEntityWithEnumeratedDynamicRelationship1> dynamicRelationship;
	}

	@Node
	private static class CyclicEntityWithStringDynamicRelationship1 {

		@Id private Long id;

		private Map<String, CyclicEntityWithStringDynamicRelationship2> dynamicRelationship;
	}

	@Node
	private static class CyclicEntityWithStringDynamicRelationship2 {

		@Id private Long id;

		private Map<String, CyclicEntityWithStringDynamicRelationship1> dynamicRelationship;
	}

}
