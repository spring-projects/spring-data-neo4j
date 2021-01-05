/*
 * Copyright 2011-2021 the original author or authors.
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
class PropertyComparisonBuilder extends FilterBuilder {

	PropertyComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		NestedAttributes nestedAttributes = getNestedAttributes(part);

		Object value = params.pop();

		Filter filter;
		String propertyName = nestedAttributes.isEmpty() ? propertyName() : nestedAttributes.getLeafPropertySegment();
		if (isInternalIdProperty.test(part)) {
			filter = new Filter(new NativeIdFilterFunction(convertToComparisonOperator(part.getType()), value));
			filter.setPropertyName(propertyName);
		} else {
			filter = new Filter(propertyName, convertToComparisonOperator(part.getType()), value);
		}
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		filter.setNestedPath(nestedAttributes.getSegments());
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
	 * actually supports it.
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
		return isSupportedIgnoreKeyword(part) && String.class.equals(part.getProperty().getLeafType());
	}

	private boolean isSupportedIgnoreKeyword(Part part) {
		Part.Type type = part.getType();
		return type == SIMPLE_PROPERTY || type == CONTAINING;
	}
}
