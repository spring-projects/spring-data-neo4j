/*
 * Copyright 2011-2020 the original author or authors.
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.domain.AbstractPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Davide Fantuzzi
 * @author Andrea Santurbano
 */
class CypherGeneratorTest {

	@Test
	void itShouldCreateRelationshipCreationQueryWithLabelIfPresent() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(Entity1.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		when(relationshipDescription.isDynamic()).thenReturn(true);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL", 1L);

		String expectedQuery = "MATCH (startNode:`Entity1`) WHERE startNode.id = $fromId MATCH (endNode)"
				+ " WHERE id(endNode) = 1 MERGE (startNode)<-[:`REL`]-(endNode)";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void itShouldCreateRelationshipCreationQueryWithMultipleLabels() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity1.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		when(relationshipDescription.isDynamic()).thenReturn(true);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL", 1L);

		String expectedQuery =
				"MATCH (startNode:`Entity1`:`MultipleLabel`) WHERE startNode.id = $fromId MATCH (endNode)"
				+ " WHERE id(endNode) = 1 MERGE (startNode)<-[:`REL`]-(endNode)";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void itShouldCreateRelationshipCreationQueryWithoutUsingInternalIds() {
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		Neo4jPersistentEntity<?> persistentEntity = Mockito.mock(Neo4jPersistentEntity.class);
		Neo4jPersistentProperty persistentProperty = Mockito.mock(Neo4jPersistentProperty.class);

		when(relationshipDescription.isDynamic()).thenReturn(true);
		when(persistentEntity.isUsingInternalIds()).thenReturn(true);
		when(persistentEntity.getRequiredIdProperty()).thenReturn(persistentProperty);

		Statement statement = CypherGenerator.INSTANCE.prepareSaveOfRelationship(persistentEntity,
				relationshipDescription, "REL", 1L);

		String expectedQuery = "MATCH (startNode) WHERE id(startNode) = $fromId MATCH (endNode)"
				+ " WHERE id(endNode) = 1 MERGE (startNode)<-[:`REL`]-(endNode)";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void itShouldCreateRelationshipRemoveQueryWithLabelIfPresent() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext().getPersistentEntity(Entity1.class);
		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext().getPersistentEntity(Entity2.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode:`Entity1`)<-[rel]-(:`Entity2`) WHERE startNode.id = $fromId DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void itShouldCreateRelationshipRemoveQueryWithMultipleLabels() {
		Neo4jPersistentEntity<?> persistentEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity1.class);
		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext()
				.getPersistentEntity(MultipleLabelEntity2.class);
		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode:`Entity1`:`MultipleLabel`)<-[rel]-(:`Entity2`:`MultipleLabel`) WHERE startNode.id = $fromId DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	@Test
	void itShouldCreateRelationshipRemoveQueryWithoutUsingInternalIds() {

		Neo4jPersistentEntity<?> relatedEntity = new Neo4jMappingContext().getPersistentEntity(Entity2.class);

		RelationshipDescription relationshipDescription = Mockito.mock(RelationshipDescription.class);
		Neo4jPersistentEntity<?> persistentEntity = Mockito.mock(Neo4jPersistentEntity.class);
		Neo4jPersistentProperty persistentProperty = Mockito.mock(Neo4jPersistentProperty.class);
		doReturn(relatedEntity).when(relationshipDescription).getTarget();

		when(relationshipDescription.isDynamic()).thenReturn(true);
		when(persistentEntity.isUsingInternalIds()).thenReturn(true);
		when(persistentEntity.getRequiredIdProperty()).thenReturn(persistentProperty);

		Statement statement = CypherGenerator.INSTANCE.prepareDeleteOf(persistentEntity, relationshipDescription);

		String expectedQuery = "MATCH (startNode)<-[rel]-(:`Entity2`) WHERE id(startNode) = $fromId DELETE rel";
		Assert.assertEquals(expectedQuery, Renderer.getDefaultRenderer().render(statement));
	}

	private static Stream<Arguments> pageables() {
		return Stream.of(
				Arguments.of(PageRequest.of(1, 2, Sort.by("a", "b").and(
						Sort.by(Sort.Order.asc("foo"), Sort.Order.desc("bar")))),
						Optional.of("ORDER BY a ASC, b ASC, foo ASC, bar DESC")),
				Arguments.of(null, Optional.empty()),
				Arguments.of(PageRequest.of(1, 2, Sort.unsorted()), Optional.empty()),
				Arguments.of(new AbstractPageRequest(1, 2) {
					@Override public Pageable next() {
						return null;
					}

					@Override public Pageable previous() {
						return null;
					}

					@Override public Pageable first() {
						return null;
					}

					@Override public Sort getSort() {
						return null;
					}
				}, Optional.empty())
		);
	}

	@ParameterizedTest
	@MethodSource("pageables")
	void shouldRenderOrderByFragment(Pageable pageable, Optional<String> expectValue) {

		Optional<String> fragment = CypherGenerator.INSTANCE.createOrderByFragment(pageable);
		assertThat(fragment).isEqualTo(expectValue);
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

}
