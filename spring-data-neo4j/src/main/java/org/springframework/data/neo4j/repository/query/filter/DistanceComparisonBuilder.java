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
package org.springframework.data.neo4j.repository.query.filter;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.DistanceComparison;
import org.neo4j.ogm.cypher.function.DistanceFromNativePoint;
import org.neo4j.ogm.cypher.function.DistanceFromPoint;
import org.neo4j.ogm.cypher.function.FilterFunction;
import org.neo4j.ogm.cypher.function.NativeDistanceComparison;
import org.neo4j.ogm.types.spatial.AbstractPoint;
import org.springframework.beans.BeanUtils;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
class DistanceComparisonBuilder extends FilterBuilder {

	private final BiFunction<String, FilterFunction, Filter> filterSupplier;

	DistanceComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);

		this.filterSupplier = createFilterSupplier();
	}

	private static BiFunction<String, FilterFunction, Filter> createFilterSupplier() {
		try {
			// Neo4j-OGM 3.2
			final Constructor<Filter> ctor = Filter.class
					.getDeclaredConstructor(String.class, FilterFunction.class, ComparisonOperator.class);
			return (propertyName, filterFunction) ->
					BeanUtils.instantiateClass(ctor, propertyName, filterFunction, ComparisonOperator.LESS_THAN);
		} catch (NoSuchMethodException e) {
			return (propertyName, filterFunction) -> new Filter(propertyName, filterFunction);
		}
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		Object firstArg = params.pop();
		Object secondArg = params.pop();

		Filter distanceComparisonFilter;

		if (needsFilterForSpringPoint(firstArg, secondArg)) {
			distanceComparisonFilter = springPointFilterOf(firstArg, secondArg);
		} else {
			distanceComparisonFilter = nativePointFilterOf(firstArg, secondArg);
		}

		return Collections.singletonList(distanceComparisonFilter);

	}

	private Filter springPointFilterOf(Object firstArg, Object secondArg) {

		Distance distance;
		Point point;

		if (firstArg instanceof Distance && secondArg instanceof Point) {
			distance = (Distance) firstArg;
			point = (Point) secondArg;
		} else if (secondArg instanceof Distance && firstArg instanceof Point) {
			distance = (Distance) secondArg;
			point = (Point) firstArg;
		} else {
			throw new IllegalArgumentException(
					"findNear requires an argument of type Distance and an argument of type Point");
		}

		return createFilterForSpringPoint(point, calculateDistanceInMeter(distance));
	}

	private Filter nativePointFilterOf(Object firstArg, Object secondArg) {

		Distance distance;
		AbstractPoint spatialPoint;

		if (firstArg instanceof AbstractPoint && secondArg instanceof Distance) {
			distance = (Distance) secondArg;
			spatialPoint = (AbstractPoint) firstArg;
		} else if (firstArg instanceof Distance && secondArg instanceof AbstractPoint) {
			distance = (Distance) firstArg;
			spatialPoint = (AbstractPoint) secondArg;
		} else {
			throw new IllegalArgumentException(
					"findNear requires an argument of type Distance and an argument of type Point");
		}

		return createFilterForSpatialPoint(spatialPoint, calculateDistanceInMeter(distance));
	}

	private boolean needsFilterForSpringPoint(Object firstArg, Object secondArg) {
		return (firstArg instanceof Point || secondArg instanceof Point);
	}

	private Filter createFilterForSpatialPoint(AbstractPoint spatialPoint, double meters) {

		NativeDistanceComparison distanceComparison = NativeDistanceComparison
				.distanceComparisonFor(new DistanceFromNativePoint(spatialPoint, meters));
		NestedAttributes nestedAttributes = getNestedAttributes(part);

		String propertyName = super.part.getProperty().getLeafProperty().getSegment();

		Filter filter = filterSupplier.apply(propertyName, distanceComparison);
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		filter.setNestedPath(nestedAttributes.getSegments());
		return filter;
	}

	private Filter createFilterForSpringPoint(Point point, double meters) {

		DistanceFromPoint distanceFromPoint = new DistanceFromPoint(point.getX(), point.getY(), meters);

		DistanceComparison distanceComparison = new DistanceComparison(distanceFromPoint) {

			@Override
			public String expression(String nodeIdentifier, String filteredProperty,
					UnaryOperator<String> createUniqueParameterName) {
				String latitudeProperty = nodeIdentifier + ".latitude";
				String longitudeProperty = nodeIdentifier + ".longitude";

				return String.format(
						"distance(coalesce(point({latitude: %s, longitude: %s}), %s), point({latitude:$lat, longitude:$lon})) "
								+ "%s $distance ",
						latitudeProperty, longitudeProperty, nodeIdentifier + "." + propertyName(),
						ComparisonOperator.LESS_THAN.getValue());
			}
		};

		NestedAttributes nestedAttributes = getNestedAttributes(part);

		Filter filter = filterSupplier.apply(nestedAttributes.getLeafPropertySegment(), distanceComparison);
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		filter.setNestedPath(nestedAttributes.getSegments());
		return filter;
	}

	static double calculateDistanceInMeter(Distance distance) {

		if (distance.getMetric() == Metrics.KILOMETERS) {
			return distance.getValue() / 0.001d;
		} else if (distance.getMetric() == Metrics.MILES) {
			return distance.getValue() / 0.00062137d;
		} else {
			return distance.getValue();
		}
	}

}
