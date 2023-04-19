/*
 * Copyright 2011-2023 the original author or authors.
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
import static org.neo4j.cypherdsl.core.Functions.coalesce;
import static org.neo4j.cypherdsl.core.Functions.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.IdentifiableElement;
import org.neo4j.cypherdsl.core.MapProjection;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingMatchAndUpdate;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingUpdate;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;
import org.neo4j.cypherdsl.core.utils.Assertions;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.neo4j.core.schema.TargetNode;
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
	private static final Pattern LOOKS_LIKE_A_FUNCTION = Pattern.compile(".+\\(.*\\)");

	/**
	 * @param nodeDescription The node description for which a match clause should be generated
	 * @return An ongoing match
	 * @see #prepareMatchOf(NodeDescription, Condition)
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription) {
		return prepareMatchOf(nodeDescription, null);
	}

	/**
	 * This will create a match statement that fits the given node description and may contain additional conditions. The
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

		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(rootNode.getRequiredSymbolicName());
		expressions.add(Functions.id(rootNode).as(Constants.NAME_OF_INTERNAL_ID));
		expressions.add(Functions.elementId(rootNode).as(Constants.NAME_OF_ELEMENT_ID));

		return match(rootNode).where(conditionOrNoCondition(condition)).with(expressions.toArray(IdentifiableElement[]::new));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
														  @Nullable List<PatternElement> initialMatchOn,
														  @Nullable Condition condition) {
		Node rootNode = createRootNode(nodeDescription);

		StatementBuilder.OngoingReadingWithoutWhere match = prepareMatchOfRootNode(rootNode, initialMatchOn);

		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(Functions.collect(Functions.id(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));

		return match
				.where(conditionOrNoCondition(condition))
				.with(expressions.toArray(IdentifiableElement[]::new));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
			  RelationshipDescription relationshipDescription, @Nullable List<PatternElement> initialMatchOn,
														  @Nullable Condition condition) {

		Node rootNode = createRootNode(nodeDescription);

		StatementBuilder.OngoingReadingWithoutWhere match = prepareMatchOfRootNode(rootNode, initialMatchOn);

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

		Relationship relationship = switch (relationshipDescription.getDirection()) {
			case OUTGOING -> rootNode.relationshipTo(targetNode, types);
			case INCOMING -> rootNode.relationshipFrom(targetNode, types);
		};

		relationship = relationship.named(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(Functions.collect(Functions.id(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		expressions.add(Functions.collect(Functions.id(targetNode)).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		expressions.add(Functions.collect(Functions.id(relationship)).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS));

		return match
				.where(conditionOrNoCondition(condition))
				.optionalMatch(relationship)
				.with(expressions.toArray(IdentifiableElement[]::new));
	}

	@NonNull
	public Node createRootNode(NodeDescription<?> nodeDescription) {
		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		return node(primaryLabel, additionalLabels).named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
	}

	private StatementBuilder.OngoingReadingWithoutWhere prepareMatchOfRootNode(
			Node rootNode, @Nullable List<PatternElement> initialMatchOn
	) {

		StatementBuilder.OngoingReadingWithoutWhere match = null;
		if (initialMatchOn == null || initialMatchOn.isEmpty()) {
			match = Cypher.match(rootNode);
		} else {
			for (PatternElement patternElement : initialMatchOn) {
				if (match == null) {
					match = Cypher.match(patternElement);
				} else {
					match = match.match(patternElement);
				}
			}
		}
		return match;
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

		IdDescription idDescription = nodeDescription.getIdDescription();
		Assert.notNull(idDescription, "Cannot load specific nodes by id without a corresponding attribute");

		final Node rootNode = createRootNode(nodeDescription);

		Condition versionCondition;
		if (((Neo4jPersistentEntity<?>) nodeDescription).hasVersionProperty()) {

			PersistentProperty<?> versionProperty = ((Neo4jPersistentEntity<?>) nodeDescription).getRequiredVersionProperty();
			versionCondition = rootNode.property(versionProperty.getName())
					.isEqualTo(coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)));
		} else {
			versionCondition = Conditions.noCondition();
		}

		return match(rootNode)
				.where(idDescription.asIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
				.and(versionCondition).unwind(rootNode.labels()).as("label").with(Cypher.name("label"))
				.where(Cypher.name("label").in(parameter(Constants.NAME_OF_STATIC_LABELS_PARAM)).not())
				.returning(Functions.collect(Cypher.name("label")).as(Constants.NAME_OF_LABELS)).build();
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription) {
		return prepareDeleteOf(nodeDescription, null);
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription, @Nullable Condition condition) {

		return prepareDeleteOf(nodeDescription, condition, false);
	}

	public Statement prepareDeleteOf(NodeDescription<?> nodeDescription, @Nullable Condition condition, boolean count) {

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
				.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
		OngoingUpdate ongoingUpdate = match(rootNode).where(conditionOrNoCondition(condition)).detachDelete(rootNode);
		if (count) {
			return ongoingUpdate.returning(Functions.count(rootNode)).build();
		}
		return ongoingUpdate.build();
	}

	public Condition createCompositePropertyCondition(GraphPropertyDescription idProperty, SymbolicName containerName, Expression actualParameter) {

		if (!idProperty.isComposite()) {
			return Cypher.property(containerName, idProperty.getPropertyName()).isEqualTo(actualParameter);
		}

		Neo4jPersistentProperty property = (Neo4jPersistentProperty) idProperty;

		Condition result = Conditions.noCondition();
		for (String key : property.getOptionalConverter().write(null).keys()) {
			Property expression = Cypher.property(containerName, key);
			result = result.and(expression.isEqualTo(actualParameter.property(key)));
		}
		return result;
	}

	public Statement prepareSaveOf(NodeDescription<?> nodeDescription,
			UnaryOperator<OngoingMatchAndUpdate> updateDecorator) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels).named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
		IdDescription idDescription = nodeDescription.getIdDescription();
		Assert.notNull(idDescription, "Cannot save individual nodes without an id attribute");
		Parameter<?> idParameter = parameter(Constants.NAME_OF_ID);

		if (!idDescription.isInternallyGeneratedId()) {
			GraphPropertyDescription idPropertyDescription = ((Neo4jPersistentEntity<?>) nodeDescription).getRequiredIdProperty();

			if (((Neo4jPersistentEntity<?>) nodeDescription).hasVersionProperty()) {
				Property versionProperty = rootNode.property(((Neo4jPersistentEntity<?>) nodeDescription).getRequiredVersionProperty().getName());
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(createCompositePropertyCondition(idPropertyDescription, possibleExistingNode.getRequiredSymbolicName(), idParameter))
						.with(possibleExistingNode)
						.where(possibleExistingNode.isNull())
						.create(rootNode.withProperties(versionProperty, literalOf(0)))
						.with(rootNode)
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode)
						.build();

				Statement updateIfExists = updateDecorator.apply(match(rootNode)
						.where(createCompositePropertyCondition(idPropertyDescription, rootNode.getRequiredSymbolicName(), idParameter))
						.and(versionProperty.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))) // Initial check
						.set(versionProperty.to(versionProperty.add(literalOf(1)))) // Acquire lock
						.with(rootNode)
						.where(versionProperty.isEqualTo(coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)).add(
								literalOf(1))))
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode)
						.build();
				return Cypher.union(createIfNew, updateIfExists);

			} else {
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
								.where(createCompositePropertyCondition(idPropertyDescription, possibleExistingNode.getRequiredSymbolicName(), idParameter))
								.with(possibleExistingNode)
								.where(possibleExistingNode.isNull())
								.create(rootNode)
								.with(rootNode)
								.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode)
						.build();

				Statement updateIfExists = updateDecorator.apply(match(rootNode)
								.where(createCompositePropertyCondition(idPropertyDescription, rootNode.getRequiredSymbolicName(), idParameter))
								.with(rootNode)
								.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode)
						.build();
				return Cypher.union(createIfNew, updateIfExists);
			}
		} else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

			Statement createIfNew;
			Statement updateIfExists;

			if (((Neo4jPersistentEntity<?>) nodeDescription).hasVersionProperty()) {
				Property versionProperty = rootNode.property(((Neo4jPersistentEntity<?>) nodeDescription).getRequiredVersionProperty().getName());

				createIfNew = updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(Functions.id(possibleExistingNode).isEqualTo(idParameter))
						.with(possibleExistingNode)
						.where(possibleExistingNode.isNull())
						.create(rootNode.withProperties(versionProperty, literalOf(0)))
						.with(rootNode)
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode)
						.build();

				updateIfExists = updateDecorator.apply(match(rootNode)
						.where(Functions.id(rootNode).isEqualTo(idParameter))
						.and(versionProperty.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))) // Initial check
						.set(versionProperty.to(versionProperty.add(literalOf(1)))) // Acquire lock
						.with(rootNode)
						.where(versionProperty.isEqualTo(coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)).add(
								literalOf(1))))
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode).build();
			} else {
				createIfNew = updateDecorator
						.apply(optionalMatch(possibleExistingNode).where(Functions.id(possibleExistingNode).isEqualTo(idParameter))
								.with(possibleExistingNode).where(possibleExistingNode.isNull()).create(rootNode)
								.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM)))
						.returning(rootNode).build();

				updateIfExists = updateDecorator.apply(match(rootNode).where(Functions.id(rootNode).isEqualTo(idParameter))
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))).returning(rootNode).build();
			}

			return Cypher.union(createIfNew, updateIfExists);
		}
	}

	public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

		Assert.isTrue(!nodeDescription.isUsingInternalIds(),
				"Only entities that use external IDs can be saved in a batch");

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
				.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
		IdDescription idDescription = nodeDescription.getIdDescription();

		@SuppressWarnings("ConstantConditions") // We now already that the node is using internal ids, and as such, an IdDescription must be present
		String nameOfIdProperty = idDescription.getOptionalGraphPropertyName()
				.orElseThrow(() -> new MappingException("External id does not correspond to a graph property"));

		String row = "entity";
		return Cypher.unwind(parameter(Constants.NAME_OF_ENTITY_LIST_PARAM)).as(row)
				.merge(rootNode.withProperties(nameOfIdProperty, Cypher.property(row, Constants.NAME_OF_ID)))
				.mutate(rootNode, Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM))
				.returning(
						Functions.id(rootNode).as(Constants.NAME_OF_INTERNAL_ID),
						Functions.elementId(rootNode).as(Constants.NAME_OF_ELEMENT_ID),
						rootNode.property(nameOfIdProperty).as(Constants.NAME_OF_ID)
				)
				.build();
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

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		Relationship relationshipFragment = (relationship.isOutgoing() ?
				startNode.relationshipTo(endNode, type) :
				startNode.relationshipFrom(endNode, type)).named(RELATIONSHIP_NAME);

		Function<Node, Expression> startNodeIdFunction = null;
		if(neo4jPersistentEntity.isUsingInternalIds()) {
			if (Neo4jPersistentEntity.DEPRECATED_GENERATED_ID_TYPES.contains(neo4jPersistentEntity.getRequiredIdProperty().getType())) {
				startNodeIdFunction = Functions::id;
			} else {
				startNodeIdFunction = Functions::elementId;
			}
		} else {
			startNodeIdFunction = node -> node.property(idPropertyName);
		}

		return match(startNode)
				.where(startNodeIdFunction.apply(startNode).isEqualTo(idParameter))
				.match(endNode).where(Functions.elementId(endNode).isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)))
				.merge(relationshipFragment)
				.returning(
						Functions.id(relationshipFragment).as(Constants.NAME_OF_INTERNAL_ID),
						Functions.elementId(relationshipFragment).as(Constants.NAME_OF_ELEMENT_ID)
				)
				.build();
	}
	@NonNull
	public Statement prepareSaveOfRelationships(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType) {

		final Node startNode = neo4jPersistentEntity.isUsingInternalIds()
				? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
						.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		Relationship relationshipFragment = (relationship.isOutgoing() ?
				startNode.relationshipTo(endNode, type) :
				startNode.relationshipFrom(endNode, type)).named(RELATIONSHIP_NAME);

		String row = "relationship";
		Property idProperty = Cypher.property(row, Constants.FROM_ID_PARAMETER_NAME);
		return Cypher.unwind(parameter(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM)).as(row)
				.with(row)
				.match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds()
						? Functions.elementId(startNode).isEqualTo(idProperty)
						: startNode.property(idPropertyName).isEqualTo(idProperty))
				.match(endNode).where(Functions.elementId(endNode).isEqualTo(Cypher.property(row, Constants.TO_ID_PARAMETER_NAME)))
				.merge(relationshipFragment)
				.returning(
						Functions.id(relationshipFragment).as(Constants.NAME_OF_INTERNAL_ID),
						Functions.elementId(relationshipFragment).as(Constants.NAME_OF_ELEMENT_ID)
				)
				.build();
	}

	@NonNull
	public Statement prepareSaveOfRelationshipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
				 RelationshipDescription relationship,
				 boolean isNew,
				 @Nullable String dynamicRelationshipType) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels()).named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		Parameter<?> relationshipProperties = parameter(Constants.NAME_OF_PROPERTIES_PARAM);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();

		Relationship relationshipFragment = (
				relationship.isOutgoing() ?
						startNode.relationshipTo(endNode, type) :
						startNode.relationshipFrom(endNode, type))
				.named(RELATIONSHIP_NAME);

		StatementBuilder.OngoingReadingWithWhere startAndEndNodeMatch = match(startNode)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? Functions.id(startNode).isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.match(endNode).where(Functions.id(endNode).isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)));

		StatementBuilder.ExposesSet createOrMatch = isNew
				? startAndEndNodeMatch.create(relationshipFragment)
				: startAndEndNodeMatch.match(relationshipFragment)
					.where(Functions.id(relationshipFragment).isEqualTo(Cypher.parameter(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM)));
		return createOrMatch
				.mutate(RELATIONSHIP_NAME, relationshipProperties)
				.returning(
						Functions.id(relationshipFragment).as(Constants.NAME_OF_INTERNAL_ID),
						Functions.elementId(relationshipFragment).as(Constants.NAME_OF_ELEMENT_ID)
				)
				.build();
	}

	@NonNull
	public Statement prepareUpdateOfRelationshipsWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, boolean isNew) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels()).named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);
		String idPropertyName = neo4jPersistentEntity.getRequiredIdProperty().getPropertyName();

		String type = relationship.getType();

		Relationship relationshipFragment = (
				relationship.isOutgoing() ?
						startNode.relationshipTo(endNode, type) :
						startNode.relationshipFrom(endNode, type))
				.named(RELATIONSHIP_NAME);

		String row = "row";
		Property relationshipProperties = Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM);
		Property idProperty = Cypher.property(row, Constants.FROM_ID_PARAMETER_NAME);
		StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere cypherUnwind = Cypher.unwind(parameter(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM))
				.as(row)
				.with(row);

		// we only need start and end node querying if we have to create a new relationship...
		if (isNew) {
			return cypherUnwind
					.match(startNode)
					.where(neo4jPersistentEntity.isUsingInternalIds() ? internalId(startNode).isEqualTo(idProperty)
							: startNode.property(idPropertyName).isEqualTo(idProperty))
					.match(endNode).where(internalId(endNode).isEqualTo(Cypher.property(row, Constants.TO_ID_PARAMETER_NAME)))
					.create(relationshipFragment)
					.mutate(RELATIONSHIP_NAME, relationshipProperties).returning(
							Functions.id(relationshipFragment).as(Constants.NAME_OF_INTERNAL_ID),
							Functions.elementId(relationshipFragment).as(Constants.NAME_OF_ELEMENT_ID)
					).build();

		}

		// ... otherwise we can just fetch the existing relationship by known id
		return cypherUnwind.match(relationshipFragment)
			.where(Functions.id(relationshipFragment).isEqualTo(Cypher.property(row, Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM)))
			.mutate(RELATIONSHIP_NAME, relationshipProperties).build();
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

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		return match(relationship)
				.where(neo4jPersistentEntity.isUsingInternalIds() ? Functions.id(startNode).isEqualTo(idParameter)
						: startNode.property(idPropertyName).isEqualTo(idParameter))
				.and(Functions.id(relationship).in(Cypher.parameter(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM)).not())
				.delete(relationship.getRequiredSymbolicName())
				.build();
	}

	public Collection<Expression> createReturnStatementForExists(Neo4jPersistentEntity<?> nodeDescription) {

		return Collections.singleton(Functions.count(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)));
	}

	public Collection<Expression> createReturnStatementForMatch(Neo4jPersistentEntity<?> nodeDescription) {
		return createReturnStatementForMatch(nodeDescription, (pp -> true));
	}

	/**
	 * Creates an order by fragment, assuming the node to match is named `n`
	 *
	 * @param sort The {@link Sort sort} that should be turned into a valid Cypher {@code ORDER}-clause
	 * @return An optional order clause. Will be {@literal null} on sorts that are {@literal null} or unsorted.
	 */
	public @Nullable String createOrderByFragment(@Nullable Sort sort) {

		if (sort == null || sort.isUnsorted()) {
			return null;
		}
		Statement statement = match(anyNode()).returning("n")
				.orderBy(sort.stream().filter(Objects::nonNull).map(order -> {
					String property = order.getProperty().trim();
					Expression expression;
					if (LOOKS_LIKE_A_FUNCTION.matcher(property).matches()) {
						expression = Cypher.raw(property);
					} else if (property.contains(".")) {
						int firstDot = property.indexOf('.');
						String tail = property.substring(firstDot + 1);
						if (tail.isEmpty() || property.lastIndexOf(".") != firstDot) {
							if (tail.trim().matches("`.+`")) {
								tail = tail.replaceFirst("`(.+)`", "$1");
							} else {
								throw new IllegalArgumentException(String.format(
										"Cannot handle order property `%s`, it must be a simple property or one-hop path",
										property));
							}
						}
						expression = Cypher.property(property.substring(0, firstDot), tail);
					} else {
						try {
							Assertions.isTrue(SourceVersion.isIdentifier(property), "Name must be a valid identifier.");
							expression = Cypher.name(property);
						} catch (IllegalArgumentException e) {
							if (e.getMessage().endsWith(".")) {
								throw new IllegalArgumentException(
										e.getMessage().substring(0, e.getMessage().length() - 1));
							}
							throw e;
						}
					}
					if (order.isIgnoreCase()) {
						expression = Functions.toLower(expression);
					}
					return order.isAscending() ? expression.ascending() : expression.descending();
				}).toArray(SortItem[]::new))
				.build();

		String renderedStatement = Renderer.getRenderer(Configuration.defaultConfig()).render(statement);
		return renderedStatement.substring(renderedStatement.indexOf("ORDER BY")).trim();
	}

	/**
	 * @param nodeDescription Description of the root node
	 * @param includeField A predicate derived from the set of included properties. This is only relevant in various forms
	 *                     of projections which allow to exclude one or more fields.
	 * @return An expression to be returned by a Cypher statement
	 */
	public Collection<Expression> createReturnStatementForMatch(Neo4jPersistentEntity<?> nodeDescription,
			Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		List<RelationshipDescription> processedRelationships = new ArrayList<>();
		if (nodeDescription.containsPossibleCircles(includeField)) {
			return createGenericReturnStatement();
		} else {
			return Collections.singleton(projectPropertiesAndRelationships(
					PropertyFilter.RelaxedPropertyPath.withRootType(nodeDescription.getUnderlyingClass()),
					nodeDescription,
					Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription),
					includeField,
					null,
					processedRelationships));
		}
	}

	public Collection<Expression> createGenericReturnStatement() {
		List<Expression> returnExpressions = new ArrayList<>();
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS));
		return returnExpressions;
	}

	@SuppressWarnings("deprecation")
	private FunctionInvocation internalId(Node node) {
		return node.internalId();
	}

	private MapProjection projectPropertiesAndRelationships(PropertyFilter.RelaxedPropertyPath parentPath, Neo4jPersistentEntity<?> nodeDescription, SymbolicName nodeName,
															Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties, @Nullable RelationshipDescription relationshipDescription, List<RelationshipDescription> processedRelationships) {

		Collection<RelationshipDescription> relationships = ((DefaultNeo4jPersistentEntity<?>) nodeDescription).getRelationshipsInHierarchy(includedProperties, parentPath);
		relationships.removeIf(r -> !includedProperties.test(parentPath.append(r.getFieldName())));

		List<Object> propertiesProjection = projectNodeProperties(parentPath, nodeDescription, nodeName, relationshipDescription, includedProperties);
		List<Object> contentOfProjection = new ArrayList<>(propertiesProjection);

		contentOfProjection.addAll(generateListsFor(parentPath, nodeDescription, relationships, nodeName, includedProperties, processedRelationships));
		return Cypher.anyNode(nodeName).project(contentOfProjection);
	}

	/**
	 * Creates a list of objects that represents a very basic of {@code MapEntry<String, Object>} with the exception that
	 * this list can also contain two "keys" in a row. The {@link MapProjection} will take care to handle them as
	 * self-reflecting fields. Example with self-reflection and explicit value: {@code n {.id, name: n.name}}.
	 */
	private List<Object> projectNodeProperties(PropertyFilter.RelaxedPropertyPath parentPath, NodeDescription<?> nodeDescription, SymbolicName nodeName,
											   @Nullable RelationshipDescription relationshipDescription, Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		List<Object> nodePropertiesProjection = new ArrayList<>();
		Node node = anyNode(nodeName);

		boolean hasCompositeProperties = false;
		for (GraphPropertyDescription graphProperty : nodeDescription.getGraphPropertiesInHierarchy()) {

			Neo4jPersistentProperty property = (Neo4jPersistentProperty) graphProperty;
			hasCompositeProperties = hasCompositeProperties || property.isComposite();

			if (property.isDynamicLabels() || property.isComposite()) {
				continue;
			}
			PropertyFilter.RelaxedPropertyPath from;

			if (relationshipDescription == null) {
				from = parentPath.append(property.getFieldName());

			} else if (relationshipDescription.hasRelationshipProperties()) {
				@SuppressWarnings("ConstantConditions") // All prerequisites have been checked by the persistence context at this point.
				String relationshipPropertyTargetNodeFieldName =
						((Neo4jPersistentEntity<?>) relationshipDescription.getRelationshipPropertiesEntity())
								.getPersistentProperty(TargetNode.class).getFieldName();

				from = parentPath.append(relationshipPropertyTargetNodeFieldName + "." + property.getFieldName());
			} else {
				from = parentPath.append(property.getFieldName());
			}

			if (!includeField.test(from)) {
				continue;
			}

			// ignore internally generated id fields
			if (graphProperty.isIdProperty() && (nodeDescription.getIdDescription() != null && nodeDescription.getIdDescription().isInternallyGeneratedId())) {
				continue;
			}
			nodePropertiesProjection.add(graphProperty.getPropertyName());
		}

		if (hasCompositeProperties || nodeDescription.describesInterface()) {
			nodePropertiesProjection.add(Constants.NAME_OF_ALL_PROPERTIES);
			nodePropertiesProjection.add(node.project(Cypher.asterisk()));
		}

		nodePropertiesProjection.add(Constants.NAME_OF_LABELS);
		nodePropertiesProjection.add(Functions.labels(node));
		nodePropertiesProjection.add(Constants.NAME_OF_INTERNAL_ID);
		nodePropertiesProjection.add(Functions.id(node));
		nodePropertiesProjection.add(Constants.NAME_OF_ELEMENT_ID);
		nodePropertiesProjection.add(Functions.elementId(node));
		return nodePropertiesProjection;
	}

	/**
	 * @see CypherGenerator#projectNodeProperties
	 */
	private List<Object> generateListsFor(PropertyFilter.RelaxedPropertyPath parentPath, Neo4jPersistentEntity<?> nodeDescription, Collection<RelationshipDescription> relationships, SymbolicName nodeName,
										  Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties, List<RelationshipDescription> processedRelationships) {

		List<Object> mapProjectionLists = new ArrayList<>();

		for (RelationshipDescription relationshipDescription : relationships) {

			String fieldName = relationshipDescription.getFieldName();

			// if we already processed the other way before, do not try to jump in the infinite loop
			// unless it is a root node relationship
			if (relationshipDescription.hasRelationshipObverse()
					&& processedRelationships.contains(relationshipDescription.getRelationshipObverse())) {
				continue;
			}

			generateListFor(parentPath, nodeDescription, relationshipDescription, nodeName, processedRelationships, fieldName, mapProjectionLists, includedProperties);
		}

		return mapProjectionLists;
	}

	private void generateListFor(PropertyFilter.RelaxedPropertyPath parentPath, Neo4jPersistentEntity<?> nodeDescription, RelationshipDescription relationshipDescription, SymbolicName nodeName,
								 List<RelationshipDescription> processedRelationships, String fieldName, List<Object> mapProjectionLists, Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties) {

		String relationshipType = relationshipDescription.getType();
		String relationshipTargetName = relationshipDescription.generateRelatedNodesCollectionName(nodeDescription);
		String sourcePrimaryLabel = relationshipDescription.getSource().getMostAbstractParentLabel(nodeDescription);
		String targetPrimaryLabel = relationshipDescription.getTarget().getPrimaryLabel();
		List<String> targetAdditionalLabels = relationshipDescription.getTarget().getAdditionalLabels();
		String relationshipSymbolicName = sourcePrimaryLabel + RelationshipDescription.NAME_OF_RELATIONSHIP + targetPrimaryLabel;

		Node startNode = anyNode(nodeName);
		SymbolicName relationshipFieldName = nodeName.concat("_" + fieldName);
		Node endNode = node(targetPrimaryLabel, targetAdditionalLabels).named(relationshipFieldName);
		Neo4jPersistentEntity<?> endNodeDescription = (Neo4jPersistentEntity<?>) relationshipDescription.getTarget();

		processedRelationships.add(relationshipDescription);
		PropertyFilter.RelaxedPropertyPath newParentPath = parentPath.append(relationshipDescription.getFieldName());

		if (relationshipDescription.isDynamic()) {
			Relationship relationship = relationshipDescription.isOutgoing()
					? startNode.relationshipTo(endNode)
					: startNode.relationshipFrom(endNode);
			relationship = relationship.named(relationshipTargetName);

			MapProjection mapProjection = projectPropertiesAndRelationships(newParentPath, endNodeDescription, relationshipFieldName,
					includedProperties, relationshipDescription, new ArrayList<>(processedRelationships));

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

			MapProjection mapProjection = projectPropertiesAndRelationships(newParentPath, endNodeDescription, relationshipFieldName,
					includedProperties, relationshipDescription, new ArrayList<>(processedRelationships));

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
}
