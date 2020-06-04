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
package org.neo4j.springframework.data.core.schema;

import static org.neo4j.opencypherdsl.Cypher.*;
import static org.neo4j.springframework.data.core.schema.Constants.*;
import static org.neo4j.springframework.data.core.schema.RelationshipDescription.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.Node;
import org.neo4j.opencypherdsl.Relationship;
import org.neo4j.opencypherdsl.*;
import org.neo4j.opencypherdsl.StatementBuilder.OngoingMatchAndUpdate;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentEntity;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A generator based on the schema defined by node and relationship descriptions.
 * Most methods return renderable Cypher statements.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @soundtrack Rammstein - Herzeleid
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public enum CypherGenerator {

	INSTANCE;

	private static final SymbolicName START_NODE_NAME = Cypher.name("startNode");
	private static final SymbolicName END_NODE_NAME = Cypher.name("endNode");

	private static final SymbolicName RELATIONSHIP_NAME = Cypher.name("relProps");

	private static final int RELATIONSHIP_DEPTH_LIMIT = 2;

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
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();

		List<Expression> expressions = new ArrayList<>();
		expressions.add(NAME_OF_ROOT_NODE);
		if (idDescription.isInternallyGeneratedId()) {
			expressions.add(Functions.id(rootNode).as(NAME_OF_INTERNAL_ID));
		}
		return match(rootNode).where(conditionOrNoCondition(condition))
			.with(expressions.toArray(new Expression[] {}));
	}

	/**
	 * Creates a statement that returns all labels of a node that are not part of a list parameter named {@link Constants#NAME_OF_STATIC_LABELS_PARAM}.
	 * Those are the "dynamic labels" of a node as set through SDN/RX.
	 *
	 * @param nodeDescription The node description for which the statement should be generated
	 * @return A statement having one parameter.
	 * @since 1.1
	 */
	public Statement createStatementReturningDynamicLabels(NodeDescription<?> nodeDescription) {

		final Node rootNode = anyNode(NAME_OF_ROOT_NODE);

		Condition versionCondition;
		if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

			PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription).getRequiredVersionProperty();
			versionCondition = rootNode.property(versionProperty.getName()).isEqualTo(parameter(NAME_OF_VERSION_PARAM));
		} else {
			versionCondition = Conditions.noCondition();
		}

		return match(rootNode)
			.where(nodeDescription.getIdDescription().asIdExpression().isEqualTo(parameter(NAME_OF_ID)))
			.and(versionCondition)
			.unwind(rootNode.labels()).as("label")
			.with(Cypher.name("label")).where(Cypher.name("label").in(parameter(NAME_OF_STATIC_LABELS_PARAM)).not())
			.returning(Functions.collect(Cypher.name("label")).as(NAME_OF_LABELS)).build();
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription) {
		return prepareDeleteOf(nodeDescription, null);
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription, @Nullable Condition condition) {

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
			.named(NAME_OF_ROOT_NODE);
		return match(rootNode).where(conditionOrNoCondition(condition)).detachDelete(rootNode).build();
	}

	public Statement prepareSaveOf(NodeDescription<?> nodeDescription, UnaryOperator<OngoingMatchAndUpdate> updateDecorator) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();
		Parameter idParameter = parameter(NAME_OF_ID);

		if (!idDescription.isInternallyGeneratedId()) {
			String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
				.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

			if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

				PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription)
					.getRequiredVersionProperty();
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
					.where(possibleExistingNode.property(nameOfIdProperty).isEqualTo(idParameter))
					.with(possibleExistingNode).where(possibleExistingNode.isNull())
					.create(rootNode)
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId())
					.build();

				Statement updateIfExists = updateDecorator.apply(match(rootNode)
					.where(rootNode.property(nameOfIdProperty).isEqualTo(idParameter))
					.and(rootNode.property(versionProperty.getName()).isEqualTo(parameter(NAME_OF_VERSION_PARAM)))
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId())
					.build();
				return Cypher.union(createIfNew, updateIfExists);

			} else {
				return updateDecorator.apply(
					Cypher.merge(rootNode.withProperties(nameOfIdProperty, idParameter))
						.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM))
				).returning(rootNode.internalId()).build();
			}
		} else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

			Statement createIfNew;
			Statement updateIfExists;

			if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

				PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription)
					.getRequiredVersionProperty();

				createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
					.where(possibleExistingNode.internalId().isEqualTo(idParameter))
					.with(possibleExistingNode).where(possibleExistingNode.isNull())
					.create(rootNode)
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId()).build();

				updateIfExists = updateDecorator.apply(match(rootNode)
					.where(rootNode.internalId().isEqualTo(idParameter))
					.and(rootNode.property(versionProperty.getName()).isEqualTo(parameter(NAME_OF_VERSION_PARAM)))
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId())
					.build();
			} else {
				createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
					.where(possibleExistingNode.internalId().isEqualTo(idParameter))
					.with(possibleExistingNode).where(possibleExistingNode.isNull())
					.create(rootNode)
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId())
					.build();

				updateIfExists = updateDecorator.apply(match(rootNode)
					.where(rootNode.internalId().isEqualTo(idParameter))
					.set(rootNode, parameter(NAME_OF_PROPERTIES_PARAM)))
					.returning(rootNode.internalId()).build();
			}

			return Cypher.union(createIfNew, updateIfExists);
		}
	}

	public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

		Assert.isTrue(!nodeDescription.isUsingInternalIds(),
			"Only entities that use external IDs can be saved in a batch.");

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
			.named(NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();

		String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
			.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

		String row = "entity";
		return Cypher
			.unwind(parameter(NAME_OF_ENTITY_LIST_PARAM)).as(row)
			.merge(rootNode.withProperties(nameOfIdProperty, property(row, NAME_OF_ID)))
			.set(rootNode, property(row, NAME_OF_PROPERTIES_PARAM))
			.returning(Functions.collect(rootNode.property(nameOfIdProperty)).as(NAME_OF_IDS))
			.build();
	}

	@NonNull
	public Statement createRelationshipCreationQuery(Neo4jPersistentEntity<?> neo4jPersistentEntity,
		RelationshipDescription relationship, @Nullable String dynamicRelationshipType, Long relatedInternalId) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds()
			? anyNode(START_NODE_NAME)
			: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
				.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);
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

	@NonNull
	public Statement createRelationshipWithPropertiesCreationQuery(Neo4jPersistentEntity<?> neo4jPersistentEntity,
		RelationshipDescription relationship, Long relatedInternalId) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
			"Properties required to create a relationship with properties");
		Assert.isTrue(!relationship.isDynamic(),
			"Creation of relationships with properties is only supported for non-dynamic relationships");

		Node startNode = anyNode(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(FROM_ID_PARAMETER_NAME);
		Parameter relationshipProperties = parameter(NAME_OF_PROPERTIES_PARAM);
		String type = relationship.getType();

		Relationship relOutgoing = startNode.relationshipTo(endNode, type).named(RELATIONSHIP_NAME);
		Relationship relIncoming = startNode.relationshipFrom(endNode, type).named(RELATIONSHIP_NAME);

		return match(startNode)
			.where(neo4jPersistentEntity.isUsingInternalIds()
				? startNode.internalId().isEqualTo(idParameter)
				: startNode.property(idPropertyName).isEqualTo(idParameter))
			.match(endNode)
			.where(endNode.internalId().isEqualTo(literalOf(relatedInternalId)))
			.merge(relationship.isOutgoing()
				? relOutgoing
				: relIncoming
			)
			.set(RELATIONSHIP_NAME, relationshipProperties)
			.build();
	}

	@NonNull
	public Statement createRelationshipRemoveQuery(Neo4jPersistentEntity<?> neo4jPersistentEntity,
		RelationshipDescription relationshipDescription, Neo4jPersistentEntity relatedNode) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds()
			? anyNode(START_NODE_NAME)
			: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
				.named(START_NODE_NAME);

		final Node endNode = node(relatedNode.getPrimaryLabel(), relatedNode.getAdditionalLabels());
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

		Predicate<String> includeField = s -> inputProperties == null || inputProperties.isEmpty()
			|| inputProperties.contains(s);

		List<RelationshipDescription> processedRelationships = new ArrayList<>();

		return projectPropertiesAndRelationships(nodeDescription, NAME_OF_ROOT_NODE, includeField,
			processedRelationships);
	}

	private MapProjection projectAllPropertiesAndRelationships(NodeDescription<?> nodeDescription,
		SymbolicName nodeName,
		List<RelationshipDescription> processedRelationships) {

		Predicate<String> includeAllFields = (field) -> true;
		return projectPropertiesAndRelationships(nodeDescription, nodeName, includeAllFields, processedRelationships);
	}

	private MapProjection projectPropertiesAndRelationships(NodeDescription<?> nodeDescription,
		SymbolicName nodeName,
		Predicate<String> includeProperty,
		List<RelationshipDescription> processedRelationships) {

		List<Object> contentOfProjection = new ArrayList<>();
		contentOfProjection.addAll(projectNodeProperties(nodeDescription, nodeName, includeProperty));
		contentOfProjection.addAll(
			generateListsFor(nodeDescription.getRelationships(), nodeName, includeProperty, processedRelationships)
		);

		return Cypher.anyNode(nodeName).project(contentOfProjection);
	}

	/**
	 * Creates a list of objects that represents a very basic of {@code MapEntry<String, Object>} with the exception that
	 * this list can also contain two "keys" in a row. The {@link MapProjection} will take care to handle them as
	 * self-reflecting fields. Example with self-reflection and explicit value: {@code n {.id, name: n.name}}.
	 */
	private List<Object> projectNodeProperties(NodeDescription<?> nodeDescription, SymbolicName nodeName,
		Predicate<String> includeField) {

		List<Object> nodePropertiesProjection = new ArrayList<>();
		Node node = anyNode(nodeName);
		for (GraphPropertyDescription property : nodeDescription.getGraphPropertiesInHierarchy()) {
			if (!includeField.test(property.getFieldName())) {
				continue;
			}

			if (property.isInternalIdProperty()) {
				nodePropertiesProjection.add(NAME_OF_INTERNAL_ID);
				nodePropertiesProjection.add(Functions.id(node));
			} else if (!((Neo4jPersistentProperty) property).isDynamicLabels()) {
				nodePropertiesProjection.add(property.getPropertyName());
			}
		}

		nodePropertiesProjection.add(NAME_OF_LABELS);
		nodePropertiesProjection.add(Functions.labels(node));

		return nodePropertiesProjection;
	}

	/**
	 * @see org.neo4j.springframework.data.core.schema.CypherGenerator#projectNodeProperties
	 */
	private List<Object> generateListsFor(Collection<RelationshipDescription> relationships,
		SymbolicName nodeName, Predicate<String> includeField,
		List<RelationshipDescription> processedRelationships) {

		List<Object> mapProjectionLists = new ArrayList<>();

		for (RelationshipDescription relationshipDescription : relationships) {

			String fieldName = relationshipDescription.getFieldName();
			if (!includeField.test(fieldName)) {
				continue;
			}

			// if we already processed the other way before, do not try to jump in the infinite loop
			// unless it is a root node relationship
			if (!nodeName.equals(NAME_OF_ROOT_NODE) && relationshipDescription.hasRelationshipObverse()
				&& processedRelationships.contains(relationshipDescription.getRelationshipObverse())) {
				continue;
			}

			if (Collections.frequency(processedRelationships, relationshipDescription) > RELATIONSHIP_DEPTH_LIMIT) {
				return mapProjectionLists;
			}

			generateListFor(relationshipDescription, nodeName, processedRelationships, fieldName, mapProjectionLists);
		}

		return mapProjectionLists;
	}

	private void generateListFor(RelationshipDescription relationshipDescription, SymbolicName nodeName,
		List<RelationshipDescription> processedRelationships, String fieldName, List<Object> mapProjectionLists) {

		String relationshipType = relationshipDescription.getType();
		String relationshipTargetName = relationshipDescription.generateRelatedNodesCollectionName();
		String targetPrimaryLabel = relationshipDescription.getTarget().getPrimaryLabel();
		List<String> targetAdditionalLabels = relationshipDescription.getTarget().getAdditionalLabels();

		Node startNode = anyNode(nodeName);
		SymbolicName relationshipFieldName = nodeName.concat("_" + fieldName);
		Node endNode = node(targetPrimaryLabel, targetAdditionalLabels).named(relationshipFieldName);
		NodeDescription<?> endNodeDescription = relationshipDescription.getTarget();

		processedRelationships.add(relationshipDescription);

		if (relationshipDescription.isDynamic()) {
			Relationship relationship = relationshipDescription
				.isOutgoing()
				? startNode.relationshipTo(endNode)
				: startNode.relationshipFrom(endNode);
			relationship = relationship.named(relationshipTargetName);

			addMapProjection(relationshipTargetName,
				listBasedOn(relationship)
					.returning(
						projectAllPropertiesAndRelationships(endNodeDescription,
							relationshipFieldName, new ArrayList<>(processedRelationships))
							.and(NAME_OF_RELATIONSHIP_TYPE, Functions.type(relationship))),
				mapProjectionLists);

		} else {
			Relationship relationship = relationshipDescription.isOutgoing()
				? startNode.relationshipTo(endNode, relationshipType)
				: startNode.relationshipFrom(endNode, relationshipType);

			MapProjection mapProjection = projectAllPropertiesAndRelationships(endNodeDescription,
				relationshipFieldName, new ArrayList<>(processedRelationships));

			if (relationshipDescription.hasRelationshipProperties()) {
				relationship = relationship.named(RelationshipDescription.NAME_OF_RELATIONSHIP);
				mapProjection = mapProjection.and(relationship);
			}

			addMapProjection(relationshipTargetName,
				listBasedOn(relationship).returning(mapProjection),
				mapProjectionLists);
		}
	}

	private void addMapProjection(String name, Object projection, List<Object> projectionList) {
		projectionList.add(name);
		projectionList.add(projection);
	}

	private static Condition conditionOrNoCondition(@Nullable Condition condition) {
		return condition == null ? Conditions.noCondition() : condition;
	}
}
