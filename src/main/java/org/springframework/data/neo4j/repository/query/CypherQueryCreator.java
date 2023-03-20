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
package org.springframework.data.neo4j.repository.query;

import static org.neo4j.cypherdsl.core.Functions.point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Conditions;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.ExposesRelationships;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.Predicates;
import org.neo4j.cypherdsl.core.Property;
import org.neo4j.cypherdsl.core.RelationshipPattern;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.driver.types.Point;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.CypherGenerator;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.mapping.NodeDescription;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;
import org.springframework.data.neo4j.core.mapping.RelationshipDescription;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * A Cypher-DSL based implementation of the {@link AbstractQueryCreator} that eventually creates Cypher queries as
 * strings to be used by a Neo4j client or driver as statement template.
 * <p/>
 * This class is not thread safe and not reusable.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class CypherQueryCreator extends AbstractQueryCreator<QueryFragmentsAndParameters, Condition> {

	private final Neo4jMappingContext mappingContext;
	private final QueryMethod queryMethod;

	private final Class<?> domainType;
	private final NodeDescription<?> nodeDescription;

	private final Neo4jQueryType queryType;
	private final boolean isDistinct;

	private final Iterator<Neo4jQueryMethod.Neo4jParameter> formalParameters;
	private final Queue<Parameter> lastParameter = new LinkedList<>();

	private final Supplier<String> indexSupplier = new IndexSupplier();

	private final BiFunction<Object, Neo4jPersistentPropertyConverter<?>, Object> parameterConversion;
	private final List<Parameter> boundedParameters = new ArrayList<>();

	private final Pageable pagingParameter;

	private final ScrollPosition scrollPosition;

	/**
	 * Stores the number of max results, if the {@link PartTree tree} is limiting.
	 */
	private final Number maxResults;

	/**
	 * Sort items may already be needed for some parts, i.e. of type NEAR.
	 */
	private final List<SortItem> sortItems = new ArrayList<>();

	private final Collection<PropertyFilter.ProjectedPath> includedProperties;

	private final List<PropertyPathWrapper> propertyPathWrappers;

	private final boolean keysetRequiresSort;

	/**
	 * Can be used to modify the limit of a paged or sliced query.
	 */
	private final UnaryOperator<Integer> limitModifier;

	CypherQueryCreator(Neo4jMappingContext mappingContext, QueryMethod queryMethod, Class<?> domainType, Neo4jQueryType queryType, PartTree tree,
			Neo4jParameterAccessor actualParameters, Collection<PropertyFilter.ProjectedPath> includedProperties,
			BiFunction<Object, Neo4jPersistentPropertyConverter<?>, Object> parameterConversion,
			UnaryOperator<Integer> limitModifier) {

		super(tree, actualParameters);
		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;

		this.domainType = domainType;
		this.nodeDescription = this.mappingContext.getRequiredNodeDescription(this.domainType);

		this.queryType = queryType;
		this.isDistinct = tree.isDistinct();

		this.formalParameters = actualParameters.getParameters().iterator();
		this.maxResults = tree.isLimiting() ? tree.getMaxResults() : null;

		this.includedProperties = includedProperties;
		this.parameterConversion = parameterConversion;

		this.pagingParameter = actualParameters.getPageable();
		this.scrollPosition = actualParameters.getScrollPosition();
		this.limitModifier = limitModifier;

		AtomicInteger symbolicNameIndex = new AtomicInteger();

		propertyPathWrappers = tree.getParts().stream()
				.map(part -> new PropertyPathWrapper(symbolicNameIndex.getAndIncrement(),
						mappingContext.getPersistentPropertyPath(part.getProperty())))
				.collect(Collectors.toList());

		this.keysetRequiresSort = queryMethod.isScrollQuery() && actualParameters.getScrollPosition() instanceof KeysetScrollPosition;
	}

	private class PropertyPathWrapper {
		private static final String NAME_OF_RELATED_FILTER_ENTITY = "m";
		private static final String NAME_OF_RELATED_FILTER_RELATIONSHIP = "r";

		private final int index;
		private final PersistentPropertyPath<?> propertyPath;

		PropertyPathWrapper(int index, PersistentPropertyPath<?> propertyPath) {
			this.index = index;
			this.propertyPath = propertyPath;
		}

		public PersistentPropertyPath<?> getPropertyPath() {
			return propertyPath;
		}

		private String getNodeName() {
			return NAME_OF_RELATED_FILTER_ENTITY + "_" + index;
		}

		private String getRelationshipName() {
			return NAME_OF_RELATED_FILTER_RELATIONSHIP + "_" + index;
		}

		private ExposesRelationships<?> createRelationshipChain(ExposesRelationships<?> existingRelationshipChain) {

			ExposesRelationships<?> cypherRelationship = existingRelationshipChain;
			int cnt = 0;
			for (PersistentProperty<?> persistentProperty : propertyPath) {

				if (persistentProperty.isAssociation() && persistentProperty.isAnnotationPresent(TargetNode.class)) {
					break;
				}

				RelationshipDescription relationshipDescription = (RelationshipDescription) persistentProperty.getAssociation();

				if (relationshipDescription == null) {
					break;
				}

				NodeDescription<?> relationshipPropertiesEntity = relationshipDescription.getRelationshipPropertiesEntity();
				boolean hasTargetNode = hasTargetNode(relationshipPropertiesEntity);

				NodeDescription<?> targetEntity = relationshipDescription.getTarget();
				Node relatedNode = Cypher.node(targetEntity.getPrimaryLabel(), targetEntity.getAdditionalLabels());

				// length - 1 = last index
				// length - 2 = property on last node
				// length - 3 = last node itself
				boolean lastNode = cnt++ > (propertyPath.getLength() - 3);
				if (lastNode || hasTargetNode) {
					relatedNode = relatedNode.named(getNodeName());
				}

				cypherRelationship = switch (relationshipDescription.getDirection()) {
					case OUTGOING -> cypherRelationship
							.relationshipTo(relatedNode, relationshipDescription.getType());
					case INCOMING -> cypherRelationship
							.relationshipFrom(relatedNode, relationshipDescription.getType());
				};

				if (lastNode || hasTargetNode) {
					cypherRelationship = ((RelationshipPattern) cypherRelationship).named(getRelationshipName());
				}
			}

			return cypherRelationship;
		}

		private boolean hasTargetNode(@Nullable NodeDescription<?> relationshipPropertiesEntity) {
			return relationshipPropertiesEntity != null
					&& ((Neo4jPersistentEntity<?>) relationshipPropertiesEntity)
						.getPersistentProperty(TargetNode.class) != null;
		}

		// if there is no direct property access, the list size is greater than 1 and as a consequence has to contain
		// relationships.
		private boolean hasRelationships() {
			return this.propertyPath.getLength() > 1;
		}
	}

	@Override
	protected Condition create(Part part, Iterator<Object> actualParameters) {
		return createImpl(part, actualParameters);
	}

	@Override
	protected Condition and(Part part, Condition base, Iterator<Object> actualParameters) {

		if (base == null) {
			return create(part, actualParameters);
		}

		return base.and(createImpl(part, actualParameters));
	}

	@Override
	protected Condition or(Condition base, Condition condition) {
		return base.or(condition);
	}

	@Override
	protected QueryFragmentsAndParameters complete(@Nullable Condition condition, Sort sort) {

		Map<String, Object> convertedParameters = this.boundedParameters.stream()
				.peek(p -> Neo4jQuerySupport.logParameterIfNull(p.nameOrIndex, p.value))
				.collect(Collectors.toMap(p -> p.nameOrIndex, p -> parameterConversion.apply(p.value, p.conversionOverride)));

		QueryFragments queryFragments = createQueryFragments(condition, sort);

		var theSort = pagingParameter.getSort().and(sort);
		if (keysetRequiresSort && theSort.isUnsorted()) {
			throw new UnsupportedOperationException("Unsorted keyset based scrolling is not supported.");
		}
		return new QueryFragmentsAndParameters(nodeDescription, queryFragments, convertedParameters, theSort);
	}

	@NonNull
	private QueryFragments createQueryFragments(@Nullable Condition condition, Sort sort) {
		QueryFragments queryFragments = new QueryFragments();

		// all the ways we could query for
		Node startNode = Cypher.node(nodeDescription.getPrimaryLabel(), nodeDescription.getAdditionalLabels())
				.named(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));

		Condition conditionFragment = Optional.ofNullable(condition).orElseGet(Conditions::noCondition);
		List<PatternElement> relationshipChain = new ArrayList<>();

		for (PropertyPathWrapper possiblePathWithRelationship : propertyPathWrappers) {
			if (possiblePathWithRelationship.hasRelationships()) {
				relationshipChain.add((RelationshipPattern) possiblePathWithRelationship.createRelationshipChain(startNode));
			}
		}

		if (!relationshipChain.isEmpty()) {
			queryFragments.setMatchOn(relationshipChain);
		} else {
			queryFragments.addMatchOn(startNode);
		}
		// end of initial filter query creation

		if (queryType == Neo4jQueryType.COUNT) {
			queryFragments.setReturnExpression(Functions.count(Cypher.asterisk()), true);
		} else if (queryType == Neo4jQueryType.EXISTS) {
			queryFragments.setReturnExpression(Functions.count(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)).gt(Cypher.literalOf(0)), true);
		} else if (queryType == Neo4jQueryType.DELETE) {
			queryFragments.setDeleteExpression(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
			queryFragments.setReturnExpression(Functions.count(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)), true);
		} else {

			var theSort = pagingParameter.getSort().and(sort);

			if (pagingParameter.isUnpaged() && scrollPosition == null && maxResults != null) {
				queryFragments.setLimit(limitModifier.apply(maxResults.intValue()));
			} else if (scrollPosition instanceof KeysetScrollPosition keysetScrollPosition) {

				Neo4jPersistentEntity<?> entity = (Neo4jPersistentEntity<?>) nodeDescription;
				// Enforce sorting by something that is hopefully stable comparable (looking at Neo4j's id() with tears in my eyes).
				theSort = theSort.and(Sort.by(entity.getRequiredIdProperty().getName()).ascending());

				queryFragments.setLimit(limitModifier.apply(maxResults.intValue()));
				if (!keysetScrollPosition.isInitial()) {
					conditionFragment = conditionFragment.and(CypherAdapterUtils.combineKeysetIntoCondition(entity, keysetScrollPosition, theSort));
				}

				queryFragments.setRequiresReverseSort(keysetScrollPosition.getDirection() == KeysetScrollPosition.Direction.Backward);
			} else if (scrollPosition instanceof OffsetScrollPosition offsetScrollPosition) {
				queryFragments.setSkip(offsetScrollPosition.getOffset());
				queryFragments.setLimit(limitModifier.apply(pagingParameter.isUnpaged() ? maxResults.intValue() : pagingParameter.getPageSize()));
			}

			queryFragments.setReturnBasedOn(nodeDescription, includedProperties, isDistinct);
			queryFragments.setOrderBy(Stream
					.concat(sortItems.stream(),
							theSort.stream().map(CypherAdapterUtils.sortAdapterFor(nodeDescription)))
					.collect(Collectors.toList()));
		}

		// closing action: add the condition and path match
		queryFragments.setCondition(conditionFragment);

		return queryFragments;
	}

	private Condition createImpl(Part part, Iterator<Object> actualParameters) {

		PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext.getPersistentPropertyPath(part.getProperty());
		Neo4jPersistentProperty property = path.getRequiredLeafProperty();

		boolean ignoreCase = ignoreCase(part);

		if (property.isComposite()) {

			Condition compositePropertyCondition = CypherGenerator.INSTANCE.createCompositePropertyCondition(
					property,
					Cypher.name(getContainerName(path, (Neo4jPersistentEntity<?>) property.getOwner())),
					toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			if (part.getType() == Part.Type.NEGATING_SIMPLE_PROPERTY) {
				compositePropertyCondition = Conditions.not(compositePropertyCondition);
			}
			return compositePropertyCondition;
		}

		return switch (part.getType()) {
			case AFTER, GREATER_THAN -> toCypherProperty(path, ignoreCase)
					.gt(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case BEFORE, LESS_THAN -> toCypherProperty(path, ignoreCase)
					.lt(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case BETWEEN -> betweenCondition(path, actualParameters, ignoreCase);
			case CONTAINING -> 	containingCondition(path, property, actualParameters, ignoreCase);
			case ENDING_WITH -> toCypherProperty(path, ignoreCase)
					.endsWith(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case EXISTS -> Predicates.exists(toCypherProperty(property));
			case FALSE -> toCypherProperty(path, ignoreCase).isFalse();
			case GREATER_THAN_EQUAL -> toCypherProperty(path, ignoreCase)
					.gte(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case IN -> toCypherProperty(path, ignoreCase)
					.in(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case IS_EMPTY -> toCypherProperty(path, ignoreCase).isEmpty();
			case IS_NOT_EMPTY -> toCypherProperty(path, ignoreCase).isEmpty().not();
			case IS_NOT_NULL -> toCypherProperty(path, ignoreCase).isNotNull();
			case IS_NULL -> toCypherProperty(path, ignoreCase).isNull();
			case LESS_THAN_EQUAL -> toCypherProperty(path, ignoreCase)
					.lte(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case LIKE -> likeCondition(path, nextRequiredParameter(actualParameters, property).nameOrIndex, ignoreCase);
			case NEAR -> createNearCondition(path, actualParameters);
			case NEGATING_SIMPLE_PROPERTY -> toCypherProperty(path, ignoreCase)
					.isNotEqualTo(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case NOT_CONTAINING -> containingCondition(path, property, actualParameters, ignoreCase).not();
			case NOT_IN -> toCypherProperty(path, ignoreCase)
					.in(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase)).not();
			case NOT_LIKE -> likeCondition(path, nextRequiredParameter(actualParameters, property).nameOrIndex,
					ignoreCase).not();
			case SIMPLE_PROPERTY -> toCypherProperty(path, ignoreCase)
					.isEqualTo(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case STARTING_WITH -> toCypherProperty(path, ignoreCase)
					.startsWith(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case REGEX -> toCypherProperty(path, ignoreCase)
					.matches(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
			case TRUE -> toCypherProperty(path, ignoreCase).isTrue();
			case WITHIN -> createWithinCondition(path, actualParameters);
		};
	}

	private Condition containingCondition(PersistentPropertyPath<Neo4jPersistentProperty> path,
			Neo4jPersistentProperty property, Iterator<Object> actualParameters, boolean ignoreCase) {

		Expression cypherProperty = toCypherProperty(path, ignoreCase);

		if (property.isDynamicLabels()) {
			Neo4jPersistentProperty leafProperty = path.getRequiredLeafProperty();
			Neo4jPersistentEntity<?> owner = (Neo4jPersistentEntity<?>) leafProperty.getOwner();
			String containerName = getContainerName(path, owner);
			return toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase)
					.in(Functions.labels(Cypher.anyNode(containerName)));
		}
		if (property.isCollectionLike()) {
			return toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase).in(cypherProperty);
		}
		return cypherProperty
				.contains(toCypherParameter(nextRequiredParameter(actualParameters, property), ignoreCase));
	}

	/**
	 * Checks whether to ignore the case for some operations. {@link PartTreeNeo4jQuery} will already have
	 * validated which properties can be made case insensitive given a certain keyword.
	 *
	 * @param part query part to get checked if case should get ignored
	 * @return should the case get ignored
	 */
	boolean ignoreCase(Part part) {

		return switch (part.shouldIgnoreCase()) {
			case ALWAYS -> true;
			case WHEN_POSSIBLE -> PartValidator.canIgnoreCase(part);
			case NEVER -> false;
		};
	}

	private Condition likeCondition(PersistentPropertyPath<Neo4jPersistentProperty> path, String parameterName,
			boolean ignoreCase) {
		String regexOptions = ignoreCase ? "(?i)" : "";
		return toCypherProperty(path, false).matches(
				Cypher.literalOf(regexOptions + ".*").concat(Cypher.parameter(parameterName)).concat(Cypher.literalOf(".*")));
	}

	private Condition betweenCondition(PersistentPropertyPath<Neo4jPersistentProperty> path, Iterator<Object> actualParameters,
			boolean ignoreCase) {

		Neo4jPersistentProperty leafProperty = path.getLeafProperty();
		Parameter lowerBoundOrRange = nextRequiredParameter(actualParameters, leafProperty);

		Expression property = toCypherProperty(path, ignoreCase);
		if (lowerBoundOrRange.value instanceof Range) {
			return createRangeConditionForProperty(property, lowerBoundOrRange);
		} else {
			Parameter upperBound = nextRequiredParameter(actualParameters, leafProperty);
			return property.gte(toCypherParameter(lowerBoundOrRange, ignoreCase))
					.and(property.lte(toCypherParameter(upperBound, ignoreCase)));
		}
	}

	private Condition createNearCondition(PersistentPropertyPath<Neo4jPersistentProperty> path, Iterator<Object> actualParameters) {

		Neo4jPersistentProperty leafProperty = path.getRequiredLeafProperty();
		Parameter p1 = nextRequiredParameter(actualParameters, leafProperty);
		Optional<Parameter> p2 = nextOptionalParameter(actualParameters, leafProperty);

		Expression referencePoint;

		Optional<Parameter> other;
		if (p1.value instanceof Point) {
			referencePoint = toCypherParameter(p1, false);
			other = p2;
		} else if (p2.isPresent() && p2.get().value instanceof Point) {
			referencePoint = toCypherParameter(p2.get(), false);
			other = Optional.of(p1);
		} else {
			throw new IllegalArgumentException(
					String.format("The NEAR operation requires a reference point of type %s", Point.class));
		}

		Expression distanceFunction = Functions.distance(toCypherProperty(path, false), referencePoint);

		if (other.filter(p -> p.hasValueOfType(Distance.class)).isPresent()) {
			return distanceFunction.lte(toCypherParameter(other.get(), false));
		} else if (other.filter(p -> p.hasValueOfType(Range.class)).isPresent()) {
			return createRangeConditionForProperty(distanceFunction, other.get());
		} else {
			// We only have a point toCypherParameter, that's ok, but we have to put back the last toCypherParameter when it
			// wasn't null
			other.ifPresent(this.lastParameter::offer);

			// Also, we cannot filter, but need to sort in the end.
			this.sortItems.add(distanceFunction.ascending());
			return Conditions.noCondition();
		}
	}

	private Condition createWithinCondition(PersistentPropertyPath<Neo4jPersistentProperty> path, Iterator<Object> actualParameters) {

		Neo4jPersistentProperty leafProperty = path.getRequiredLeafProperty();
		Parameter area = nextRequiredParameter(actualParameters, leafProperty);
		if (area.hasValueOfType(Circle.class)) {
			// We don't know the CRS of the point, so we assume the same as the reference toCypherProperty
			Expression referencePoint = point(Cypher.mapOf("x", createCypherParameter(area.nameOrIndex + ".x", false), "y",
					createCypherParameter(area.nameOrIndex + ".y", false), "srid",
					Cypher.property(toCypherProperty(path, false), "srid")));
			Expression distanceFunction = Functions.distance(toCypherProperty(path, false), referencePoint);
			return distanceFunction.lte(createCypherParameter(area.nameOrIndex + ".radius", false));
		} else if (area.hasValueOfType(BoundingBox.class) || area.hasValueOfType(Box.class)) {
			Expression llx = createCypherParameter(area.nameOrIndex + ".llx", false);
			Expression lly = createCypherParameter(area.nameOrIndex + ".lly", false);
			Expression urx = createCypherParameter(area.nameOrIndex + ".urx", false);
			Expression ury = createCypherParameter(area.nameOrIndex + ".ury", false);

			Expression x = Cypher.property(toCypherProperty(path, false), "x");
			Expression y = Cypher.property(toCypherProperty(path, false), "y");

			return llx.lte(x).and(x.lte(urx)).and(lly.lte(y)).and(y.lte(ury));
		} else if (area.hasValueOfType(Polygon.class)) {
			throw new IllegalArgumentException(String.format(
					"The WITHIN operation does not support a %s, you might want to pass a bounding box instead: %s.of(polygon)",
					Polygon.class, BoundingBox.class));
		} else {
			throw new IllegalArgumentException(
					String.format("The WITHIN operation requires an area of type %s or %s", Circle.class, Box.class));
		}
	}

	/**
	 * @param property property for which the range should get checked
	 * @param rangeParameter parameter that expresses the range
	 * @return The equivalent of a A BETWEEN B AND C expression for a given range.
	 */
	private Condition createRangeConditionForProperty(Expression property, Parameter rangeParameter) {

		Range range = (Range) rangeParameter.value;
		Condition betweenCondition = Conditions.noCondition();
		if (range.getLowerBound().isBounded()) {
			Expression parameterPlaceholder = createCypherParameter(rangeParameter.nameOrIndex + ".lb", false);
			betweenCondition = betweenCondition.and(
					range.getLowerBound().isInclusive() ? property.gte(parameterPlaceholder) : property.gt(parameterPlaceholder));
		}

		if (range.getUpperBound().isBounded()) {
			Expression parameterPlaceholder = createCypherParameter(rangeParameter.nameOrIndex + ".ub", false);
			betweenCondition = betweenCondition.and(
					range.getUpperBound().isInclusive() ? property.lte(parameterPlaceholder) : property.lt(parameterPlaceholder));
		}
		return betweenCondition;
	}

	private Property toCypherProperty(Neo4jPersistentProperty persistentProperty) {

		return Cypher.property(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription), persistentProperty.getPropertyName());
	}

	private Expression toCypherProperty(PersistentPropertyPath<Neo4jPersistentProperty> path, boolean addToLower) {

		Neo4jPersistentProperty leafProperty = path.getRequiredLeafProperty();
		Neo4jPersistentEntity<?> owner = (Neo4jPersistentEntity<?>) leafProperty.getOwner();
		Expression expression;

		String containerName = getContainerName(path, owner);
		if (owner.equals(this.nodeDescription) && path.getLength() == 1) {
			expression = leafProperty.isInternalIdProperty() ?
					Cypher.call("id").withArgs(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription)).asFunction() :
					Cypher.property(containerName, leafProperty.getPropertyName());
		} else if (leafProperty.isInternalIdProperty()) {
			expression = Cypher.call("id").withArgs(Cypher.name(containerName)).asFunction();
		} else {
			expression = Cypher.property(containerName, leafProperty.getPropertyName());
		}

		if (addToLower) {
			expression = Functions.toLower(expression);
		}

		return expression;
	}

	private String getContainerName(PersistentPropertyPath<Neo4jPersistentProperty> path, Neo4jPersistentEntity<?> owner) {

		if (owner.equals(this.nodeDescription) && path.getLength() == 1) {
			return Constants.NAME_OF_TYPED_ROOT_NODE.apply(this.nodeDescription).getValue();
		}

		PropertyPathWrapper propertyPathWrapper = propertyPathWrappers.stream()
				.filter(rp -> rp.getPropertyPath().equals(path)).findFirst().get();
		String cypherElementName;
		// this "entity" is a representation of a relationship with properties
		if (owner.isRelationshipPropertiesEntity()) {
			cypherElementName = propertyPathWrapper.getRelationshipName();
		} else {
			cypherElementName = propertyPathWrapper.getNodeName();
		}
		return cypherElementName;
	}

	private Expression toCypherParameter(Parameter parameter, boolean addToLower) {

		return createCypherParameter(parameter.nameOrIndex, addToLower);
	}

	private Expression createCypherParameter(String name, boolean addToLower) {

		Expression expression = Cypher.parameter(name);
		if (addToLower) {
			expression = Functions.toLower(expression);
		}
		return expression;
	}

	private Optional<Parameter> nextOptionalParameter(Iterator<Object> actualParameters, Neo4jPersistentProperty property) {

		Parameter nextRequiredParameter = lastParameter.poll();
		if (nextRequiredParameter != null) {
			return Optional.of(nextRequiredParameter);
		} else if (formalParameters.hasNext()) {
			final Neo4jQueryMethod.Neo4jParameter parameter = formalParameters.next();

			Parameter boundedParameter = new Parameter(parameter.getName().orElseGet(indexSupplier),
					actualParameters.next(), property.getOptionalConverter());
			boundedParameters.add(boundedParameter);
			return Optional.of(boundedParameter);
		} else {
			return Optional.empty();
		}
	}

	private Parameter nextRequiredParameter(Iterator<Object> actualParameters, Neo4jPersistentProperty property) {

		Parameter nextRequiredParameter = lastParameter.poll();
		if (nextRequiredParameter != null) {
			return nextRequiredParameter;
		} else {
			if (!formalParameters.hasNext()) {
				throw new IllegalStateException("Not enough formal, bindable parameters for parts");
			}
			final Neo4jQueryMethod.Neo4jParameter parameter = formalParameters.next();
			Parameter boundedParameter = new Parameter(parameter.getName().orElseGet(indexSupplier),
					actualParameters.next(), property.getOptionalConverter());
			boundedParameters.add(boundedParameter);
			return boundedParameter;
		}
	}

	static class Parameter {

		final String nameOrIndex;

		final Object value;

		final @Nullable Neo4jPersistentPropertyConverter<?> conversionOverride;

		Parameter(String nameOrIndex, Object value, @Nullable Neo4jPersistentPropertyConverter<?> conversionOverride) {
			this.nameOrIndex = nameOrIndex;
			this.value = value;
			this.conversionOverride = conversionOverride;
		}

		boolean hasValueOfType(Class<?> type) {
			return type.isInstance(value);
		}

		@Override
		public String toString() {
			return "Parameter{" + "nameOrIndex='" + nameOrIndex + '\'' + ", value=" + value + '}';
		}
	}

	/**
	 * Provides unique, incrementing indexes for parameter. Parameter indexes in derived query methods are not necessary
	 * dense.
	 */
	static final class IndexSupplier implements Supplier<String> {

		private AtomicInteger current = new AtomicInteger(0);

		@Override
		public String get() {
			return Integer.toString(current.getAndIncrement());
		}
	}
}
