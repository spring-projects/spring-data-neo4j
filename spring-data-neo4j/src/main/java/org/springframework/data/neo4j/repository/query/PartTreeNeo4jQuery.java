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

import static java.util.stream.Collectors.*;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.neo4j.driver.types.Point;
import org.springframework.data.domain.Range;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.neo4j.core.NodeManager;
import org.springframework.data.neo4j.core.PreparedQuery;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.repository.query.Neo4jQueryMethod.Neo4jParameters;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.repository.query.parser.PartTree.OrPart;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.util.Assert;

/**
 * Implementation of {@link RepositoryQuery} for derived finder methods.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@Slf4j
final class PartTreeNeo4jQuery extends AbstractNeo4jQuery {

	//
	/**
	 * A set of the temporal types that are directly passable to the driver and support a meaningful comparision in a
	 * temporal sense (after, before).
	 * See <a href="See https://neo4j.com/docs/driver-manual/1.7/cypher-values/#driver-neo4j-type-system"
	 */
	private static final Set<Class<?>> COMPARABLE_TEMPORAL_TYPES = Collections
		.unmodifiableSet(new HashSet<>(Arrays.asList(LocalDate.class, OffsetTime.class, ZonedDateTime.class,
			LocalDateTime.class)));

	private final ResultProcessor processor;
	private final PartTree tree;

	PartTreeNeo4jQuery(
		NodeManager nodeManager,
		Neo4jMappingContext mappingContext,
		Neo4jQueryMethod queryMethod
	) {
		super(nodeManager, mappingContext, queryMethod);

		this.processor = queryMethod.getResultProcessor();
		this.tree = new PartTree(queryMethod.getName(), domainType);

		this.tree.flatMap(OrPart::stream)
			.forEach(part -> validatePart(part));
	}

	@Override
	protected PreparedQuery<?> prepareQuery(Object[] parameters) {

		Neo4jParameters formalParameters = (Neo4jParameters) this.queryMethod.getParameters();
		ParameterAccessor actualParameters = new ParametersParameterAccessor(formalParameters, parameters);
		CypherQueryCreator queryCreator = new CypherQueryCreator(
			mappingContext, domainType, tree, formalParameters, actualParameters
		);

		String cypherQuery = queryCreator.createQuery();
		Map<String, Object> boundedParameters = formalParameters
			.getBindableParameters().stream()
			.collect(toMap(Neo4jQueryMethod.Neo4jParameter::getNameOrIndex,
				formalParameter -> convertParameter(parameters[formalParameter.getIndex()])));

		return PreparedQuery.queryFor(super.domainType).withCypherQuery(cypherQuery)
			.withParameters(boundedParameters)
			.usingMappingFunction(mappingContext.getMappingFunctionFor(super.domainType).orElse(null))
			.build();
	}

	void validatePart(Part part) {

		switch (part.getType()) {
			case AFTER:
			case BEFORE:
				validateTemporalProperty(part);
				break;
			case IS_EMPTY:
			case IS_NOT_EMPTY:
				validateCollectionProperty(part);
				break;
			case NEAR:
			case WITHIN:
				validatePointProperty(part);
				break;
		}
	}

	void validateTemporalProperty(Part part) {

		Assert.state(COMPARABLE_TEMPORAL_TYPES.contains(part.getProperty().getLeafType()), () -> String
			.format("%s works only with properties with one of the following types: %s", part.getType(),
				COMPARABLE_TEMPORAL_TYPES));
	}

	void validateCollectionProperty(Part part) {
		Assert.state(part.getProperty().getLeafProperty().isCollection(), () -> String
			.format("%s works only with collection properties", part.getType()));
	}

	void validatePointProperty(Part part) {

		Assert.state(ClassTypeInformation.from(Point.class)
			.isAssignableFrom(part.getProperty().getLeafProperty().getTypeInformation()), () -> String
			.format("%s works only with spatial properties", part.getType()));
	}

	/**
	 * Converts parameter as needed by the query generated, which is not covered by standard conversion services
	 *
	 * @param parameter The parameter to fit into the generated query.
	 * @return A parameter that fits the place holders of a generated query
	 */
	static Object convertParameter(Object parameter) {
		if (parameter instanceof Range) {
			Range range = (Range) parameter;
			Map<String, Object> map = new HashMap<>();
			range.getLowerBound().getValue().map(PartTreeNeo4jQuery::convertParameter).ifPresent(v -> map.put("lb", v));
			range.getUpperBound().getValue().map(PartTreeNeo4jQuery::convertParameter).ifPresent(v -> map.put("ub", v));
			return map;
		} else if (parameter instanceof Distance) {
			return calculateDistanceInMeter((Distance) parameter);
		} else if (parameter instanceof Circle) {
			Circle circle = (Circle) parameter;
			Map<String, Object> map = new HashMap<>();
			map.put("x", convertParameter(circle.getCenter().getX()));
			map.put("y", convertParameter(circle.getCenter().getY()));
			map.put("radius", convertParameter(calculateDistanceInMeter(circle.getRadius())));
			return map;
		}
		return parameter;
	}

	private static double calculateDistanceInMeter(Distance distance) {

		if (distance.getMetric() == Metrics.KILOMETERS) {
			return distance.getValue() / 0.001d;
		} else if (distance.getMetric() == Metrics.MILES) {
			return distance.getValue() / 0.00062137d;
		} else {
			return distance.getValue();
		}
	}


	@Override
	protected boolean isCountQuery() {
		return tree.isCountProjection();
	}

	@Override
	protected boolean isExistsQuery() {
		return tree.isExistsProjection();
	}

	@Override
	protected boolean isDeleteQuery() {
		return tree.isDelete();
	}

	@Override
	protected boolean isLimiting() {
		return tree.isLimiting();
	}
}
