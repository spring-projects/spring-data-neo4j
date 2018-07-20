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

package org.springframework.data.neo4j.repository.query.derived.builder;

import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
public class PropertyComparisonBuilder extends FilterBuilder {

	public PropertyComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		Object value = params.pop();

		Filter filter = new Filter(propertyName(), convertToComparisonOperator(part.getType()), value);
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		setNestedAttributes(part, filter);
		applyCaseInsensitivityIfShouldIgnoreCase(part, filter);

		return Collections.singletonList(filter);
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

	/**
	 * Sets the filter to ignore the case in case the underlying {@link Part} requires ignoring case and the property
	 * actually supports it. Note: The method is modelled after {@link #setNestedAttributes(Part, Filter)} to have some
	 * consistency. Preferable it should return a new filter.
	 *
	 * @param part
	 * @param filter
	 */
	private void applyCaseInsensitivityIfShouldIgnoreCase(Part part, Filter filter) {

		switch (part.shouldIgnoreCase()) {
			case ALWAYS:
				Assert.state(canIgnoreCase(part), "Unable to ignore case of " + part.getProperty().getLeafType().getName()
						+ " types, the property '" + part.getProperty().getSegment() + "' must reference a String");
				filter.ignoreCase();
				break;
			case WHEN_POSSIBLE:
				if (canIgnoreCase(part)) {
					filter.ignoreCase();
				}
				break;
			case NEVER:
			default:
				break;
		}
	}

	private boolean canIgnoreCase(Part part) {
		return part.getType() == SIMPLE_PROPERTY && String.class.equals(part.getProperty().getLeafType());
	}
}
