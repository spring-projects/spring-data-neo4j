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

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.listBasedOn;
import static org.neo4j.cypherdsl.core.Cypher.literalOf;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.optionalMatch;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.MapProjection;
import org.neo4j.cypherdsl.core.NamedPath;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
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
import org.springframework.data.neo4j.core.schema.Relationship.Direction;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_ROOT_NODE);

		List<Expression> expressions = new ArrayList<>();
		expressions.add(Constants.NAME_OF_ROOT_NODE);
		expressions.add(Functions.id(rootNode).as(Constants.NAME_OF_INTERNAL_ID));
		Collection<RelationshipDescription> relationships = getRelationshipDescriptionsUpAndDown(nodeDescription);
		StatementBuilder.OngoingReadingWithoutWhere dings = match(rootNode);
		if (nodeDescription.containsPossibleCircles()) {
			RelationshipPattern pattern = createRelationships(rootNode, relationships);
			NamedPath p = Cypher.path("p").definedBy(pattern);
			dings = match(p);
			expressions.add(p.getRequiredSymbolicName());
		}
		return dings.where(conditionOrNoCondition(condition)).with(expressions.toArray(new Expression[] {}));
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

				Statement updateIfExists = updateDecorator
						.apply(match(rootNode).where(rootNode.property(nameOfIdProperty).isEqualTo(idParameter))
								.and(rootNode.property(versionProperty.getName()).isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM)))
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode.internalId()).build();
				return Cypher.union(createIfNew, updateIfExists);

			} else {
				return updateDecorator.apply(Cypher.merge(rootNode.withProperties(nameOfIdProperty, idParameter)).set(rootNode,
						parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode.internalId()).build();
			}
		} else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

			Statement createIfNew;
			Statement updateIfExists;

			if (((Neo4jPersistentEntity) nodeDescription).hasVersionProperty()) {

				PersistentProperty versionProperty = ((Neo4jPersistentEntity) nodeDescription).getRequiredVersionProperty();

				createIfNew = updateDecorator
						.apply(optionalMatch(possibleExistingNode).where(possibleExistingNode.internalId().isEqualTo(idParameter))
								.with(possibleExistingNode).where(possibleExistingNode.isNull()).create(rootNode)
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode.internalId()).build();

				updateIfExists = updateDecorator.apply(match(rootNode).where(rootNode.internalId().isEqualTo(idParameter))
						.and(rootNode.property(versionProperty.getName()).isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM)))
						.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode.internalId()).build();
			} else {
				createIfNew = updateDecorator
						.apply(optionalMatch(possibleExistingNode).where(possibleExistingNode.internalId().isEqualTo(idParameter))
								.with(possibleExistingNode).where(possibleExistingNode.isNull()).create(rootNode)
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode.internalId()).build();

				updateIfExists = updateDecorator.apply(match(rootNode).where(rootNode.internalId().isEqualTo(idParameter))
						.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode.internalId()).build();
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
				.set(rootNode, Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM))
				.returning(Functions.collect(rootNode.property(nameOfIdProperty)).as(Constants.NAME_OF_IDS)).build();
	}

	@NonNull
	public Statement prepareSaveOfRelationship(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType, Long relatedInternalId) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds()
				? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
						.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		return match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? startNode.internalId().isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.match(endNode).where(endNode.internalId().isEqualTo(literalOf(relatedInternalId)))
				.merge(relationship.isOutgoing() ? startNode.relationshipTo(endNode, type)
						: startNode.relationshipFrom(endNode, type))
				.build();
	}

	@NonNull
	public Statement prepareSaveOfRelationshipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType, Long relatedInternalId) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels()).named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		Parameter relationshipProperties = parameter(Constants.NAME_OF_PROPERTIES_PARAM);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();

		Relationship relOutgoing = startNode.relationshipTo(endNode, type).named(RELATIONSHIP_NAME);
		Relationship relIncoming = startNode.relationshipFrom(endNode, type).named(RELATIONSHIP_NAME);

		return match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? startNode.internalId().isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.match(endNode).where(endNode.internalId().isEqualTo(literalOf(relatedInternalId)))
				.merge(relationship.isOutgoing() ? relOutgoing : relIncoming).set(RELATIONSHIP_NAME, relationshipProperties)
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
				.delete(relationship.getSymbolicName().get()).build();
	}

	public Expression[] createReturnStatementForMatch(NodeDescription<?> nodeDescription) {
		return createReturnStatementForMatch(nodeDescription, null);
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
	 * @param inputProperties A list of Java properties of the domain to be included. Those properties are compared with
	 *          the field names of graph properties respectively relationships.
	 * @return An expresion to be returned by a Cypher statement
	 */
	public Expression[] createReturnStatementForMatch(NodeDescription<?> nodeDescription,
			@Nullable List<String> inputProperties) {

		List<RelationshipDescription> processedRelationships = new ArrayList<>();
		if (nodeDescription.containsPossibleCircles()) {
			List<Expression> returnExpressions = new ArrayList<>();
			Node rootNode = anyNode(Constants.NAME_OF_ROOT_NODE);
			NamedPath p = Cypher.path("p").get();
			returnExpressions.add(rootNode.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
			returnExpressions.add(Functions.relationships(p).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS));
			returnExpressions.add(Functions.nodes(p).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
			return returnExpressions.toArray(new Expression[]{});
		} else {
			Predicate<String> includeField = s -> inputProperties == null || inputProperties.isEmpty()
					|| inputProperties.contains(s);
			return new Expression[]{projectPropertiesAndRelationships(nodeDescription, Constants.NAME_OF_ROOT_NODE, includeField, processedRelationships)};
		}
	}

	// recursive entry point for relationships in return statement
	private MapProjection projectAllPropertiesAndRelationships(NodeDescription<?> nodeDescription, SymbolicName nodeName,
			List<RelationshipDescription> processedRelationships) {

		Predicate<String> includeAllFields = (field) -> true;
		// Because we are getting called recursive, there cannot be any circle
		return projectPropertiesAndRelationships(nodeDescription, nodeName, includeAllFields, processedRelationships);
	}

	private MapProjection projectPropertiesAndRelationships(NodeDescription<?> nodeDescription, SymbolicName nodeName,
			Predicate<String> includeProperty, List<RelationshipDescription> processedRelationships) {

		List<Object> contentOfProjection = new ArrayList<>();

		Collection<RelationshipDescription> relationships = getRelationshipDescriptionsUpAndDown(nodeDescription);
		relationships.removeIf(r -> !includeProperty.test(r.getFieldName()));

		if (nodeDescription.containsPossibleCircles()) {
			Node node = anyNode(nodeName);
			RelationshipPattern pattern = createRelationships(node, relationships);
			NamedPath p = Cypher.path("p").definedBy(pattern);
			contentOfProjection.add(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE);
			contentOfProjection.add(Constants.NAME_OF_ROOT_NODE);
			contentOfProjection.add(Constants.NAME_OF_PATHS);
			contentOfProjection.add(Cypher.listBasedOn(p).returning(p));
		} else {
			contentOfProjection.addAll(generateListsFor(relationships, nodeName, processedRelationships));
		}
		return Cypher.anyNode(nodeName).project(contentOfProjection);
	}

	@NonNull
	static Collection<RelationshipDescription> getRelationshipDescriptionsUpAndDown(NodeDescription<?> nodeDescription) {
		Collection<RelationshipDescription> relationships = new HashSet<>(nodeDescription.getRelationships());

		for (NodeDescription<?> childDescription : nodeDescription.getChildNodeDescriptionsInHierarchy()) {
			childDescription.getRelationships().forEach(concreteRelationship -> {

				String fieldName = concreteRelationship.getFieldName();

				if (relationships.stream().noneMatch(relationship -> relationship.getFieldName().equals(fieldName))) {
					relationships.add(concreteRelationship);
				}
			});
		}
		return relationships;
	}

	private RelationshipPattern createRelationships(Node node, Collection<RelationshipDescription> relationshipDescriptions) {
		RelationshipPattern relationship;

		Direction determinedDirection = determineDirection(relationshipDescriptions);
		if (Direction.OUTGOING.equals(determinedDirection)) {
			relationship = node.relationshipTo(anyNode(), collectFirstLevelRelationshipTypes(relationshipDescriptions));
		} else if (Direction.INCOMING.equals(determinedDirection)) {
			relationship = node.relationshipFrom(anyNode(), collectFirstLevelRelationshipTypes(relationshipDescriptions));
		} else {
			relationship = node.relationshipBetween(anyNode(), collectFirstLevelRelationshipTypes(relationshipDescriptions));
		}

		Set<RelationshipDescription> processedRelationshipDescriptions = new HashSet<>(relationshipDescriptions);
		for (RelationshipDescription relationshipDescription : relationshipDescriptions) {
			Collection<RelationshipDescription> relationships = relationshipDescription.getTarget().getRelationships();
			if (relationships.size() > 0) {
				relationship = createRelationships(relationship, relationships, processedRelationshipDescriptions)
						.relationship;
			}
		}

		return relationship;
	}

	private RelationshipProcessState createRelationships(RelationshipPattern existingRelationship,
				Collection<RelationshipDescription> relationshipDescriptions,
				Set<RelationshipDescription> processedRelationshipDescriptions) {

		RelationshipPattern relationship = existingRelationship;
		String[] relationshipTypes = collectAllRelationshipTypes(relationshipDescriptions);
		if (processedRelationshipDescriptions.containsAll(relationshipDescriptions)) {
			return new RelationshipProcessState(
					relationship.relationshipBetween(anyNode(),
							relationshipTypes).unbounded().min(0), true);
		}
		processedRelationshipDescriptions.addAll(relationshipDescriptions);

		// we can process through the path
		if (relationshipDescriptions.size() == 1) {
			RelationshipDescription relationshipDescription = relationshipDescriptions.iterator().next();
			switch (relationshipDescription.getDirection()) {
				case OUTGOING:
					relationship = existingRelationship.relationshipTo(anyNode(),
							collectFirstLevelRelationshipTypes(relationshipDescriptions)).unbounded().min(0).max(1);
					break;
				case INCOMING:
					relationship = existingRelationship.relationshipFrom(anyNode(),
							collectFirstLevelRelationshipTypes(relationshipDescriptions)).unbounded().min(0).max(1);
					break;
				default:
					relationship = existingRelationship.relationshipBetween(anyNode(),
							collectFirstLevelRelationshipTypes(relationshipDescriptions)).unbounded().min(0).max(1);
			}

			RelationshipProcessState relationships = createRelationships(relationship,
					relationshipDescription.getTarget().getRelationships(), processedRelationshipDescriptions);

			if (!relationships.done) {
				relationship = relationships.relationship;
			}
		} else {
			Direction determinedDirection = determineDirection(relationshipDescriptions);
			if (Direction.OUTGOING.equals(determinedDirection)) {
				relationship = existingRelationship.relationshipTo(anyNode(), relationshipTypes).unbounded().min(0);
			} else if (Direction.INCOMING.equals(determinedDirection)) {
				relationship = existingRelationship.relationshipFrom(anyNode(), relationshipTypes).unbounded().min(0);
			} else {
				relationship = existingRelationship.relationshipBetween(anyNode(), relationshipTypes).unbounded().min(0);
			}
			return new RelationshipProcessState(relationship, true);
		}
		return new RelationshipProcessState(relationship, false);
	}

	@Nullable
	Direction determineDirection(Collection<RelationshipDescription> relationshipDescriptions) {

		Direction direction = null;
		for (RelationshipDescription relationshipDescription : relationshipDescriptions) {
			if (direction == null) {
				direction = relationshipDescription.getDirection();
			}
			if (!direction.equals(relationshipDescription.getDirection())) {
				return null;
			}
		}
		return direction;
	}

	private String[] collectFirstLevelRelationshipTypes(Collection<RelationshipDescription> relationshipDescriptions) {
		Set<String> relationshipTypes = new HashSet<>();

		for (RelationshipDescription relationshipDescription : relationshipDescriptions) {
			String relationshipType = relationshipDescription.getType();
			if (relationshipTypes.contains(relationshipType)) {
				continue;
			}
			if (relationshipDescription.isDynamic()) {
				relationshipTypes.clear();
				continue;
			}
			relationshipTypes.add(relationshipType);
		}
		return relationshipTypes.toArray(new String[0]);
	}

	private String[] collectAllRelationshipTypes(Collection<RelationshipDescription> relationshipDescriptions) {
		Set<String> relationshipTypes = new HashSet<>();

		for (RelationshipDescription relationshipDescription : relationshipDescriptions) {
			String relationshipType = relationshipDescription.getType();
			if (relationshipDescription.isDynamic()) {
				relationshipTypes.clear();
				continue;
			}
			relationshipTypes.add(relationshipType);
			collectAllRelationshipTypes(relationshipDescription.getTarget(), relationshipTypes, new HashSet<>(relationshipDescriptions));
		}
		return relationshipTypes.toArray(new String[0]);
	}

	private void collectAllRelationshipTypes(NodeDescription<?> nodeDescription, Set<String> relationshipTypes,
											 Collection<RelationshipDescription> processedRelationshipDescriptions) {

		for (RelationshipDescription relationshipDescription : nodeDescription.getRelationships()) {
			String relationshipType = relationshipDescription.getType();
			if (processedRelationshipDescriptions.contains(relationshipDescription)) {
				continue;
			}
			relationshipTypes.add(relationshipType);
			processedRelationshipDescriptions.add(relationshipDescription);
			collectAllRelationshipTypes(relationshipDescription.getTarget(), relationshipTypes,
					processedRelationshipDescriptions);
		}
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
