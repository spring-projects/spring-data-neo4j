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
package org.springframework.data.neo4j.repository.query;

import static lombok.AccessLevel.*;
import static org.springframework.data.neo4j.core.cypher.Cypher.*;
import static org.springframework.data.neo4j.core.cypher.Functions.*;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import org.neo4j.driver.types.Point;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.mapping.PersistentPropertyPath;
import org.springframework.data.neo4j.core.cypher.Condition;
import org.springframework.data.neo4j.core.cypher.Conditions;
import org.springframework.data.neo4j.core.cypher.Cypher;
import org.springframework.data.neo4j.core.cypher.Expression;
import org.springframework.data.neo4j.core.cypher.Functions;
import org.springframework.data.neo4j.core.cypher.Property;
import org.springframework.data.neo4j.core.cypher.SortItem;
import org.springframework.data.neo4j.core.cypher.Statement;
import org.springframework.data.neo4j.core.cypher.renderer.CypherRenderer;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.core.schema.NodeDescription;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameter;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * A Cypher-DSL based implementation of the {@link AbstractQueryCreator} that eventually creates Cypher queries as strings
 * to be used by a Neo4j client or driver as statement template.
 * <p />
 * This class is not thread safe and not reusable.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
final class CypherQueryCreator extends AbstractQueryCreator<String, Condition> {

	private final Neo4jMappingContext mappingContext;
	private final Class<?> domainType;
	private final NodeDescription<?> nodeDescription;

	private Iterator<Neo4jParameter> formalParameters;
	private final Queue<Parameter> lastParameter = new LinkedList<>();

	/**
	 * Sort items may already be needed for some parts, i.e. of type NEAR.
	 */
	private final List<SortItem> sortItems = new ArrayList<>();

	CypherQueryCreator(Neo4jMappingContext mappingContext, Class<?> domainType, PartTree tree,
		Parameters<Neo4jParameters, Neo4jParameter> formalParameters, ParameterAccessor actualParameters
	) {
		super(tree, actualParameters);

		this.mappingContext = mappingContext;
		this.domainType = domainType;
		this.nodeDescription = this.mappingContext.getRequiredNodeDescription(this.domainType);
		this.formalParameters = formalParameters.iterator();
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
	protected String complete(Condition condition, Sort sort) {

		Statement statement = mappingContext
			.prepareMatchOf(nodeDescription, Optional.of(condition))
			.returning(Cypher.asterisk())
			.orderBy(sortItems.toArray(new SortItem[sortItems.size()]))
			.build();

		return CypherRenderer.create().render(statement);
	}

	private Condition createImpl(Part part, Iterator<Object> actualParameters) {

		PersistentPropertyPath<Neo4jPersistentProperty> path = mappingContext
			.getPersistentPropertyPath(part.getProperty());
		Neo4jPersistentProperty persistentProperty = path.getLeafProperty();

		// TODO case insensitive (like, notlike, simpleProperty, negatedSimpleProperty)
		switch (part.getType()) {
			case AFTER:
			case GREATER_THAN:
				return property(persistentProperty).gt(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case BEFORE:
			case LESS_THAN:
				return property(persistentProperty).lt(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case BETWEEN:
				return betweenCondition(persistentProperty, actualParameters);
			case CONTAINING:
				return property(persistentProperty)
					.contains(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case ENDING_WITH:
				return property(persistentProperty)
					.endsWith(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case EXISTS:
				return Conditions.exists(property(persistentProperty));
			case FALSE:
				return property(persistentProperty).isFalse();
			case GREATER_THAN_EQUAL:
				return property(persistentProperty).gte(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case IN:
				return property(persistentProperty).in(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case IS_EMPTY:
				return property(persistentProperty).isEmpty();
			case IS_NOT_EMPTY:
				return property(persistentProperty).isEmpty().not();
			case IS_NOT_NULL:
				return property(persistentProperty).isNotNull();
			case IS_NULL:
				return property(persistentProperty).isNull();
			case LESS_THAN_EQUAL:
				return property(persistentProperty).lte(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex);
			case NEAR:
				return createNearCondition(persistentProperty, actualParameters);
			case NEGATING_SIMPLE_PROPERTY:
				return property(persistentProperty)
					.isNotEqualTo(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case NOT_CONTAINING:
				return property(persistentProperty)
					.contains(parameter(nextRequiredParameter(actualParameters).nameOrIndex)).not();
			case NOT_IN:
				return property(persistentProperty).in(parameter(nextRequiredParameter(actualParameters).nameOrIndex))
					.not();
			case NOT_LIKE:
				return likeCondition(persistentProperty, nextRequiredParameter(actualParameters).nameOrIndex).not();
			case SIMPLE_PROPERTY:
				return property(persistentProperty)
					.isEqualTo(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case STARTING_WITH:
				return property(persistentProperty)
					.startsWith(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case REGEX:
				return property(persistentProperty)
					.matches(parameter(nextRequiredParameter(actualParameters).nameOrIndex));
			case TRUE:
				return property(persistentProperty).isTrue();
			case WITHIN:
				return createWithinCondition(persistentProperty, actualParameters);
			default:
				throw new IllegalArgumentException("Unsupported part type: " + part.getType());
		}
	}

	private Condition likeCondition(Neo4jPersistentProperty persistentProperty, String parameterName) {
		return property(persistentProperty)
			.matches(literalOf(".*").plus(parameter(parameterName)).plus(literalOf(".*")));
	}

	private Condition betweenCondition(Neo4jPersistentProperty persistentProperty, Iterator<Object> actualParameters) {

		Parameter lowerBoundOrRange = nextRequiredParameter(actualParameters);

		Property property = property(persistentProperty);
		if (lowerBoundOrRange.value instanceof Range) {
			return createRangeConditionForProperty(property, lowerBoundOrRange);
		} else {
			Parameter upperBound = nextRequiredParameter(actualParameters);
			return property.gte(parameter(lowerBoundOrRange.nameOrIndex))
				.and(property.lte(parameter(upperBound.nameOrIndex)));
		}
	}

	private Condition createNearCondition(Neo4jPersistentProperty persistentProperty,
		Iterator<Object> actualParameters) {

		Parameter p1 = nextRequiredParameter(actualParameters);
		Optional<Parameter> p2 = nextOptionalParameter(actualParameters);

		Expression referencePoint;

		Optional<Parameter> other;
		if (p1.value instanceof Point) {
			referencePoint = parameter(p1.nameOrIndex);
			other = p2;
		} else if (p2.isPresent() && p2.get().value instanceof Point) {
			referencePoint = parameter(p2.get().nameOrIndex);
			other = Optional.of(p1);
		} else {
			throw new IllegalArgumentException(
				String.format("The NEAR operation requires a reference point of type %s", Point.class));
		}

		Expression distanceFunction = Functions.distance(property(persistentProperty), referencePoint);

		if (other.filter(p -> p.hasValueOfType(Distance.class)).isPresent()) {
			return distanceFunction.lte(parameter(other.get().nameOrIndex));
		} else if (other.filter(p -> p.hasValueOfType(Range.class)).isPresent()) {
			return createRangeConditionForProperty(distanceFunction, other.get());
		} else {
			// We only have a point parameter, that's ok, but we have to put back the last parameter when it wasn't null
			other.ifPresent(this.lastParameter::offer);

			// Also, we cannot filter, but need to sort in the end.
			this.sortItems.add(distanceFunction.ascending());
			return Conditions.noCondition();
		}
	}

	private Condition createWithinCondition(Neo4jPersistentProperty persistentProperty,
		Iterator<Object> actualParameters) {

		Parameter area = nextRequiredParameter(actualParameters);
		if (area.hasValueOfType(Circle.class)) {
			// We don't know the CRS of the point, so we assume the same as the reference property
			Expression referencePoint = point(mapOf(
				"x", parameter(area.nameOrIndex + ".x"),
				"y", parameter(area.nameOrIndex + ".y"),
				"srid", Cypher.property(property(persistentProperty), "srid"))
			);
			Expression distanceFunction = Functions.distance(property(persistentProperty), referencePoint);
			return distanceFunction.lte(parameter(area.nameOrIndex + ".radius"));
		} else {
			throw new IllegalArgumentException(
				String.format("The WITHIN operation requires an area of type %s or %s.", Circle.class));
		}
	}

	/**
	 * @param property
	 * @param rangeParameter
	 * @return The equivalent of a A BETWEEN B AND C expression for a given range.
	 */
	private static Condition createRangeConditionForProperty(Expression property, Parameter rangeParameter) {

		Range range = (Range) rangeParameter.value;
		Condition betweenCondition = Conditions.noCondition();
		if (range.getLowerBound().isBounded()) {
			Expression parameterPlaceholder = parameter(rangeParameter.nameOrIndex + ".lb");
			betweenCondition = betweenCondition.and(range.getLowerBound().isInclusive() ?
				property.gte(parameterPlaceholder) :
				property.gt(parameterPlaceholder));
		}

		if (range.getUpperBound().isBounded()) {
			Expression parameterPlaceholder = parameter(rangeParameter.nameOrIndex + ".ub");
			betweenCondition = betweenCondition.and(range.getUpperBound().isInclusive() ?
				property.lte(parameterPlaceholder) :
				property.lt(parameterPlaceholder));
		}
		return betweenCondition;
	}

	private static Property property(Neo4jPersistentProperty persistentProperty) {
		return Cypher.property(NodeDescription.NAME_OF_ROOT_NODE, persistentProperty.getPropertyName());
	}

	private Optional<Parameter> nextOptionalParameter(Iterator<Object> actualParameters) {

		Parameter nextRequiredParameter = lastParameter.poll();
		if (nextRequiredParameter != null) {
			return Optional.of(nextRequiredParameter);
		} else if (formalParameters.hasNext()) {
			final Neo4jParameter parameter = formalParameters.next();
			return Optional.of(new Parameter(parameter.getNameOrIndex(), actualParameters.next()));
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
			final Neo4jParameter parameter = formalParameters.next();
			return new Parameter(parameter.getNameOrIndex(), actualParameters.next());
		}
	}

	@RequiredArgsConstructor(access = PACKAGE)
	@ToString
	static class Parameter {

		final String nameOrIndex;

		final Object value;

		boolean hasValueOfType(Class<?> type) {
			return type.isInstance(value);
		}
	}
}
