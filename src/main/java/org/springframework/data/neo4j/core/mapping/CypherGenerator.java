/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javax.lang.model.SourceVersion;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.FunctionInvocation;
import org.neo4j.cypherdsl.core.IdentifiableElement;
import org.neo4j.cypherdsl.core.MapProjection;
import org.neo4j.cypherdsl.core.Named;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Parameter;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.neo4j.cypherdsl.core.StatementBuilder;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingMatchAndUpdate;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingReadingWithoutWhere;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingUpdate;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Renderer;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.util.Assert;

import static org.neo4j.cypherdsl.core.Cypher.anyNode;
import static org.neo4j.cypherdsl.core.Cypher.coalesce;
import static org.neo4j.cypherdsl.core.Cypher.collect;
import static org.neo4j.cypherdsl.core.Cypher.listBasedOn;
import static org.neo4j.cypherdsl.core.Cypher.literalOf;
import static org.neo4j.cypherdsl.core.Cypher.match;
import static org.neo4j.cypherdsl.core.Cypher.node;
import static org.neo4j.cypherdsl.core.Cypher.optionalMatch;
import static org.neo4j.cypherdsl.core.Cypher.parameter;

/**
 * A generator based on the schema defined by node and relationship descriptions. Most
 * methods return renderable Cypher statements.
 *
 * @author Michael J. Simons
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public enum CypherGenerator {

	/**
	 * The sole instance of this generator.
	 */
	INSTANCE;

	private static final SymbolicName START_NODE_NAME = Cypher.name("startNode");

	private static final SymbolicName END_NODE_NAME = Cypher.name("endNode");

	private static final SymbolicName RELATIONSHIP_NAME = Cypher.name("relProps");

	private static final Pattern LOOKS_LIKE_A_FUNCTION = Pattern.compile(".+\\(.*\\)");

	// keeping elementId/id function selection in one place within this class
	// default elementId function
	private Function<Named, FunctionInvocation> elementIdOrIdFunction = named -> {
		if (named instanceof Node node) {
			return Cypher.elementId(node);
		}
		else if (named instanceof Relationship relationship) {
			return Cypher.elementId(relationship);
		}
		else {
			throw new IllegalArgumentException("Unsupported CypherDSL type: " + named.getClass());
		}
	};

	@SuppressWarnings("deprecation")
	private static Function<Node, Expression> getNodeIdFunction(Neo4jPersistentEntity<?> entity,
			boolean canUseElementId) {

		Function<Node, Expression> startNodeIdFunction;
		var idProperty = entity.getRequiredIdProperty();
		if (entity.isUsingInternalIds()) {
			if (entity.isUsingDeprecatedInternalId() || !canUseElementId) {
				startNodeIdFunction = Node::internalId;
			}
			else {
				startNodeIdFunction = Cypher::elementId;
			}
		}
		else {
			startNodeIdFunction = node -> node.property(idProperty.getPropertyName());
		}
		return startNodeIdFunction;
	}

	@SuppressWarnings("deprecation")
	private static Function<Node, Expression> getEndNodeIdFunction(Neo4jPersistentEntity<?> entity,
			boolean canUseElementId) {

		Function<Node, Expression> startNodeIdFunction;
		if (entity == null) {
			return Cypher::elementId;
		}
		if (!entity.isUsingDeprecatedInternalId() && canUseElementId) {
			startNodeIdFunction = Cypher::elementId;
		}
		else {
			startNodeIdFunction = Node::internalId;
		}
		return startNodeIdFunction;
	}

	static Expression relId(Relationship r) {
		return FunctionInvocation.create(() -> "id", r.getRequiredSymbolicName());
	}

	private static Function<Relationship, Expression> getRelationshipIdFunction(
			RelationshipDescription relationshipDescription, boolean canUseElementId) {

		Function<Relationship, Expression> result = canUseElementId ? Cypher::elementId : CypherGenerator::relId;
		if (relationshipDescription.hasRelationshipProperties()) {
			Neo4jPersistentEntity<?> entity = (Neo4jPersistentEntity<?>) relationshipDescription
				.getRelationshipPropertiesEntity();
			if ((entity != null && entity.isUsingDeprecatedInternalId()) || !canUseElementId) {
				result = CypherGenerator::relId;
			}
			else {
				result = Cypher::elementId;
			}
		}
		return result;
	}

	private static Condition conditionOrNoCondition(@Nullable Condition condition) {
		return (condition != null) ? condition : Cypher.noCondition();
	}

	/**
	 * Set function to be used to query either elementId or id.
	 * @param elementIdOrIdFunction new function to use.
	 */
	public void setElementIdOrIdFunction(Function<Named, FunctionInvocation> elementIdOrIdFunction) {
		this.elementIdOrIdFunction = elementIdOrIdFunction;
	}

	/**
	 * Prepares a match for a given entity.
	 * @param nodeDescription the node description for which a match clause should be
	 * generated
	 * @return an ongoing match
	 * @see #prepareMatchOf(NodeDescription, Condition)
	 */
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription) {
		return prepareMatchOf(nodeDescription, null);
	}

	/**
	 * This will create a match statement that fits the given node description and may
	 * contain additional conditions. The {@code WITH} clause of this statement contains
	 * all nodes and relationships necessary to map a record to the given
	 * {@code nodeDescription}.
	 * <p>
	 * It is recommended to use {@link Cypher#asterisk()} to return everything from the
	 * query in the end.
	 * <p>
	 * The root node is guaranteed to have the symbolic name {@code n}.
	 * @param nodeDescription the node description for which a match clause should be
	 * generated
	 * @param condition an optional conditions to add
	 * @return an ongoing match
	 */
	@SuppressWarnings("deprecation")
	public StatementBuilder.OrderableOngoingReadingAndWith prepareMatchOf(NodeDescription<?> nodeDescription,
			@Nullable Condition condition) {

		Node rootNode = createRootNode(nodeDescription);

		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(rootNode.getRequiredSymbolicName());
		if (nodeDescription instanceof Neo4jPersistentEntity<?> entity && entity.isUsingDeprecatedInternalId()) {
			expressions.add(rootNode.internalId().as(Constants.NAME_OF_INTERNAL_ID));
		}
		expressions.add(this.elementIdOrIdFunction.apply(rootNode).as(Constants.NAME_OF_ELEMENT_ID));

		return match(rootNode).where(conditionOrNoCondition(condition))
			.with(expressions.toArray(IdentifiableElement[]::new));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
			List<PatternElement> initialMatchOn, @Nullable Condition condition) {
		Node rootNode = createRootNode(nodeDescription);

		OngoingReadingWithoutWhere match = prepareMatchOfRootNode(rootNode, initialMatchOn);

		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(
				Cypher.collect(this.elementIdOrIdFunction.apply(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));

		return match.where(conditionOrNoCondition(condition)).with(expressions.toArray(IdentifiableElement[]::new));
	}

	public StatementBuilder.OngoingReading prepareMatchOf(NodeDescription<?> nodeDescription,
			RelationshipDescription relationshipDescription, @Nullable List<PatternElement> initialMatchOn,
			@Nullable Condition condition) {

		Node rootNode = createRootNode(nodeDescription);

		OngoingReadingWithoutWhere match = prepareMatchOfRootNode(rootNode, initialMatchOn);

		Node targetNode = node(relationshipDescription.getTarget().getPrimaryLabel(),
				relationshipDescription.getTarget().getAdditionalLabels())
			.named(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES);

		boolean dynamicRelationship = relationshipDescription.isDynamic();
		Class<?> componentType = ((DefaultRelationshipDescription) relationshipDescription).getInverse()
			.getComponentType();
		List<String> relationshipTypes = new ArrayList<>();
		if (dynamicRelationship && componentType != null && componentType.isEnum()) {
			Arrays.stream(componentType.getEnumConstants())
				.forEach(constantName -> relationshipTypes.add(constantName.toString()));
		}
		else if (!dynamicRelationship) {
			relationshipTypes.add(relationshipDescription.getType());
		}
		String[] types = relationshipTypes.toArray(new String[] {});

		Relationship relationship = switch (relationshipDescription.getDirection()) {
			case OUTGOING -> rootNode.relationshipTo(targetNode, types);
			case INCOMING -> rootNode.relationshipFrom(targetNode, types);
		};

		relationship = relationship.named(Constants.NAME_OF_SYNTHESIZED_RELATIONS);
		List<IdentifiableElement> expressions = new ArrayList<>();
		expressions.add(
				Cypher.collect(this.elementIdOrIdFunction.apply(rootNode)).as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		expressions.add(Cypher.collect(this.elementIdOrIdFunction.apply(targetNode))
			.as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		expressions.add(Cypher.collect(this.elementIdOrIdFunction.apply(relationship))
			.as(Constants.NAME_OF_SYNTHESIZED_RELATIONS));

		return match.where(conditionOrNoCondition(condition))
			.optionalMatch(relationship)
			.with(expressions.toArray(IdentifiableElement[]::new));
	}

	public Node createRootNode(NodeDescription<?> nodeDescription) {
		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		return node(primaryLabel, additionalLabels).named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
	}

	private OngoingReadingWithoutWhere prepareMatchOfRootNode(Node rootNode,
			@Nullable List<PatternElement> initialMatchOn) {

		OngoingReadingWithoutWhere match = null;
		if (initialMatchOn == null || initialMatchOn.isEmpty()) {
			match = Cypher.match(rootNode);
		}
		else {
			for (PatternElement patternElement : initialMatchOn) {
				if (match == null) {
					match = Cypher.match(patternElement);
				}
				else {
					match = match.match(patternElement);
				}
			}
		}
		return Objects.requireNonNull(match);
	}

	/**
	 * Creates a statement that returns all labels of a node that are not part of a list
	 * parameter named {@link Constants#NAME_OF_STATIC_LABELS_PARAM}. Those are the
	 * "dynamic labels" of a node as set through SDN.
	 * @param nodeDescription the node description for which the statement should be
	 * generated
	 * @return a statement having one parameter
	 * @since 6.0
	 */
	public Statement createStatementReturningDynamicLabels(NodeDescription<?> nodeDescription) {

		IdDescription idDescription = Objects.requireNonNull(nodeDescription.getIdDescription(),
				"Cannot load specific nodes by id without a corresponding attribute");

		final Node rootNode = createRootNode(nodeDescription);

		Condition versionCondition;
		if (((Neo4jPersistentEntity<?>) nodeDescription).hasVersionProperty()) {

			PersistentProperty<?> versionProperty = ((Neo4jPersistentEntity<?>) nodeDescription)
				.getRequiredVersionProperty();
			versionCondition = rootNode.property(versionProperty.getName())
				.isEqualTo(coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)));
		}
		else {
			versionCondition = Cypher.noCondition();
		}

		return match(rootNode).where(idDescription.asIdExpression().isEqualTo(parameter(Constants.NAME_OF_ID)))
			.and(versionCondition)
			.unwind(rootNode.labels())
			.as("label")
			.with(Cypher.name("label"))
			.where(Cypher.name("label").in(parameter(Constants.NAME_OF_STATIC_LABELS_PARAM)).not())
			.returning(collect(Cypher.name("label")).as(Constants.NAME_OF_LABELS))
			.build();
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
			return ongoingUpdate.returning(Cypher.count(rootNode)).build();
		}
		return ongoingUpdate.build();
	}

	public Condition createCompositePropertyCondition(GraphPropertyDescription idProperty, SymbolicName containerName,
			Expression actualParameter) {

		if (!idProperty.isComposite()) {
			return Cypher.property(containerName, idProperty.getPropertyName()).isEqualTo(actualParameter);
		}

		Neo4jPersistentProperty property = (Neo4jPersistentProperty) idProperty;

		Condition result = Cypher.noCondition();
		for (String key : Objects.requireNonNull(property.getOptionalConverter()).write(null).keys()) {
			Property expression = Cypher.property(containerName, key);
			result = result.and(expression.isEqualTo(actualParameter.property(key)));
		}
		return result;
	}

	public Statement prepareSaveOf(NodeDescription<?> nodeDescription,
			UnaryOperator<OngoingMatchAndUpdate> updateDecorator, boolean canUseElementId) {

		String primaryLabel = nodeDescription.getPrimaryLabel();
		List<String> additionalLabels = nodeDescription.getAdditionalLabels();

		Node rootNode = node(primaryLabel, additionalLabels)
			.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
		IdDescription idDescription = nodeDescription.getIdDescription();
		Assert.notNull(idDescription, "Cannot save individual nodes without an id attribute");
		Parameter<?> idParameter = parameter(Constants.NAME_OF_ID);

		Function<StatementBuilder.OngoingMatchAndUpdate, Statement> vectorProcedureCall = (bs) -> {
			if (((Neo4jPersistentEntity<?>) nodeDescription).hasVectorProperty()) {
				return bs.with(rootNode)
					.call("db.create.setNodeVectorProperty")
					.withArgs(rootNode.getRequiredSymbolicName(), parameter(Constants.NAME_OF_VECTOR_PROPERTY),
							parameter(Constants.NAME_OF_VECTOR_VALUE))
					.withoutResults()
					.returning(rootNode)
					.build();
			}
			return bs.returning(rootNode).build();
		};

		if (idDescription != null && !idDescription.isInternallyGeneratedId()) {
			GraphPropertyDescription idPropertyDescription = ((Neo4jPersistentEntity<?>) nodeDescription)
				.getRequiredIdProperty();

			if (((Neo4jPersistentEntity<?>) nodeDescription).hasVersionProperty()) {
				Property versionProperty = rootNode
					.property(((Neo4jPersistentEntity<?>) nodeDescription).getRequiredVersionProperty().getName());
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = vectorProcedureCall
					.apply(updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(createCompositePropertyCondition(idPropertyDescription,
								possibleExistingNode.getRequiredSymbolicName(), idParameter))
						.with(possibleExistingNode)
						.where(possibleExistingNode.isNull())
						.create(rootNode.withProperties(versionProperty, literalOf(0)))
						.with(rootNode)
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));

				Statement updateIfExists = vectorProcedureCall.apply(updateDecorator.apply(match(rootNode)
					.where(createCompositePropertyCondition(idPropertyDescription, rootNode.getRequiredSymbolicName(),
							idParameter))
					.and(versionProperty.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))) // Initial
																								// check
					.set(versionProperty.to(versionProperty.add(literalOf(1)))) // Acquire
																				// lock
					.with(rootNode)
					.where(versionProperty.isEqualTo(
							coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)).add(literalOf(1))))
					.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));
				return Cypher.union(createIfNew, updateIfExists);

			}
			else {
				String nameOfPossibleExistingNode = "hlp";
				Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

				Statement createIfNew = vectorProcedureCall
					.apply(updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(createCompositePropertyCondition(idPropertyDescription,
								possibleExistingNode.getRequiredSymbolicName(), idParameter))
						.with(possibleExistingNode)
						.where(possibleExistingNode.isNull())
						.create(rootNode)
						.with(rootNode)
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));

				Statement updateIfExists = vectorProcedureCall.apply(updateDecorator.apply(match(rootNode)
					.where(createCompositePropertyCondition(idPropertyDescription, rootNode.getRequiredSymbolicName(),
							idParameter))
					.with(rootNode)
					.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));
				return Cypher.union(createIfNew, updateIfExists);
			}
		}
		else {
			String nameOfPossibleExistingNode = "hlp";
			Node possibleExistingNode = node(primaryLabel, additionalLabels).named(nameOfPossibleExistingNode);

			var neo4jPersistentEntity = (Neo4jPersistentEntity<?>) nodeDescription;
			var nodeIdFunction = getNodeIdFunction(neo4jPersistentEntity, canUseElementId);

			if (neo4jPersistentEntity.hasVersionProperty()) {
				Property versionProperty = rootNode
					.property(neo4jPersistentEntity.getRequiredVersionProperty().getName());

				var createIfNew = vectorProcedureCall.apply(updateDecorator.apply(optionalMatch(possibleExistingNode)
					.where(nodeIdFunction.apply(possibleExistingNode).isEqualTo(idParameter))
					.with(possibleExistingNode)
					.where(possibleExistingNode.isNull())
					.create(rootNode.withProperties(versionProperty, literalOf(0)))
					.with(rootNode)
					.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));

				var updateIfExists = vectorProcedureCall.apply(updateDecorator
					.apply(match(rootNode).where(nodeIdFunction.apply(rootNode).isEqualTo(idParameter))
						.and(versionProperty.isEqualTo(parameter(Constants.NAME_OF_VERSION_PARAM))) // Initial
																									// check
						.set(versionProperty.to(versionProperty.add(literalOf(1)))) // Acquire
																					// lock
						.with(rootNode)
						.where(versionProperty.isEqualTo(
								coalesce(parameter(Constants.NAME_OF_VERSION_PARAM), literalOf(0)).add(literalOf(1))))
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));
				return Cypher.union(createIfNew, updateIfExists);
			}
			else {
				var createStatement = vectorProcedureCall
					.apply(updateDecorator.apply(optionalMatch(possibleExistingNode)
						.where(nodeIdFunction.apply(possibleExistingNode).isEqualTo(idParameter))
						.with(possibleExistingNode)
						.where(possibleExistingNode.isNull())
						.create(rootNode)
						.set(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));
				var updateStatement = vectorProcedureCall.apply(updateDecorator
					.apply(match(rootNode).where(nodeIdFunction.apply(rootNode).isEqualTo(idParameter))
						.mutate(rootNode, parameter(Constants.NAME_OF_PROPERTIES_PARAM))));

				return Cypher.union(createStatement, updateStatement);
			}

		}
	}

	@SuppressWarnings("deprecation")
	public Statement prepareSaveOfMultipleInstancesOf(NodeDescription<?> nodeDescription) {

		Assert.isTrue(!nodeDescription.isUsingInternalIds(),
				"Only entities that use external IDs can be saved in a batch");

		Node rootNode = node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
			.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
		IdDescription idDescription = nodeDescription.getIdDescription();

		@SuppressWarnings("ConstantConditions") // We now already that the node is using
												// internal ids, and as such, an
												// IdDescription must be present
		String nameOfIdProperty = Optional.ofNullable(idDescription)
			.flatMap(IdDescription::getOptionalGraphPropertyName)
			.orElseThrow(() -> new MappingException("External id does not correspond to a graph property"));

		List<Expression> expressions = new ArrayList<>();
		if (nodeDescription instanceof Neo4jPersistentEntity<?> entity && entity.isUsingDeprecatedInternalId()) {
			rootNode.internalId().as(Constants.NAME_OF_INTERNAL_ID);
		}
		expressions.add(this.elementIdOrIdFunction.apply(rootNode).as(Constants.NAME_OF_ELEMENT_ID));
		expressions.add(rootNode.property(nameOfIdProperty).as(Constants.NAME_OF_ID));

		String row = "entity";
		return Cypher.unwind(parameter(Constants.NAME_OF_ENTITY_LIST_PARAM))
			.as(row)
			.merge(rootNode.withProperties(nameOfIdProperty, Cypher.property(row, Constants.NAME_OF_ID)))
			.mutate(rootNode, Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM))
			.returning(expressions)
			.build();
	}

	public Statement prepareSaveOfRelationship(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, String dynamicRelationshipType, boolean canUseElementId) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds() ? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
					.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		Relationship relationshipFragment = (relationship.isOutgoing() ? startNode.relationshipTo(endNode, type)
				: startNode.relationshipFrom(endNode, type))
			.named(RELATIONSHIP_NAME);

		var startNodeIdFunction = getNodeIdFunction(neo4jPersistentEntity, canUseElementId);
		return match(startNode).where(startNodeIdFunction.apply(startNode).isEqualTo(idParameter))
			.match(endNode)
			.where(getEndNodeIdFunction((Neo4jPersistentEntity<?>) relationship.getTarget(), canUseElementId)
				.apply(endNode)
				.isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)))
			.merge(relationshipFragment)
			.returning(getReturnedIdExpressionsForRelationship(relationship, relationshipFragment))
			.build();
	}

	public Statement prepareSaveOfRelationships(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, @Nullable String dynamicRelationshipType, boolean canUseElementId) {

		final Node startNode = neo4jPersistentEntity.isUsingInternalIds() ? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
					.named(START_NODE_NAME);

		final Node endNode = anyNode(END_NODE_NAME);

		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();
		Relationship relationshipFragment = (relationship.isOutgoing() ? startNode.relationshipTo(endNode, type) : // CypherDSL
																													// is
																													// fine
																													// with
																													// a
																													// null
																													// type
				startNode.relationshipFrom(endNode, type))
			.named(RELATIONSHIP_NAME);

		String row = "relationship";
		Property idProperty = Cypher.property(row, Constants.FROM_ID_PARAMETER_NAME);
		return Cypher.unwind(parameter(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM))
			.as(row)
			.with(row)
			.match(startNode)
			.where(getNodeIdFunction(neo4jPersistentEntity, canUseElementId).apply(startNode).isEqualTo(idProperty))
			.match(endNode)
			.where(getEndNodeIdFunction((Neo4jPersistentEntity<?>) relationship.getTarget(), canUseElementId)
				.apply(endNode)
				.isEqualTo(Cypher.property(row, Constants.TO_ID_PARAMETER_NAME)))
			.merge(relationshipFragment)
			.returning(getReturnedIdExpressionsForRelationship(relationship, relationshipFragment))
			.build();
	}

	public Statement prepareSaveOfRelationshipWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, boolean isNew, @Nullable String dynamicRelationshipType,
			boolean canUseElementId, boolean matchOnly) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
			.named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		Parameter<?> relationshipProperties = parameter(Constants.NAME_OF_PROPERTIES_PARAM);
		String type = relationship.isDynamic() ? dynamicRelationshipType : relationship.getType();

		Relationship relationshipFragment = (relationship.isOutgoing() ? startNode.relationshipTo(endNode, type)
				: startNode.relationshipFrom(endNode, type))
			.named(RELATIONSHIP_NAME);

		var nodeIdFunction = getNodeIdFunction(neo4jPersistentEntity, canUseElementId);
		var relationshipIdFunction = getRelationshipIdFunction(relationship, canUseElementId);

		StatementBuilder.OngoingReadingWithWhere startAndEndNodeMatch = match(startNode)
			.where(nodeIdFunction.apply(startNode).isEqualTo(idParameter))
			.match(endNode)
			.where(getEndNodeIdFunction((Neo4jPersistentEntity<?>) relationship.getTarget(), canUseElementId)
				.apply(endNode)
				.isEqualTo(parameter(Constants.TO_ID_PARAMETER_NAME)));

		if (matchOnly) {
			return startAndEndNodeMatch.match(relationshipFragment)
				.returning(getReturnedIdExpressionsForRelationship(relationship, relationshipFragment))
				.build();
		}

		StatementBuilder.ExposesSet createOrMatch = isNew ? startAndEndNodeMatch.create(relationshipFragment)
				: startAndEndNodeMatch.match(relationshipFragment)
					.where(relationshipIdFunction.apply(relationshipFragment)
						.isEqualTo(Cypher.parameter(Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM)));
		return createOrMatch.mutate(RELATIONSHIP_NAME, relationshipProperties)
			.returning(getReturnedIdExpressionsForRelationship(relationship, relationshipFragment))
			.build();
	}

	public Statement prepareUpdateOfRelationshipsWithProperties(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationship, boolean isNew, boolean canUseElementId) {

		Assert.isTrue(relationship.hasRelationshipProperties(),
				"Properties required to create a relationship with properties");

		Node startNode = node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
			.named(START_NODE_NAME);
		Node endNode = anyNode(END_NODE_NAME);

		String type = relationship.getType();

		Relationship relationshipFragment = (relationship.isOutgoing() ? startNode.relationshipTo(endNode, type)
				: startNode.relationshipFrom(endNode, type))
			.named(RELATIONSHIP_NAME);

		String row = "row";
		Property relationshipProperties = Cypher.property(row, Constants.NAME_OF_PROPERTIES_PARAM);
		Property idProperty = Cypher.property(row, Constants.FROM_ID_PARAMETER_NAME);
		StatementBuilder.OrderableOngoingReadingAndWithWithoutWhere cypherUnwind = Cypher
			.unwind(parameter(Constants.NAME_OF_RELATIONSHIP_LIST_PARAM))
			.as(row)
			.with(row);

		var nodeIdFunction = getNodeIdFunction(neo4jPersistentEntity, canUseElementId);
		var relationshipIdFunction = getRelationshipIdFunction(relationship, canUseElementId);

		// we only need start and end node querying if we have to create a new
		// relationship...
		if (isNew) {
			return cypherUnwind.match(startNode)
				.where(nodeIdFunction.apply(startNode).isEqualTo(idProperty))
				.match(endNode)
				.where(getEndNodeIdFunction((Neo4jPersistentEntity<?>) relationship.getTarget(), canUseElementId)
					.apply(endNode)
					.isEqualTo(Cypher.property(row, Constants.TO_ID_PARAMETER_NAME)))
				.create(relationshipFragment)
				.mutate(RELATIONSHIP_NAME, relationshipProperties)
				.returning(getReturnedIdExpressionsForRelationship(relationship, relationshipFragment))
				.build();
		}

		// ... otherwise we can just fetch the existing relationship by known id
		return cypherUnwind.match(relationshipFragment)
			.where(relationshipIdFunction.apply(relationshipFragment)
				.isEqualTo(Cypher.property(row, Constants.NAME_OF_KNOWN_RELATIONSHIP_PARAM)))
			.mutate(RELATIONSHIP_NAME, relationshipProperties)
			.build();
	}

	private List<Expression> getReturnedIdExpressionsForRelationship(RelationshipDescription relationship,
			Relationship relationshipFragment) {
		List<Expression> result = new ArrayList<>();
		if (relationship.hasRelationshipProperties()
				&& relationship.getRelationshipPropertiesEntity() instanceof Neo4jPersistentEntity<?> entity
				&& entity.isUsingDeprecatedInternalId()) {
			result.add(relId(relationshipFragment).as(Constants.NAME_OF_INTERNAL_ID));
		}
		result.add(this.elementIdOrIdFunction.apply(relationshipFragment).as(Constants.NAME_OF_ELEMENT_ID));
		return result;
	}

	public Statement prepareDeleteOf(Neo4jPersistentEntity<?> neo4jPersistentEntity,
			RelationshipDescription relationshipDescription, boolean canUseElementId) {
		final Node startNode = neo4jPersistentEntity.isUsingInternalIds() ? anyNode(START_NODE_NAME)
				: node(neo4jPersistentEntity.getPrimaryLabel(), neo4jPersistentEntity.getAdditionalLabels())
					.named(START_NODE_NAME);

		NodeDescription<?> target = relationshipDescription.getTarget();
		Node endNode = node(target.getPrimaryLabel(), target.getAdditionalLabels());

		boolean outgoing = relationshipDescription.isOutgoing();

		String relationshipType = relationshipDescription.isDynamic() ? null : relationshipDescription.getType();

		String relationshipToRemoveName = "rel";
		Relationship relationship = outgoing
				? startNode.relationshipTo(endNode, relationshipType).named(relationshipToRemoveName)
				: startNode.relationshipFrom(endNode, relationshipType).named(relationshipToRemoveName);

		Parameter<?> idParameter = parameter(Constants.FROM_ID_PARAMETER_NAME);
		return match(relationship)
			.where(getNodeIdFunction(neo4jPersistentEntity, canUseElementId).apply(startNode).isEqualTo(idParameter))
			.and(getRelationshipIdFunction(relationshipDescription, canUseElementId).apply(relationship)
				.in(Cypher.parameter(Constants.NAME_OF_KNOWN_RELATIONSHIPS_PARAM))
				.not())
			.delete(relationship.getRequiredSymbolicName())
			.build();
	}

	public Collection<Expression> createReturnStatementForExists(Neo4jPersistentEntity<?> nodeDescription) {

		return Collections.singleton(Cypher.count(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)));
	}

	/**
	 * Used for create statements from the (Reactive)Neo4jTemplate. This shouldn't be used
	 * for any find operations.
	 * @param nodeDescription persistentEntity
	 * @return return expression for entity
	 */
	public Collection<Expression> createReturnStatementForMatch(Neo4jPersistentEntity<?> nodeDescription) {
		return createReturnStatementForMatch(nodeDescription, PropertyFilter.NO_FILTER);
	}

	/**
	 * Creates an order by fragment, assuming the node to match is named `n`.
	 * @param sort the {@link Sort sort} that should be turned into a valid Cypher
	 * {@code ORDER}-clause
	 * @return an optional order clause. Will be {@literal null} on sorts that are
	 * {@literal null} or unsorted
	 */
	@Nullable public String createOrderByFragment(@Nullable Sort sort) {

		if (sort == null || sort.isUnsorted()) {
			return null;
		}
		Statement statement = match(anyNode()).returning("n").orderBy(sort.stream().map(order -> {
			String property = order.getProperty().trim();
			Expression expression;
			if (LOOKS_LIKE_A_FUNCTION.matcher(property).matches()) {
				expression = Cypher.raw(property);
			}
			else if (property.contains(".")) {
				int firstDot = property.indexOf('.');
				String tail = property.substring(firstDot + 1);
				if (tail.isEmpty() || property.lastIndexOf(".") != firstDot) {
					if (tail.trim().matches("`.+`")) {
						tail = tail.replaceFirst("`(.+)`", "$1");
					}
					else {
						throw new IllegalArgumentException(String.format(
								"Cannot handle order property `%s`, it must be a simple property or one-hop path",
								property));
					}
				}
				expression = Cypher.property(property.substring(0, firstDot), tail);
			}
			else {
				try {
					Assert.isTrue(SourceVersion.isIdentifier(property), "Name must be a valid identifier.");
					expression = Cypher.name(property);
				}
				catch (IllegalArgumentException ex) {
					var msg = Optional.ofNullable(ex.getMessage()).orElse("");
					if (msg.endsWith(".")) {
						throw new IllegalArgumentException(msg.substring(0, msg.length() - 1));
					}
					throw ex;
				}
			}
			if (order.isIgnoreCase()) {
				expression = Cypher.toLower(expression);
			}
			return order.isAscending() ? expression.ascending() : expression.descending();
		}).toArray(SortItem[]::new)).build();

		String renderedStatement = Renderer.getRenderer(Configuration.defaultConfig()).render(statement);
		return renderedStatement.substring(renderedStatement.indexOf("ORDER BY")).trim();
	}

	/**
	 * Creates a return statement for an ongoing match.
	 * @param nodeDescription description of the root node
	 * @param includeField a predicate derived from the set of included properties. This
	 * is only relevant in various forms of projections which allow to exclude one or more
	 * fields
	 * @param additionalExpressions any additional expressions to add to the return
	 * statement
	 * @return an expression to be returned by a Cypher statement
	 */
	public Collection<Expression> createReturnStatementForMatch(Neo4jPersistentEntity<?> nodeDescription,
			Predicate<PropertyFilter.RelaxedPropertyPath> includeField, Expression... additionalExpressions) {
		List<RelationshipDescription> processedRelationships = new ArrayList<>();

		if (nodeDescription.containsPossibleCircles(includeField)) {
			return createGenericReturnStatement(additionalExpressions);
		}
		else {
			List<Expression> returnContent = new ArrayList<>();
			returnContent.add(projectPropertiesAndRelationships(
					PropertyFilter.RelaxedPropertyPath.withRootType(nodeDescription.getUnderlyingClass()),
					nodeDescription, Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription), includeField,
					processedRelationships));
			Collections.addAll(returnContent, additionalExpressions);
			return returnContent;
		}
	}

	public Collection<Expression> createGenericReturnStatement(Expression... additionalExpressions) {
		List<Expression> returnExpressions = new ArrayList<>();
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES));
		returnExpressions.add(Cypher.name(Constants.NAME_OF_SYNTHESIZED_RELATIONS));
		returnExpressions.addAll(Arrays.asList(additionalExpressions));
		return returnExpressions;
	}

	public StatementBuilder.OngoingReading prepareFindOf(NodeDescription<?> nodeDescription,
			List<PatternElement> initialMatchOn, @Nullable Condition condition) {
		var rootNode = createRootNode(nodeDescription);
		return prepareMatchOfRootNode(rootNode, initialMatchOn).where(conditionOrNoCondition(condition));
	}

	private MapProjection projectPropertiesAndRelationships(PropertyFilter.RelaxedPropertyPath parentPath,
			Neo4jPersistentEntity<?> nodeDescription, SymbolicName nodeName,
			Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties,
			@Nullable List<RelationshipDescription> processedRelationships) {

		Collection<RelationshipDescription> relationships = ((DefaultNeo4jPersistentEntity<?>) nodeDescription)
			.getRelationshipsInHierarchy(includedProperties, parentPath);
		relationships.removeIf(r -> !includedProperties.test(parentPath.append(r.getFieldName())));

		List<Object> propertiesProjection = projectNodeProperties(parentPath, nodeDescription, nodeName,
				includedProperties);
		List<Object> contentOfProjection = new ArrayList<>(propertiesProjection);

		contentOfProjection.addAll(generateListsFor(parentPath, nodeDescription, relationships, nodeName,
				includedProperties, processedRelationships));
		return Cypher.anyNode(nodeName).project(contentOfProjection);
	}

	/**
	 * Creates a list of objects that represents a very basic of
	 * {@code MapEntry<String, Object>} with the exception that this list can also contain
	 * two "keys" in a row. The {@link MapProjection} will take care to handle them as
	 * self-reflecting fields. Example with self-reflection and explicit value: {@code n
	 * {.id, name: n.name}}.
	 * @param parentPath parent path
	 * @param nodeDescription the description to work on
	 * @param nodeName the name of the node to project from
	 * @param includeField a predicate to decide on including fields or not
	 * @return a list of projected properties
	 */
	@SuppressWarnings("deprecation")
	private List<Object> projectNodeProperties(PropertyFilter.RelaxedPropertyPath parentPath,
			NodeDescription<?> nodeDescription, SymbolicName nodeName,
			Predicate<PropertyFilter.RelaxedPropertyPath> includeField) {

		List<Object> nodePropertiesProjection = new ArrayList<>();
		Node node = anyNode(nodeName);

		boolean hasCompositeProperties = false;
		for (GraphPropertyDescription graphProperty : nodeDescription.getGraphPropertiesInHierarchy()) {

			Neo4jPersistentProperty property = (Neo4jPersistentProperty) graphProperty;
			hasCompositeProperties = hasCompositeProperties || property.isComposite();

			if (property.isDynamicLabels() || property.isComposite()) {
				continue;
			}
			PropertyFilter.RelaxedPropertyPath from = parentPath.append(property.getFieldName());

			if (!includeField.test(from)) {
				continue;
			}

			// ignore internally generated id fields
			if (graphProperty.isIdProperty() && (nodeDescription.getIdDescription() != null
					&& nodeDescription.getIdDescription().isInternallyGeneratedId())) {
				continue;
			}
			nodePropertiesProjection.add(graphProperty.getPropertyName());
		}

		if (hasCompositeProperties || nodeDescription.describesInterface()) {
			nodePropertiesProjection.add(Constants.NAME_OF_ALL_PROPERTIES);
			nodePropertiesProjection.add(node.project(Cypher.asterisk()));
		}

		nodePropertiesProjection.add(Constants.NAME_OF_LABELS);
		nodePropertiesProjection.add(Cypher.labels(node));
		if (nodeDescription instanceof Neo4jPersistentEntity<?> entity && entity.isUsingDeprecatedInternalId()) {
			nodePropertiesProjection.add(Constants.NAME_OF_INTERNAL_ID);
			nodePropertiesProjection.add(node.internalId());
		}
		nodePropertiesProjection.add(Constants.NAME_OF_ELEMENT_ID);
		nodePropertiesProjection.add(this.elementIdOrIdFunction.apply(node));
		return nodePropertiesProjection;
	}

	private List<Object> generateListsFor(PropertyFilter.RelaxedPropertyPath parentPath,
			Neo4jPersistentEntity<?> nodeDescription, Collection<RelationshipDescription> relationships,
			SymbolicName nodeName, Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties,
			@Nullable List<RelationshipDescription> processedRelationships) {

		List<Object> mapProjectionLists = new ArrayList<>();
		List<RelationshipDescription> processed = Objects.requireNonNullElseGet(processedRelationships, ArrayList::new);

		for (RelationshipDescription relationshipDescription : relationships) {

			String fieldName = relationshipDescription.getFieldName();

			// if we already processed the other way before, do not try to jump in the
			// infinite loop
			// unless it is a root node relationship
			if (relationshipDescription.hasRelationshipObverse()
					&& processed.contains(relationshipDescription.getRelationshipObverse())) {
				continue;
			}

			generateListFor(parentPath, nodeDescription, relationshipDescription, nodeName, processed, fieldName,
					mapProjectionLists, includedProperties);
		}

		return mapProjectionLists;
	}

	private void generateListFor(PropertyFilter.RelaxedPropertyPath parentPath,
			Neo4jPersistentEntity<?> nodeDescription, RelationshipDescription relationshipDescription,
			SymbolicName nodeName, List<RelationshipDescription> processedRelationships, String fieldName,
			List<Object> mapProjectionLists, Predicate<PropertyFilter.RelaxedPropertyPath> includedProperties) {

		String relationshipType = relationshipDescription.getType();
		String relationshipTargetName = relationshipDescription.generateRelatedNodesCollectionName(nodeDescription);
		String sourcePrimaryLabel = relationshipDescription.getSource().getMostAbstractParentLabel(nodeDescription);
		String targetPrimaryLabel = relationshipDescription.getTarget().getPrimaryLabel();
		List<String> targetAdditionalLabels = relationshipDescription.getTarget().getAdditionalLabels();
		String relationshipSymbolicName = sourcePrimaryLabel + RelationshipDescription.NAME_OF_RELATIONSHIP
				+ targetPrimaryLabel;

		Node startNode = anyNode(nodeName);
		SymbolicName relationshipFieldName = nodeName.concat("_" + fieldName);
		Node endNode = node(targetPrimaryLabel, targetAdditionalLabels).named(relationshipFieldName);
		Neo4jPersistentEntity<?> endNodeDescription = (Neo4jPersistentEntity<?>) relationshipDescription.getTarget();

		processedRelationships.add(relationshipDescription);
		PropertyFilter.RelaxedPropertyPath newParentPath;
		newParentPath = parentPath.append(relationshipDescription.getFieldName());
		if (relationshipDescription.hasRelationshipProperties()) {
			var persistentProperty = ((Neo4jPersistentEntity<?>) relationshipDescription
				.getRequiredRelationshipPropertiesEntity()).getPersistentProperty(TargetNode.class);
			if (persistentProperty != null) {
				newParentPath = newParentPath.append(persistentProperty.getFieldName());
			}
		}

		if (relationshipDescription.isDynamic()) {
			Relationship relationship = relationshipDescription.isOutgoing() ? startNode.relationshipTo(endNode)
					: startNode.relationshipFrom(endNode);
			relationship = relationship.named(relationshipTargetName);

			MapProjection mapProjection = projectPropertiesAndRelationships(newParentPath, endNodeDescription,
					relationshipFieldName, includedProperties, new ArrayList<>(processedRelationships));

			if (relationshipDescription.hasRelationshipProperties()) {
				relationship = relationship.named(relationshipSymbolicName);
				mapProjection = mapProjection.and(relationship);
			}

			addMapProjection(relationshipTargetName,
					listBasedOn(relationship).returning(mapProjection
						.and(RelationshipDescription.NAME_OF_RELATIONSHIP_TYPE, Cypher.type(relationship))),
					mapProjectionLists);

		}
		else {
			Relationship relationship = relationshipDescription.isOutgoing()
					? startNode.relationshipTo(endNode, relationshipType)
					: startNode.relationshipFrom(endNode, relationshipType);

			MapProjection mapProjection = projectPropertiesAndRelationships(newParentPath, endNodeDescription,
					relationshipFieldName, includedProperties, new ArrayList<>(processedRelationships));

			if (relationshipDescription.hasRelationshipProperties()) {
				relationship = relationship.named(relationshipSymbolicName);
				mapProjection = mapProjection.and(relationship);
			}

			addMapProjection(relationshipTargetName, listBasedOn(relationship).returning(mapProjection),
					mapProjectionLists);
		}
	}

	private void addMapProjection(String name, Object projection, List<Object> projectionList) {
		projectionList.add(name);
		projectionList.add(projection);
	}

}
