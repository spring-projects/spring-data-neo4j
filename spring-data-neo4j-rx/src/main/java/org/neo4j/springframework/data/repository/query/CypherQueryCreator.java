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
package org.neo4j.springframework.data.repository.query;

import static java.util.stream.Collectors.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.cypher.Functions.*;
import static org.neo4j.springframework.data.core.schema.Constants.*;
import static org.neo4j.springframework.data.repository.query.CypherAdapterUtils.*;
import static org.neo4j.springframework.data.repository.query.PartValidator.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.neo4j.driver.types.Point;
import org.neo4j.springframework.data.core.cypher.Condition;
import org.neo4j.springframework.data.core.cypher.Conditions;
import org.neo4j.springframework.data.core.cypher.Cypher;
import org.neo4j.springframework.data.core.cypher.Expression;
import org.neo4j.springframework.data.core.cypher.Functions;
import org.neo4j.springframework.data.core.cypher.Property;
import org.neo4j.springframework.data.core.cypher.SortItem;
import org.neo4j.springframework.data.core.cypher.Statement;
import org.neo4j.springframework.data.core.cypher.StatementBuilder.OngoingMatchAndReturnWithOrder;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.core.mapping.Neo4jPersistentProperty;
import org.neo4j.springframework.data.core.schema.CypherGenerator;
import org.neo4j.springframework.data.core.schema.GraphPropertyDescription;
import org.neo4j.springframework.data.core.schema.NodeDescription;
import org.neo4j.springframework.data.repository.query.Neo4jQueryMethod.Neo4jParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Box;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Polygon;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * A Cypher-DSL based implementation of the {@link AbstractQueryCreator} that eventually creates Cypher queries as strings
 * to be used by a Neo4j client or driver as statement template.
 * <p/>
 * This class is not thread safe and not reusable.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
final class CypherQueryCreator extends AbstractQueryCreator<QueryAndParameters, Condition> {

	private final Neo4jMappingContext mappingContext;

	private final Class<?> domainType;
	private final NodeDescription<?> nodeDescription;

	private final Neo4jQueryType queryType;

	private final Iterator<?> formalParameters;
	private final Queue<Parameter> lastParameter = new LinkedList<>();

	private final Supplier<String> indexSupplier = new IndexSupplier();

	private final Function<Object, Object> parameterConversion;
	private final List<Parameter> boundedParameters = new ArrayList<>();

	private final Pageable pagingParameter;

	/**
	 * Stores the number of max results, if the {@link PartTree tree} is limiting.
	 */
	private final Number maxResults;

	/**
	 * Sort items may already be needed for some parts, i.e. of type NEAR.
	 */
	private final List<SortItem> sortItems = new ArrayList<>();

	private final List<String> includedProperties;

	CypherQueryCreator(Neo4jMappingContext mappingContext, Class<?> domainType, Neo4jQueryType queryType,
		PartTree tree,
		ParametersParameterAccessor actualParameters,
		List<String> includedProperties,
		Function<Object, Object> parameterConversion
	) {
		super(tree, actualParameters);
		this.mappingContext = mappingContext;

		this.domainType = domainType;
		this.nodeDescription = this.mappingContext.getRequiredNodeDescription(this.domainType);

		this.queryType = queryType;

		this.formalParameters = actualParameters.getParameters().iterator();
		this.maxResults = tree.isLimiting() ? tree.getMaxResults() : null;

		this.includedProperties = includedProperties;
		this.parameterConversion = parameterConversion;

		this.pagingParameter = actualParameters.getPageable();
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
	protected QueryAndParameters complete(Condition condition, Sort sort) {

		CypherGenerator cypherGenerator = CypherGenerator.INSTANCE;
		Statement statement;
		if (queryType == Neo4jQueryType.COUNT) {
			statement = cypherGenerator
				.prepareMatchOf(nodeDescription, condition)
				.returning(Functions.count(asterisk()))
				.build();
		} else {
			OngoingMatchAndReturnWithOrder ongoingMatchAndReturnWithOrder = cypherGenerator
				.prepareMatchOf(nodeDescription, condition)
				.returning(cypherGenerator.createReturnStatementForMatch(nodeDescription, includedProperties))
				.orderBy(
					Stream.concat(
						sortItems.stream(),
						pagingParameter.getSort().and(sort).stream().map(sortAdapterFor(nodeDescription))
					).toArray(SortItem[]::new)
				);
			if (pagingParameter.isUnpaged()) {
				statement = ongoingMatchAndReturnWithOrder.limit(maxResults).build();
			} else {
				long skip = pagingParameter.getOffset();
				int pageSize = pagingParameter.getPageSize();
				statement = ongoingMatchAndReturnWithOrder.skip(skip).limit(pageSize).build();
			}
		}

		Map<String, Object> convertedParameters = this.boundedParameters
			.stream().collect(toMap(p -> p.nameOrIndex, p -> parameterConversion.apply(p.value)));

		return new QueryAndParameters(Renderer.getDefaultRenderer().render(statement), convertedParameters);
	}

	private Condition createImpl(Part part, Iterator<Object> actualParameters) {

		PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext
			.getPersistentPropertyPath(part.getProperty());
		Neo4jPersistentProperty persistentProperty = path.getRequiredLeafProperty();

		boolean ignoreCase = ignoreCase(part);
		switch (part.getType()) {
			case AFTER:
			case GREATER_THAN:
				return toCypherProperty(persistentProperty, ignoreCase)
					.gt(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case BEFORE:
			case LESS_THAN:
				return toCypherProperty(persistentProperty, ignoreCase)
					.lt(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case BETWEEN:
				return betweenCondition(persistentProperty, actualParameters, ignoreCase);
			case CONTAINING:
				return toCypherProperty(persistentProperty, ignoreCase)
					.contains(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case ENDING_WITH:
				return toCypherProperty(persistentProperty, ignoreCase)
					.endsWith(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case EXISTS:
				return Conditions.exists(toCypherProperty(persistentProperty));
			case FALSE:
				return toCypherProperty(persistentProperty, ignoreCase).isFalse();
			case GREATER_THAN_EQUAL:
				return toCypherProperty(persistentProperty, ignoreCase)
					.gte(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case IN:
				return toCypherProperty(persistentProperty, ignoreCase)
					.in(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case IS_EMPTY:
				return toCypherProperty(persistentProperty, ignoreCase).isEmpty();
			case IS_NOT_EMPTY:
				return toCypherProperty(persistentProperty, ignoreCase).isEmpty().not();
			case IS_NOT_NULL:
				return toCypherProperty(persistentProperty, ignoreCase).isNotNull();
			case IS_NULL:
				return toCypherProperty(persistentProperty, ignoreCase).isNull();
			case LESS_THAN_EQUAL:
				return toCypherProperty(persistentProperty, ignoreCase)
					.lte(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex,
					ignoreCase);
			case NEAR:
				return createNearCondition(persistentProperty, actualParameters);
			case NEGATING_SIMPLE_PROPERTY:
				return toCypherProperty(persistentProperty, ignoreCase)
					.isNotEqualTo(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case NOT_CONTAINING:
				return toCypherProperty(persistentProperty, ignoreCase)
					.contains(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase)).not();
			case NOT_IN:
				return toCypherProperty(persistentProperty, ignoreCase)
					.in(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase))
					.not();
			case NOT_LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex,
					ignoreCase).not();
			case SIMPLE_PROPERTY:
				return toCypherProperty(persistentProperty, ignoreCase)
					.isEqualTo(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case STARTING_WITH:
				return toCypherProperty(persistentProperty, ignoreCase)
					.startsWith(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case REGEX:
				return toCypherProperty(persistentProperty, ignoreCase)
					.matches(toCypherParameter(nextRequiredParameter(actualParameters), ignoreCase));
			case TRUE:
				return toCypherProperty(persistentProperty, ignoreCase).isTrue();
			case WITHIN:
				return createWithinCondition(persistentProperty, actualParameters);
			default:
				throw new IllegalArgumentException("Unsupported part type: " + part.getType());
		}
	}

	/**
	 * Checks whether or not to ignore the case for some operations. {@link PartTreeNeo4jQuery} will already have validated
	 * which properties can be made case insensitive given a certain keyword.
	 *
	 * @param part query part to get checked if case should get ignored
	 * @return should the case get ignored
	 */
	boolean ignoreCase(Part part) {

		switch (part.shouldIgnoreCase()) {
			case ALWAYS:
				return true;
			case WHEN_POSSIBLE:
				return canIgnoreCase(part);
			case NEVER:
				return false;
			default:
				throw new IllegalArgumentException("Unsupported option for ignoring case: " + part.shouldIgnoreCase());
		}
	}

	private Condition likeCondition(GraphPropertyDescription persistentProperty, String parameterName,
		boolean ignoreCase) {
		String regexOptions = ignoreCase ? "(?i)" : "";
		return toCypherProperty(persistentProperty, false)
			.matches(literalOf(regexOptions + ".*").plus(Cypher.parameter(parameterName)).plus(literalOf(".*")));
	}

	private Condition betweenCondition(GraphPropertyDescription persistentProperty, Iterator<Object> actualParameters,
		boolean ignoreCase) {

		Parameter lowerBoundOrRange = nextRequiredParameter(actualParameters);

		Expression property = toCypherProperty(persistentProperty, ignoreCase);
		if (lowerBoundOrRange.value instanceof Range) {
			return createRangeConditionForProperty(property, lowerBoundOrRange);
		} else {
			Parameter upperBound = nextRequiredParameter(actualParameters);
			return property.gte(toCypherParameter(lowerBoundOrRange, ignoreCase))
				.and(property.lte(toCypherParameter(upperBound, ignoreCase)));
		}
	}

	private Condition createNearCondition(GraphPropertyDescription persistentProperty,
		Iterator<Object> actualParameters) {

		Parameter p1 = nextRequiredParameter(actualParameters);
		Optional<Parameter> p2 = nextOptionalParameter(actualParameters);

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

		Expression distanceFunction = Functions.distance(toCypherProperty(persistentProperty, false), referencePoint);

		if (other.filter(p -> p.hasValueOfType(Distance.class)).isPresent()) {
			return distanceFunction.lte(toCypherParameter(other.get(), false));
		} else if (other.filter(p -> p.hasValueOfType(Range.class)).isPresent()) {
			return createRangeConditionForProperty(distanceFunction, other.get());
		} else {
			// We only have a point toCypherParameter, that's ok, but we have to put back the last toCypherParameter when it wasn't null
			other.ifPresent(this.lastParameter::offer);

			// Also, we cannot filter, but need to sort in the end.
			this.sortItems.add(distanceFunction.ascending());
			return Conditions.noCondition();
		}
	}

	private Condition createWithinCondition(GraphPropertyDescription persistentProperty,
		Iterator<Object> actualParameters) {
		Parameter area = nextRequiredParameter(actualParameters);
		if (area.hasValueOfType(Circle.class)) {
			// We don't know the CRS of the point, so we assume the same as the reference toCypherProperty
			Expression referencePoint = point(mapOf(
				"x", createCypherParameter(area.nameOrIndex + ".x", false),
				"y", createCypherParameter(area.nameOrIndex + ".y", false),
				"srid", Cypher.property(toCypherProperty(persistentProperty, false), "srid"))
			);
			Expression distanceFunction = Functions
				.distance(toCypherProperty(persistentProperty, false), referencePoint);
			return distanceFunction.lte(createCypherParameter(area.nameOrIndex + ".radius", false));
		} else if (area.hasValueOfType(BoundingBox.class) || area.hasValueOfType(Box.class)) {
			Expression llx = createCypherParameter(area.nameOrIndex + ".llx", false);
			Expression lly = createCypherParameter(area.nameOrIndex + ".lly", false);
			Expression urx = createCypherParameter(area.nameOrIndex + ".urx", false);
			Expression ury = createCypherParameter(area.nameOrIndex + ".ury", false);

			Expression x = Cypher.property(toCypherProperty(persistentProperty, false), "x");
			Expression y = Cypher.property(toCypherProperty(persistentProperty, false), "y");

			return llx.lte(x).and(x.lte(urx)).and(lly.lte(y)).and(y.lte(ury));
		} else if (area.hasValueOfType(Polygon.class)) {
			throw new IllegalArgumentException(String.format("The WITHIN operation does not support a %s. You might want to pass a bounding box instead: %s.of(polygon).", Polygon.class, BoundingBox.class));
		} else {
			throw new IllegalArgumentException(String.format("The WITHIN operation requires an area of type %s or %s.", Circle.class, Box.class));
		}
	}

	/**
	 * @param property property for which the range should get checked
	 * @param rangeParameter parameter that expresses the range
	 * @return The equivalent of a A BETWEEN B AND C expression for a given range.
	 */
	private static Condition createRangeConditionForProperty(Expression property, Parameter rangeParameter) {

		Range range = (Range) rangeParameter.value;
		Condition betweenCondition = Conditions.noCondition();
		if (range.getLowerBound().isBounded()) {
			Expression parameterPlaceholder = createCypherParameter(rangeParameter.nameOrIndex + ".lb", false);
			betweenCondition = betweenCondition.and(range.getLowerBound().isInclusive() ?
				property.gte(parameterPlaceholder) :
				property.gt(parameterPlaceholder));
		}

		if (range.getUpperBound().isBounded()) {
			Expression parameterPlaceholder = createCypherParameter(rangeParameter.nameOrIndex + ".ub", false);
			betweenCondition = betweenCondition.and(range.getUpperBound().isInclusive() ?
				property.lte(parameterPlaceholder) :
				property.lt(parameterPlaceholder));
		}
		return betweenCondition;
	}

	private static Property toCypherProperty(GraphPropertyDescription persistentProperty) {

		return Cypher.property(NAME_OF_ROOT_NODE, persistentProperty.getPropertyName());
	}

	private static Expression toCypherProperty(GraphPropertyDescription persistentProperty, boolean addToLower) {

		Expression expression = Cypher.property(NAME_OF_ROOT_NODE, persistentProperty.getPropertyName());
		if (addToLower) {
			expression = Functions.toLower(expression);
		}
		return expression;
	}

	private static Expression toCypherParameter(Parameter parameter, boolean addToLower) {

		return createCypherParameter(parameter.nameOrIndex, addToLower);
	}

	private static Expression createCypherParameter(String name, boolean addToLower) {

		Expression expression = Cypher.parameter(name);
		if (addToLower) {
			expression = Functions.toLower(expression);
		}
		return expression;
	}

	private Optional<Parameter> nextOptionalParameter(Iterator<Object> actualParameters) {

		Parameter nextRequiredParameter = lastParameter.poll();
		if (nextRequiredParameter != null) {
			return Optional.of(nextRequiredParameter);
		} else if (formalParameters.hasNext()) {
			final Neo4jParameter parameter = (Neo4jParameter) formalParameters.next();
			Parameter boundedParameter = new Parameter(parameter.getName().orElseGet(indexSupplier),
				actualParameters.next());
			boundedParameters.add(boundedParameter);
			return Optional.of(boundedParameter);
		} else {
			return Optional.empty();
		}
	}

	private Parameter nextRequiredParameter(Iterator<Object> actualParameters) {

		Parameter nextRequiredParameter = lastParameter.poll();
		if (nextRequiredParameter != null) {
			return nextRequiredParameter;
		} else {
			if (!formalParameters.hasNext()) {
				throw new IllegalStateException("Not enough formal, bindable parameters for parts");
			}
			final Neo4jParameter parameter = (Neo4jParameter) formalParameters.next();
			Parameter boundedParameter = new Parameter(parameter.getName().orElseGet(indexSupplier),
				actualParameters.next());
			boundedParameters.add(boundedParameter);
			return boundedParameter;
		}
	}

	static class Parameter {

		final String nameOrIndex;

		final Object value;

		Parameter(String nameOrIndex, Object value) {
			this.nameOrIndex = nameOrIndex;
			this.value = value;
		}

		boolean hasValueOfType(Class<?> type) {
			return type.isInstance(value);
		}

		@Override
		public String toString() {
			return "Parameter{" +
				"nameOrIndex='" + nameOrIndex + '\'' +
				", value=" + value +
				'}';
		}
	}

	/**
	 * Provides unique, incrementing indexes for parameter. Parameter indexes in derived query methods
	 * are not necessary dense.
	 */
	static final class IndexSupplier implements Supplier<String> {

		private AtomicInteger current = new AtomicInteger(0);

		@Override
		public String get() {
			return Integer.toString(current.getAndIncrement());
		}
	}
}
