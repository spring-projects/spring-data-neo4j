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

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.MapProjection;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingMatchAndUpdate;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.listBasedOn;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.optionalMatch;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * A generator based on the schema defined by node and relationship descriptions. Most methods return renderable Cypher
 * statements.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @soundtrack Rammstein - Herzeleid
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public enum CypherGenerator {

	INSTANCE;

	private static final SymbolicName START_NODE_NAME = Cypher.name("startNode");
	private static final SymbolicName END_NODE_NAME = Cypher.name("endNode");

	private static final SymbolicName RELATIONSHIP_NAME = Cypher.name("relProps");

	/**
	 * @param nodeDescription The node description for which a match clause should be generated
	 * @return An ongoing match
	 * @see #prepareMatchOf(NodeDescription, Condition)
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription) {
		return prepareMatchOf(nodeDescription, null);
	}

	/**
	 * This will create a match statement that fits the given node description and may contains additional conditions. The
	 * {@code WITH} clause of this statement contains all nodes and relationships necessary to map a record to the given
	 * {@code nodeDescription}.
	 * <p>
	 * It is recommended to use {@link Cypher#asterisk()} to return everything from the query in the end.
	 * <p>
	 * The root node is guaranteed to have the symbolic name {@code n}.
	 *
	 * @param nodeDescription The node description for which a match clause should be generated
	 * @param condition Optional conditions to add
	 * @return An ongoing match
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription,
																		  @Nullable Condition condition) {

		Node rootNode = createRootNode(nodeDescription);

		List<Expression> expressions = new ArrayList<>();
		expressions.add(Constants.NAME_OF_ROOT_NODE);
		expressions.add(Functions.id(rootNode).as(Constants.NAME_OF_INTERNAL_ID));

		return match(rootNode).where(conditionOrNoCondition(condition)).with(expressions.toArray(new Expression[] {}));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
														  @Nullable List<PatternElement> initialMatchOn,
														  @Nullable Condition condition) {
		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);

		StatementBuilder.OngoingReadingWithoutWhere match = null;
		if (initialMatchOn == null || initialMatchOn.isEmpty()) {
			match = Cypher.match(rootNode);
		} else {
			for (PatternElement patternElement : initialMatchOn) {
				if (match == null) {
					match = Cypher.match(patternElement);
				} else {
					match.match(patternElement);
				}
			}
		}
		List<Expression> expressions = new ArrayList<>();
		expressions.add(Functions.collect(Functions.id(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));

		return match
				.where(conditionOrNoCondition(condition))
				.with(expressions.toArray(new Expression[]{}));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
			  RelationshipDescription relationshipDescription, @Nullable List<PatternElement> initialMatchOn,
														  @Nullable Condition condition) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);

		StatementBuilder.OngoingReadingWithoutWhere match = null;
		if (initialMatchOn == null || initialMatchOn.isEmpty()) {
			match = Cypher.match(rootNode);
		} else {
			for (PatternElement patternElement : initialMatchOn) {
				if (match == null) {
					match = Cypher.match(patternElement);
				} else {
					match.match(patternElement);
				}
			}
		}

		Node targetNode = node(relationshipDescription.getTarget().getPrimaryLabel(),
				relationshipDescription.getTarget().getAdditionalLabels())
				.named(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);

		boolean dynamicRelationship = relationshipDescription.isDynamic();
		Class<?> componentType = ((DefaultRelationshipDescription) relationshipDescription).getInverse().getComponentType();
		List<String> relationshipTypes = new ArrayList<>();
		if (dynamicRelationship && componentType != null && componentType.isEnum()) {
			Arrays.stream(componentType.getEnumConstants())
					.forEach(constantName -> relationshipTypes.add(constantName.toString()));
		} else if (!dynamicRelationship) {
			relationshipTypes.add(relationshipDescription.getType());
		}
		String[] types = relationshipTypes.toArray(new String[]{});

		Relationship relationship = null;
		switch (relationshipDescription.getDirection()) {
			case OUTGOING:
				relationship = rootNode.relationshipTo(targetNode, types);
				break;
			case INCOMING:
				relationship = rootNode.relationshipFrom(targetNode, types);
				break;
			default:
				relationship = rootNode.relationshipBetween(targetNode, types);
				break;
		}

		relationship = relationship.named(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
		List<Expression> expressions = new ArrayList<>();
		expressions.add(Functions.collect(Functions.id(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		expressions.add(Functions.collect(Functions.id(targetNode)).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		expressions.add(Functions.collect(Functions.id(relationship)).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS));

		return match
				.where(conditionOrNoCondition(condition))
				.optionalMatch(relationship)
				.with(expressions.toArray(new Expression[]{}));
	}

	@NonNull
	public Node createRootNode(NodeDescription<?> nodeDescription) {
		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		return node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);
	}

	/**
	 * Creates a statement that returns all labels of a node that are not part of a list parameter named
	 * {@link Constants#NAME_OF_STATIC_LABELS_PARAM}. Those are the "dynamic labels" of a node as set through SDN.
	 *
	 * @param nodeDescription The node description for which the statement should be generated
	 * @return A statement having one parameter.
	 * @since 6.0
	 */
	public Statement createStatementReturningDynamicLabels(NodeDescription<?> nodeDescription) {

		final Node rootNode = Cypher.anyNode(Constants.NAME_OF_ROOT_NODE);

		Condition versionCondition;
		if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

			PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription).getRequiredVersionProperty();
			versionCondition = rootNode.property(versionProperty.getName())
					.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM));
		} else {
			versionCondition = Conditions.noCondition();
		}

		return match(rootNode)
				.where(nodeDescription.getIdDescription().asIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
				.and(versionCondition).unwind(rootNode.labels()).as("label").with(Cypher.name("label"))
				.where(Cypher.name("label").in(parameter(Constants.NAME_OF_STATIC_LABELS_PARAM)).not())
				.returning(Functions.collect(Cypher.name("label")).as(Constants.NAME_OF_LABELS)).build();
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription) {
		return prepareDeleteOf(nodeDescription, null);
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription, @Nullable Condition condition) {

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
				.named(Constants.NAME_OF_ROOT_NODE);
		return match(rootNode).where(conditionOrNoCondition(condition)).detachDelete(rootNode).build();
	}

	public Statement prepareSaveOf(NodeDescription<?> nodeDescription,
			UnaryOperator<OngoingMatchAndUpdate> updateDecorator) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();
		Parameter idParameter = parameter(Constants.NAME_OF_ID);

		if (!idDescription.isInternallyGeneratedId()) {
			String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
					.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

			if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

				PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription).getRequiredVersionProperty();
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(possibleExistingNode.property(nameOfIdProperty).isEqualTo(idParameter)).with(possibleExistingNode)
						.where(possibleExistingNode.isNull()).create(rootNode)
						.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode.internalId()).build();

				// needs a dedicated variable for the union function in the CypherDSL.
				Node nodeToUpdate = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);
				Statement updateIfExists = updateDecorator
						.apply(match(nodeToUpdate).where(nodeToUpdate.property(nameOfIdProperty).isEqualTo(idParameter))
								.and(nodeToUpdate.property(versionProperty.getName()).isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM)))
								.mutate(nodeToUpdate, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(nodeToUpdate.internalId()).build();
				return Cypher.union(createIfNew, updateIfExists);

			} else {
				return updateDecorator.apply(Cypher.merge(rootNode.withProperties(nameOfIdProperty, idParameter)).mutate(rootNode,
						parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode.internalId()).build();
			}
		} else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

			Statement createIfNew;
			Statement updateIfExists;

			// needs a dedicated variable for the union function in the CypherDSL.
			Node nodeToUpdate = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);

			if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

				PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription).getRequiredVersionProperty();

				createIfNew = updateDecorator
						.apply(optionalMatch(possibleExistingNode).where(possibleExistingNode.internalId().isEqualTo(idParameter))
								.with(possibleExistingNode).where(possibleExistingNode.isNull()).create(rootNode)
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode.internalId()).build();

				updateIfExists = updateDecorator.apply(match(nodeToUpdate).where(nodeToUpdate.internalId().isEqualTo(idParameter))
						.and(nodeToUpdate.property(versionProperty.getName()).isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM)))
						.mutate(nodeToUpdate, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(nodeToUpdate.internalId()).build();
			} else {
				createIfNew = updateDecorator
						.apply(optionalMatch(possibleExistingNode).where(possibleExistingNode.internalId().isEqualTo(idParameter))
								.with(possibleExistingNode).where(possibleExistingNode.isNull()).create(rootNode)
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode.internalId()).build();

				updateIfExists = updateDecorator.apply(match(nodeToUpdate).where(nodeToUpdate.internalId().isEqualTo(idParameter))
						.mutate(nodeToUpdate, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(nodeToUpdate.internalId()).build();
			}

			return Cypher.union(createIfNew, updateIfExists);
		}
	}

	public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

		Assert.isTrue(!nodeDescription.isUsingInternalIds(),
				"Only entities that use external IDs can be saved in a batch.");

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
				.named(Constants.NAME_OF_ROOT_NODE);
		IdDescription idDescription = nodeDescription.getIdDescription();

		String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
				.orElseThrow(() -> new MappingException("External id does not correspond to a graph property!"));

		String row = "entity";
		return Cypher.unwind(parameter(Constants.NAME_OF_ENTITY_LIST_PARAM)).as(row)
				.merge(rootNode.withProperties(nameOfIdProperty, Cypher.property(row, Constants.NAME_OF_ID)))
				.mutate(rootNode, Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM))
				.returning(Functions.collect(rootNode.property(nameOfIdProperty)).as(Constants.NAME_OF_IDS)).build();
	}

	@NonNull
	public Statement prepareSaveOfRelationship(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds()
				? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
						.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		Relationship relationshipFragment = (relationship.isOutgoing() ?
				startNode.relationshipTo(endNode, type) :
				startNode.relationshipFrom(endNode, type)).named(RELATIONSHIP_NAME);

		return match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? startNode.internalId().isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.match(endNode).where(endNode.internalId().isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)))
				.merge(relationshipFragment)
				.returning(Functions.id(relationshipFragment))
				.build();
	}

	@NonNull
	public Statement prepareSaveOfRelationshipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels()).named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		Parameter relationshipProperties = parameter(Constants.NAME_OF_PROPERTIES_PARAM);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();

		Relationship relationshipFragment = (
				relationship.isOutgoing() ?
						startNode.relationshipTo(endNode, type) :
						startNode.relationshipFrom(endNode, type))
				.named(RELATIONSHIP_NAME);

		return match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? startNode.internalId().isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.match(endNode).where(endNode.internalId().isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)))
				.merge(relationshipFragment)
				.mutate(RELATIONSHIP_NAME, relationshipProperties)
				.returning(Functions.id(relationshipFragment))
				.build();
	}

	@NonNull
	public Statement prepareDeleteOf(
			Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationshipDescription
	) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds() ? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
						.named(START_NODE_NAME);

		NodeDescription<?> target = relationshipDescription.getTarget();
		Node endNode = node(target.getPrimaryLabel(), target.getAdditionalLabels());

		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();
		boolean outgoing = relationshipDescription.isOutgoing();

		String relationshipType = relationshipDescription.isDynamic() ? null : relationshipDescription.getType();

		String relationshipToRemoveName = "rel";
		Relationship relationship = outgoing
				? startNode.relationshipTo(endNode, relationshipType).named(relationshipToRemoveName)
				: startNode.relationshipFrom(endNode, relationshipType).named(relationshipToRemoveName);

		Parameter idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		return match(relationship)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? startNode.internalId().isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.and(Functions.id(relationship).in(Cypher.parameter(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM)).not())
				.delete(relationship.getSymbolicName().get())
				.build();
	}

	public Expression[] createReturnStatementForMatch(NodeDescription<?> nodeDescription) {
		return createReturnStatementForMatch(nodeDescription, fieldName -> true);
	}

	/**
	 * Creates an order by fragment, assuming the node to match is named `n`
	 *
	 * @param sort The {@link Sort sort} that should be turned into a valid Cypher {@code ORDER}-clause
	 * @return An optional order clause. Will be {@literal null} on sorts that are {@literal null} or a unsorted.
	 */
	public @Nullable String createOrderByFragment(@Nullable Sort sort) {

		if (sort == null || sort.isUnsorted()) {
			return null;
		}

		Statement statement = Cypher.match(Cypher.anyNode()).returning("n")
				.orderBy(sort.stream().filter(order -> order != null).map(order -> {
					String property = order.getProperty();
					Expression expression;
					if (property.contains(".")) {
						String[] path = property.split("\\.");
						if (path.length != 2) {
							throw new IllegalArgumentException(String.format(
									"Cannot handle order property `%s`, it must be a simple property or one-hop path.",
									property));
						}
						expression = Cypher.property(path[0], path[1]);
					} else {
						expression = Cypher.name(property);
					}
					return order.isAscending() ? expression.ascending() : expression.descending();
				}).toArray(SortItem[]::new))
				.build();
		String renderedStatement = Renderer.getDefaultRenderer().render(statement);
		return renderedStatement.substring(renderedStatement.indexOf("ORDER BY")).trim();
	}

	/**
	 * @param nodeDescription Description of the root node
	 * @param includeField A predicate derived from the set of included properties. This is only relevant in various forms
	 *                     of projections which allow to exclude one or more fields.
	 * @return An expresion to be returned by a Cypher statement
	 */
	public Expression[] createReturnStatementForMatch(NodeDescription<?> nodeDescription,
			Predicate<String> includeField) {

		List<RelationshipDescription> processedRelationships = new ArrayList<>();
		if (nodeDescription.containsPossibleCircles(includeField)) {
			return createGenericReturnStatement();
		} else {
			return new Expression[]{projectPropertiesAndRelationships(nodeDescription, Constants.NAME_OF_ROOT_NODE, includeField, processedRelationships)};
		}
	}

	public Expression[] createGenericReturnStatement() {
		List<Expression> returnExpressions = new ArrayList<>();
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS));
		return returnExpressions.toArray(new Expression[]{});
	}

	// recursive entry point for relationships in return statement
	private MapProjection projectAllPropertiesAndRelationships(NodeDescription<?> nodeDescription, SymbolicName nodeName,
			List<RelationshipDescription> processedRelationships) {

		Predicate<String> includeAllFields = (field) -> true;
		// Because we are getting called recursive, there cannot be any circle
		return projectPropertiesAndRelationships(nodeDescription, nodeName, includeAllFields, processedRelationships);
	}

	private MapProjection projectPropertiesAndRelationships(NodeDescription<?> nodeDescription, SymbolicName nodeName,
			Predicate<String> includedProperties, List<RelationshipDescription> processedRelationships) {

		List<Object> propertiesProjection = projectNodeProperties(nodeDescription, nodeName, includedProperties);
		List<Object> contentOfProjection = new ArrayList<>(propertiesProjection);

		Collection<RelationshipDescription> relationships = nodeDescription.getRelationshipsInHierarchy(includedProperties);
		relationships.removeIf(r -> !includedProperties.test(r.getFieldName()));

		contentOfProjection.addAll(generateListsFor(relationships, nodeName, processedRelationships));
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
		boolean hasCompositeProperties = false;
		for (GraphPropertyDescription graphProperty : nodeDescription.getGraphPropertiesInHierarchy()) {

			Neo4jPersistentProperty property = (Neo4jPersistentProperty) graphProperty;
			hasCompositeProperties = hasCompositeProperties || property.isComposite();

			if (!includeField.test(property.getFieldName()) || property.isDynamicLabels() || property.isComposite()) {
				continue;
			}

			nodePropertiesProjection.add(graphProperty.getPropertyName());
		}

		if (hasCompositeProperties) {
			nodePropertiesProjection.add(Constants.NAME_OF_ALL_PROPERTIES);
			nodePropertiesProjection.add(node.project(Cypher.asterisk()));
		}

		nodePropertiesProjection.add(Constants.NAME_OF_LABELS);
		nodePropertiesProjection.add(Functions.labels(node));
		nodePropertiesProjection.add(Constants.NAME_OF_INTERNAL_ID);
		nodePropertiesProjection.add(Functions.id(node));
		return nodePropertiesProjection;
	}

	/**
	 * @see CypherGenerator#projectNodeProperties
	 */
	private List<Object> generateListsFor(Collection<RelationshipDescription> relationships, SymbolicName nodeName,
			List<RelationshipDescription> processedRelationships) {

		List<Object> mapProjectionLists = new ArrayList<>();

		for (RelationshipDescription relationshipDescription : relationships) {

			String fieldName = relationshipDescription.getFieldName();

			// if we already processed the other way before, do not try to jump in the infinite loop
			// unless it is a root node relationship
			if (!nodeName.equals(Constants.NAME_OF_ROOT_NODE) && relationshipDescription.hasRelationshipObverse()
					&& processedRelationships.contains(relationshipDescription.getRelationshipObverse())) {
				continue;
			}

			generateListFor(relationshipDescription, nodeName, processedRelationships, fieldName, mapProjectionLists);
		}

		return mapProjectionLists;
	}

	private void generateListFor(RelationshipDescription relationshipDescription, SymbolicName nodeName,
			List<RelationshipDescription> processedRelationships, String fieldName, List<Object> mapProjectionLists) {

		String relationshipType = relationshipDescription.getType();
		String relationshipTargetName = relationshipDescription.generateRelatedNodesCollectionName(relationshipDescription.getSource());
		String sourcePrimaryLabel = relationshipDescription.getSource().getMostAbstractParentLabel(relationshipDescription.getSource());
		String targetPrimaryLabel = relationshipDescription.getTarget().getPrimaryLabel();
		List<String> targetAdditionalLabels = relationshipDescription.getTarget().getAdditionalLabels();
		String relationshipSymbolicName = sourcePrimaryLabel + RelationshipDescription.NAME_OF_RELATIONSHIP + targetPrimaryLabel;

		Node startNode = anyNode(nodeName);
		SymbolicName relationshipFieldName = nodeName.concat("_" + fieldName);
		Node endNode = node(targetPrimaryLabel, targetAdditionalLabels).named(relationshipFieldName);
		NodeDescription<?> endNodeDescription = relationshipDescription.getTarget();

		processedRelationships.add(relationshipDescription);

		if (relationshipDescription.isDynamic()) {
			Relationship relationship = relationshipDescription.isOutgoing()
					? startNode.relationshipTo(endNode)
					: startNode.relationshipFrom(endNode);
			relationship = relationship.named(relationshipTargetName);

			MapProjection mapProjection = projectAllPropertiesAndRelationships(endNodeDescription, relationshipFieldName,
					new ArrayList<>(processedRelationships));

			if (relationshipDescription.hasRelationshipProperties()) {
				relationship = relationship.named(relationshipSymbolicName);
				mapProjection = mapProjection.and(relationship);
			}

			addMapProjection(relationshipTargetName,
					listBasedOn(relationship).returning(mapProjection
							.and(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE, Functions.type(relationship))),
					mapProjectionLists);

		} else {
			Relationship relationship = relationshipDescription.isOutgoing()
					? startNode.relationshipTo(endNode, relationshipType)
					: startNode.relationshipFrom(endNode, relationshipType);

			MapProjection mapProjection = projectAllPropertiesAndRelationships(endNodeDescription, relationshipFieldName,
					new ArrayList<>(processedRelationships));

			if (relationshipDescription.hasRelationshipProperties()) {
				relationship = relationship.named(relationshipSymbolicName);
				mapProjection = mapProjection.and(relationship);
			}

			addMapProjection(relationshipTargetName, listBasedOn(relationship).returning(mapProjection), mapProjectionLists);
		}
	}

	private void addMapProjection(String name, Object projection, List<Object> projectionList) {
		projectionList.add(name);
		projectionList.add(projection);
	}

	private static Condition conditionOrNoCondition(@Nullable Condition condition) {
		return condition == null ? Conditions.noCondition() : condition;
	}

	private static class RelationshipProcessState {
		private final RelationshipPattern relationship;
		private final boolean done;

		RelationshipProcessState(RelationshipPattern relationship, boolean done) {
			this.relationship = relationship;
			this.done = done;
		}
	}

}
