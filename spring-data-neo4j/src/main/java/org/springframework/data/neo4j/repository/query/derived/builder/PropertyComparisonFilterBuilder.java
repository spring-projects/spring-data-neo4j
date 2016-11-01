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

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 */
public class PropertyComparisonFilterBuilder extends CypherFilterBuilder {

	public PropertyComparisonFilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<CypherFilter> build() {
		List<CypherFilter> filters = new ArrayList<>();

		CypherFilter filter = new CypherFilter();
		filter.setPropertyName(propertyName());
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());

		filter.setComparisonOperator(convertToComparisonOperator(part.getType()));
		setNestedAttributes(part, filter);

		filters.add(filter);

		return filters;
	}


	private ComparisonOperator convertToComparisonOperator(Part.Type type) {
		switch (type) {
			case AFTER:
			case GREATER_THAN:
				return ComparisonOperator.GREATER_THAN;
			case GREATER_THAN_EQUAL:
				return ComparisonOperator.GREATER_THAN_EQUAL;
			case BEFORE:
			case LESS_THAN:
				return ComparisonOperator.LESS_THAN;
			case LESS_THAN_EQUAL:
				return ComparisonOperator.LESS_THAN_EQUAL;
			case REGEX:
				return ComparisonOperator.MATCHES;
			case LIKE:
			case NOT_LIKE:
				return ComparisonOperator.LIKE;
			case STARTING_WITH:
				return ComparisonOperator.STARTING_WITH;
			case ENDING_WITH:
				return ComparisonOperator.ENDING_WITH;
			case CONTAINING:
			case NOT_CONTAINING:
				return ComparisonOperator.CONTAINING;
			case IN:
			case NOT_IN:
				return ComparisonOperator.IN;
			case SIMPLE_PROPERTY:
				return ComparisonOperator.EQUALS;
			default:
				throw new IllegalArgumentException("No ComparisonOperator for Part.Type " + type);
		}
	}
}
