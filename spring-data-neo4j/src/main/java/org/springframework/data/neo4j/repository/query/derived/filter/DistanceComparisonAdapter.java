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

package org.springframework.data.neo4j.repository.query.derived.filter;

import java.util.Map;

import org.neo4j.ogm.cypher.function.DistanceComparison;
import org.neo4j.ogm.cypher.function.DistanceFromPoint;
import org.neo4j.ogm.cypher.function.FilterFunction;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;

/**
 * Adapter to the OGM FilterFunction interface for a PropertyComparison.
 *
 * @author Jasper Blues
 */
public class DistanceComparisonAdapter implements FunctionAdapter<DistanceFromPoint> {

	private CypherFilter cypherFilter;
	private DistanceComparison distanceComparison;

	public DistanceComparisonAdapter(CypherFilter cypherFilter) {
		this.distanceComparison = new DistanceComparison();
		this.cypherFilter = cypherFilter;
	}

	public DistanceComparisonAdapter() {
		this(null);
	}

	@Override
	public CypherFilter cypherFilter() {
		return cypherFilter;
	}

	@Override
	public FilterFunction<DistanceFromPoint> filterFunction() {
		return distanceComparison;
	}

	@Override
	public int parameterCount() {
		return 2;
	}

	@Override
	public void setValueFromArgs(Map<Integer, Object> params) {
		if (cypherFilter == null) {
			throw new IllegalStateException("Can't set value from args when cypherFilter is null.");
		}

		Object firstArg = params.get(cypherFilter().getPropertyPosition());
		Object secondArg = params.get(cypherFilter().getPropertyPosition() + 1);

		Distance distance;
		Point point;

		if (firstArg instanceof Distance && secondArg instanceof Point) {
			distance = (Distance) firstArg;
			point = (Point) secondArg;
		} else if (secondArg instanceof Distance && firstArg instanceof Point) {
			distance = (Distance) secondArg;
			point = (Point) firstArg;
		} else {
			throw new IllegalArgumentException("findNear requires an argument of type Distance and an argument of type Point");
		}

		double meters;
		if (distance.getMetric() == Metrics.KILOMETERS) {
			meters = distance.getValue() * 1000.0d;
		} else if (distance.getMetric() == Metrics.MILES) {
			meters = distance.getValue() / 0.00062137d;
		} else {
			meters = distance.getValue();
		}

		distanceComparison.setValue(new DistanceFromPoint(point.getX(), point.getY(), distance.getValue() * meters));
	}
}
