/*
 * Copyright (c) 2019 "Neo4j,"
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
package org.neo4j.springframework.data.core.schema;

import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.schema.NodeDescription.*;
import static org.neo4j.springframework.data.core.schema.RelationshipDescription.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.jetbrains.annotations.NotNull;
import org.neo4j.springframework.data.core.cypher.Node;
import org.neo4j.springframework.data.core.cypher.Relationship;
import org.neo4j.springframework.data.core.cypher.*;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.mapping.MappingException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A generator based on the schema defined by node and relationship descriptions.
 * Most methods return renderable Cypher statements.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @soundtrack Rammstein - Herzeleid
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public enum CypherGenerator {

	INSTANCE;

	public static final String FROM_ID_PARAMETER_NAME = "fromId";

	private static final String START_NODE_NAME = "startNode";
	private static final String END_NODE_NAME = "endNode";

	/**
	 * @param nodeDescription The node description for which a match clause should be generated
	 * @return An ongoing match
	 * @see #prepareMatchOf(NodeDescription, Condition)
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription) {
		return prepareMatchOf(nodeDescription, null);
	}

	/**
	 * This will create a match statement that fits the given node description and may contains additional conditions.
	 * The {@code WITH} clause of this statement contains all nodes and relationships necessary to map a record to
	 * the given {@code nodeDescription}.
	 * <p>
	 * It is recommended to use {@link Cypher#asterisk()} to return everything from the query in the end.
	 * <p>
	 * The root node is guaranteed to have the symbolic name {@code n}.
	 *
	 * @param nodeDescription The node description for which a match clause should be generated
	 * @param condition       Optional conditions to add
	 * @return An ongoing match
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription, @Nullable
		Condition condition) {

		String primaryLabel = nodeDescription.getPrimaryLabel();

		Node rootNode = node(primaryLabel).named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();

		List<Expression> expressions = new ArrayList<>();
		expressions.add(rootNode);
		if (idDescription.isInternallyGeneratedId()) {
			expressions.add(Functions.id(rootNode).as(NAME_OF_INTERNAL_ID));
		}
		return Cypher.match(rootNode).where(conditionOrNoCondition(condition))
			.with(expressions.toArray(new Expression[] {}));
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription) {
		return prepareDeleteOf(nodeDescription, null);
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription, @Nullable Condition condition) {

		Node rootNode = node(nodeDescription.getPrimaryLabel())
			.named(NAME_OF_ROOT_NODE);
		return Cypher.match(rootNode).where(conditionOrNoCondition(condition)).detachDelete(rootNode).build();
	}

	public Statement prepareSaveOf(NodeDescription<?> nodeDescription) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		Node rootNode = node(primaryLabel).named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();
		Parameter idParameter = parameter(NAME_OF_ID_PARAM);

		if (!idDescription.isInternallyGeneratedId()) {
			String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
				.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

			return Cypher.merge(rootNode.properties(nameOfIdProperty, idParameter))
				.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
				.returning(rootNode.internalId())
				.build();
		} else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel).named(nameOfPossibleExistingNode);

			Statement createIfNew = optionalMatch(possibleExistingNode)
				.where(possibleExistingNode.internalId().isEqualTo(idParameter))
				.with(possibleExistingNode).where(possibleExistingNode.isNull())
				.create(rootNode)
				.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
				.returning(rootNode.internalId())
				.build();

			Statement updateIfExists = Cypher
				.match(rootNode)
				.where(rootNode.internalId().isEqualTo(idParameter))
				.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
				.returning(rootNode.internalId())
				.build();

			return Cypher.union(createIfNew, updateIfExists);
		}
	}

	public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

		Assert.isTrue(!nodeDescription.isUsingInternalIds(),
			"Only entities that use external IDs can be saved in a batch.");

		Node rootNode = node(nodeDescription.getPrimaryLabel())
			.named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();

		String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
			.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

		String row = "entity";
		return Cypher
			.unwind(parameter(NAME_OF_ENTITY_LIST_PARAM)).as(row)
			.merge(rootNode.properties(nameOfIdProperty, property(row, NAME_OF_ID_PARAM)))
			.set(rootNode, property(row, NAME_OF_PROPERTIES_PARAM))
			.returning(Functions.collect(rootNode.property(nameOfIdProperty)).as(NAME_OF_IDS_RESULT))
			.build();
	}

	@NotNull
	public Statement createRelationshipCreationQuery(Neo4jPersistentEntity<?> neo4jPersistentEntity,
		RelationshipDescription relationship, @Nullable String dynamicRelationshipType, Long relatedInternalId) {

		Node startNode = anyNode(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(FROM_ID_PARAMETER_NAME);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		return match(startNode)
			.where(neo4jPersistentEntity.isUsingInternalIds()
				? startNode.internalId().isEqualTo(idParameter)
				: startNode.property(idPropertyName).isEqualTo(idParameter))
			.match(endNode)
			.where(endNode.internalId().isEqualTo(literalOf(relatedInternalId)))
			.merge(relationship.isOutgoing()
				? startNode.relationshipTo(endNode, type)
				: startNode.relationshipFrom(endNode, type)
			)
			.build();
	}

	@NotNull
	public Statement createRelationshipRemoveQuery(Neo4jPersistentEntity<?> neo4jPersistentEntity,
		RelationshipDescription relationshipDescription, String relatedNodeLabel) {

		Node startNode = anyNode(START_NODE_NAME);
		Node endNode = node(relatedNodeLabel);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();
		boolean outgoing = relationshipDescription.isOutgoing();

		String relationshipType = relationshipDescription.isDynamic() ? null : relationshipDescription.getType();
		String relationshipToRemoveName = "rel";

		Relationship relationship = outgoing
			? startNode.relationshipTo(endNode, relationshipType).named(relationshipToRemoveName)
			: startNode.relationshipFrom(endNode, relationshipType).named(relationshipToRemoveName);

		Parameter idParameter = parameter(FROM_ID_PARAMETER_NAME);
		return match(relationship)
			.where(neo4jPersistentEntity.isUsingInternalIds()
				? startNode.internalId().isEqualTo(idParameter)
				: startNode.property(idPropertyName).isEqualTo(idParameter))
			.delete(relationship.getSymbolicName().get()).build();
	}

	public Expression createReturnStatementForMatch(NodeDescription<?> nodeDescription) {
		return createReturnStatementForMatch(nodeDescription, null);
	}

	/**
	 * @param nodeDescription Description of the root node
	 * @param inputProperties A list of Java properties of the domain to be included.
	 *                        Those properties are compared with the field names of graph properties respectively relationships.
	 * @return An expresion to be returned by a Cypher statement
	 */
	public Expression createReturnStatementForMatch(NodeDescription<?> nodeDescription,
		@Nullable List<String> inputProperties) {

		Predicate<String> includeField = s -> inputProperties == null || inputProperties.isEmpty() || inputProperties
			.contains(s);
		return projectPropertiesAndRelationships(nodeDescription, NAME_OF_ROOT_NODE, includeField);
	}

	private MapProjection projectAllPropertiesAndRelationships(NodeDescription<?> nodeDescription, String nodeName) {
		Predicate<String> includeAllFields = (field) -> true;
		return projectPropertiesAndRelationships(nodeDescription, nodeName, includeAllFields);
	}

	private MapProjection projectPropertiesAndRelationships(NodeDescription<?> nodeDescription,
		String nodeName,
		Predicate<String> includeProperty) {

		Collection<RelationshipDescription> relationships = nodeDescription.getRelationships();

		List<Object> contentOfProjection = new ArrayList<>();
		contentOfProjection.addAll(projectNodeProperties(nodeDescription, nodeName, includeProperty));
		contentOfProjection.addAll(generateListsOf(relationships, nodeName, includeProperty));

		return Cypher.anyNode(nodeName).project(contentOfProjection);
	}

	private List<Object> projectNodeProperties(NodeDescription<?> nodeDescription, String nodeName,
		Predicate<String> includeField) {

		List<Object> nodePropertiesProjection = new ArrayList<>();
		for (GraphPropertyDescription property : nodeDescription.getGraphProperties()) {
			if (!includeField.test(property.getFieldName())) {
				continue;
			}

			if (property.isInternalIdProperty()) {
				nodePropertiesProjection.add(NAME_OF_INTERNAL_ID);
				nodePropertiesProjection.add(Functions.id(Cypher.name(nodeName)));
			} else {
				nodePropertiesProjection.add(property.getPropertyName());
			}
		}

		return nodePropertiesProjection;
	}

	private List<Object> generateListsOf(Collection<RelationshipDescription> relationships,
		String nameOfStartNode, Predicate<String> includeField) {

		List<Object> generatedLists = new ArrayList<>();
		for (RelationshipDescription relationshipDescription : relationships) {

			String sourceLabel = relationshipDescription.getSource().getPrimaryLabel();
			String targetLabel = relationshipDescription.getTarget().getPrimaryLabel();

			String fieldName = relationshipDescription.getFieldName();
			if (!includeField.test(fieldName)) {
				continue;
			}

			// do not follow self-references more than once
			if (targetLabel.equals(sourceLabel) && nameOfStartNode.equals(fieldName)) {
				continue;
			}

			String relationshipType = relationshipDescription.getType();
			String relationshipTargetName = relationshipDescription.generateRelatedNodesCollectionName();

			Node startNode = anyNode(nameOfStartNode);
			Node endNode = node(targetLabel).named(fieldName);
			NodeDescription<?> endNodeDescription = relationshipDescription.getTarget();

			if (relationshipDescription.isDynamic()) {
				Relationship relationship = relationshipDescription
					.isOutgoing()
					? startNode.relationshipTo(endNode)
					: startNode.relationshipFrom(endNode);
				relationship = relationship.named(relationshipTargetName);

				generatedLists.add(relationshipTargetName);
				generatedLists.add(listBasedOn(relationship)
					.returning(
						projectAllPropertiesAndRelationships(endNodeDescription, fieldName)
							.and(NAME_OF_RELATIONSHIP_TYPE, Functions.type(relationship))));
			} else {
				Relationship relationship = relationshipDescription.isOutgoing()
					? startNode.relationshipTo(endNode, relationshipType)
					: startNode.relationshipFrom(endNode, relationshipType);

				generatedLists.add(relationshipTargetName);
				generatedLists.add(listBasedOn(relationship)
					.returning(projectAllPropertiesAndRelationships(endNodeDescription, fieldName)));
			}
		}

		return generatedLists;
	}

	private static Condition conditionOrNoCondition(@Nullable Condition condition) {
		return condition == null ? Conditions.noCondition() : condition;
	}
}

