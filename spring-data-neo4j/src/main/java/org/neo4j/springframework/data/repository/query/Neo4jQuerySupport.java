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
package org.neo4j.springframework.data.repository.query;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.apache.commons.logging.LogFactory;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;
import org.neo4j.springframework.data.core.convert.Neo4jSimpleTypes;
import org.neo4j.springframework.data.core.mapping.Neo4jMappingContext;
import org.neo4j.springframework.data.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.core.log.LogAccessor;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.Assert;

/**
 * Some conversions used by both reactive and imperative Neo4j queries. While we try to separate reactive and imperative
 * flows, it is cumbersome to repeat those conversions all over the place.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
abstract class Neo4jQuerySupport {

	protected final Neo4jMappingContext mappingContext;
	protected final Neo4jQueryMethod queryMethod;
	protected final Class<?> domainType;

	private static final LogAccessor log = new LogAccessor(LogFactory.getLog(Neo4jQuerySupport.class));

	Neo4jQuerySupport(Neo4jMappingContext mappingContext, Neo4jQueryMethod queryMethod) {

		Assert.notNull(mappingContext, "The mapping context is required.");
		Assert.notNull(queryMethod, "Query method must not be null!");

		this.mappingContext = mappingContext;
		this.queryMethod = queryMethod;
		this.domainType = queryMethod.getDomainClass();
	}

	protected final Neo4jParameterAccessor getParameterAccessor(Object[] actualParameters) {
		return new Neo4jParameterAccessor((Neo4jParameters) this.queryMethod.getParameters(), actualParameters);
	}

	protected final ResultProcessor getResultProcessor(ParameterAccessor parameterAccessor) {
		return queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);
	}

	protected final BiFunction<TypeSystem, Record, ?> getMappingFunction(final ResultProcessor resultProcessor) {

		final Class<?> returnedType = resultProcessor.getReturnedType().getReturnedType();

		final BiFunction<TypeSystem, Record, ?> mappingFunction;
		if (Neo4jSimpleTypes.HOLDER.isSimpleType(returnedType)) {
			// Clients automatically selects a single value mapping function.
			// It will thrown an error if the query contains more than one column.
			mappingFunction = null;
		} else if (resultProcessor.getReturnedType().isProjecting()) {

			if (returnedType.isInterface()) {
				mappingFunction = this.mappingContext.getMappingFunctionFor(domainType);
			} else if (this.mappingContext.hasPersistentEntityFor(returnedType)) {
				mappingFunction = this.mappingContext.getMappingFunctionFor(returnedType);
			} else {
				this.mappingContext.addPersistentEntity(returnedType);
				mappingFunction = this.mappingContext.getMappingFunctionFor(returnedType);
			}
		} else {
			mappingFunction = this.mappingContext.getMappingFunctionFor(domainType);
		}
		return mappingFunction;
	}

	protected final List<String> getInputProperties(final ResultProcessor resultProcessor) {

		ReturnedType returnedType = resultProcessor.getReturnedType();
		return returnedType.isProjecting() ? returnedType.getInputProperties() : Collections.emptyList();
	}

	/**
	 * Converts parameter as needed by the query generated, which is not covered by standard conversion services.
	 *
	 * @param parameter The parameter to fit into the generated query.
	 * @return A parameter that fits the place holders of a generated query
	 */
	final Object convertParameter(Object parameter) {

		if (parameter == null) {
			// According to https://neo4j.com/docs/cypher-manual/current/syntax/working-with-null/#cypher-null-intro
			// it does not make any sense to continue if a `null` value gets into a comparison
			// but we just warn the users and do not throw an exception on `null`.
			log.warn("Do not use `null` as a property value for comparison."
				+ " It will always be false and return an empty result.");

			return Values.NULL;
		}

		// Maybe move all of those into Neo4jConverter at some point.
		if (parameter instanceof Range) {
			return convertRange((Range) parameter);
		} else if (parameter instanceof Distance) {
			return calculateDistanceInMeter((Distance) parameter);
		} else if (parameter instanceof Circle) {
			return convertCircle((Circle) parameter);
		} else if (parameter instanceof Instant) {
			return ((Instant) parameter).atOffset(ZoneOffset.UTC);
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
