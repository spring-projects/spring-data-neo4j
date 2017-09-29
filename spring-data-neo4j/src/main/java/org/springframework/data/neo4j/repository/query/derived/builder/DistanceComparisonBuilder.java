/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.DistanceComparison;
import org.neo4j.ogm.cypher.function.DistanceFromPoint;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class DistanceComparisonBuilder extends FilterBuilder {

	public DistanceComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		Object firstArg = params.pop();
		Object secondArg = params.pop();

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

		double meters;
		if (distance.getMetric() == Metrics.KILOMETERS) {
			meters = distance.getValue() * 1000.0d;
		} else if (distance.getMetric() == Metrics.MILES) {
			meters = distance.getValue() / 0.00062137d;
		} else {
			meters = distance.getValue();
		}

		DistanceFromPoint distanceFromPoint = new DistanceFromPoint(point.getX(), point.getY(),
				distance.getValue() * meters);
		DistanceComparison distanceComparison = new DistanceComparison(distanceFromPoint);

		Filter filter = new Filter(distanceComparison, ComparisonOperator.LESS_THAN);
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		setNestedAttributes(part, filter);

		return Collections.singletonList(filter);
	}

}
