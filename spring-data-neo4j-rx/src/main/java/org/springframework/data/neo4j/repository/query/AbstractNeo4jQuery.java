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

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Range;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * Base class for {@link RepositoryQuery} implementations for Neo4j.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
abstract class AbstractNeo4jQuery implements RepositoryQuery {

	protected final Neo4jClient neo4jClient;
	protected final Neo4jMappingContext mappingContext;
	protected final Neo4jQueryMethod queryMethod;
	protected final Class<?> domainType;

	AbstractNeo4jQuery(Neo4jClient neo4jClient,
		Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod) {

		Assert.notNull(neo4jClient, "The Neo4j client is required.");
		Assert.notNull(mappingContext, "The mapping context is required.");
		Assert.notNull(queryMethod, "Query method must not be null!");

		this.neo4jClient = neo4jClient;
		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;
		this.domainType = queryMethod.getReturnedObjectType();
	}

	@Override
	public QueryMethod getQueryMethod() {
		return this.queryMethod;
	}

	@Override
	public final Object execute(Object[] parameters) {
		return new Neo4jQueryExecution.DefaultQueryExecution(neo4jClient)
			.execute(prepareQuery(parameters), queryMethod.isCollectionQuery());
	}

	protected abstract PreparedQuery prepareQuery(Object[] parameters);

	/**
	 * Returns whether the query should get a count projection applied.
	 *
	 * @return
	 */
	protected abstract boolean isCountQuery();

	/**
	 * @return True if the query should get an exists projection applied.
	 */
	protected abstract boolean isExistsQuery();

	/**
	 * @return True if the query should delete matching nodes.
	 */
	protected abstract boolean isDeleteQuery();

	/**
	 * @return True if the query has an explicit limit set.
	 */
	protected abstract boolean isLimiting();

	/**
	 * Converts parameter as needed by the query generated, which is not covered by standard conversion services.
	 *
	 * @param parameter The parameter to fit into the generated query.
	 * @return A parameter that fits the place holders of a generated query
	 */
	final Object convertParameter(Object parameter) {
		if (parameter instanceof Range) {
			return convertRange((Range) parameter);
		} else if (parameter instanceof Distance) {
			return calculateDistanceInMeter((Distance) parameter);
		} else if (parameter instanceof Circle) {
			return convertCircle((Circle) parameter);
		}

		// Good hook to check the NodeManager whether the thing is an entity and we replace the value with a known id.

		return parameter;
	}

	private Map<String, Object> convertRange(Range range) {
		Map<String, Object> map = new HashMap<>();
		range.getLowerBound().getValue().map(this::convertParameter).ifPresent(v -> map.put("lb", v));
		range.getUpperBound().getValue().map(this::convertParameter).ifPresent(v -> map.put("ub", v));
		return map;
	}

	private Map<String, Object> convertCircle(Circle circle) {
		Map<String, Object> map = new HashMap<>();
		map.put("x", convertParameter(circle.getCenter().getX()));
		map.put("y", convertParameter(circle.getCenter().getY()));
		map.put("radius", convertParameter(calculateDistanceInMeter(circle.getRadius())));
		return map;
	}

	private static double calculateDistanceInMeter(Distance distance) {

		if (distance.getMetric() == Metrics.KILOMETERS) {
			double kilometersDivisor = 0.001d;
			return distance.getValue() / kilometersDivisor;

		} else if (distance.getMetric() == Metrics.MILES) {
			double milesDivisor = 0.00062137d;
			return distance.getValue() / milesDivisor;

		} else {
			return distance.getValue();
		}
	}
}
