/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repository.query.derived;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filters;
import org.springframework.data.neo4j.repository.query.derived.builder.BetweenComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.BooleanComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.ContainsComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.DistanceComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.ExistsFilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.FilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.IsNullFilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.PropertyComparisonBuilder;
import org.springframework.data.repository.query.parser.Part;

/**
 * A filter builder definition represents an ongoing state of {@link FilterBuilder filter builder} chained together with
 * "and" or "or". It is used with a resolved parameters to create an instance {@link Filters} for building a query
 * object.
 *
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
class FilterBuildersDefinition {

	private final Class<?> entityType;

	private final Part basePart;

	private final List<FilterBuilder> filterBuilders;

	static UnstartedBuild forType(Class<?> entityType) {
		return new UnstartedBuild(entityType);
	}

	private FilterBuildersDefinition(Class<?> entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
		this.filterBuilders = new LinkedList<>();

		this.filterBuilders.add(builderForPart(basePart, BooleanOperator.NONE));
	}

	FilterBuildersQuery buildFilterQuery() {
		return new FilterBuildersQuery(Collections.unmodifiableList(filterBuilders));
	}

	/**
	 * Comment on original DerivedQueryDefinition was: "because the OR is handled in a weird way. Luanne, explain better"
	 *
	 * @return
	 */
	Part getBasePart() {
		return basePart;
	}

	FilterBuildersDefinition and(Part part) {
		this.filterBuilders.add(builderForPart(part, BooleanOperator.AND));
		return this;
	}

	FilterBuildersDefinition or(Part part) {
		this.filterBuilders.add(builderForPart(part, BooleanOperator.OR));
		return this;
	}

	private FilterBuilder builderForPart(Part part, BooleanOperator booleanOperator) {
		switch (part.getType()) {
			case NEAR:
				return new DistanceComparisonBuilder(part, booleanOperator, entityType);
			case BETWEEN:
				return new BetweenComparisonBuilder(part, booleanOperator, entityType);
			case NOT_CONTAINING:
			case CONTAINING:
				return resolveMatchingContainsFilterBuilder(part, booleanOperator);
			case IS_NULL:
			case IS_NOT_NULL:
				return new IsNullFilterBuilder(part, booleanOperator, entityType);
			case EXISTS:
				return new ExistsFilterBuilder(part, booleanOperator, entityType);
			case TRUE:
			case FALSE:
				return new BooleanComparisonBuilder(part, booleanOperator, entityType);
			default:
				return new PropertyComparisonBuilder(part, booleanOperator, entityType);
		}
	}

	private FilterBuilder resolveMatchingContainsFilterBuilder(Part part, BooleanOperator booleanOperator) {
		boolean usePropertyComparison = !part.getProperty().getTypeInformation().isCollectionLike();
		if (usePropertyComparison) {
			return new PropertyComparisonBuilder(part, booleanOperator, entityType);
		}
		return new ContainsComparisonBuilder(part, booleanOperator, entityType);
	}

	static class UnstartedBuild {
		private final Class<?> entityType;

		UnstartedBuild(Class<?> entityType) {
			this.entityType = entityType;
		}

		FilterBuildersDefinition startWith(Part firstPart) {
			return new FilterBuildersDefinition(entityType, firstPart);
		}
	}
}
