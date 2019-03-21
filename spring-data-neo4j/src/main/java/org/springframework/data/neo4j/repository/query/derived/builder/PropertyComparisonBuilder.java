/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.PropertyComparison;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class PropertyComparisonBuilder extends FilterBuilder {

	public PropertyComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		Filter filter = new Filter(propertyName(), convertToComparisonOperator(part.getType()), params.peek());
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		filter.setFunction(new PropertyComparison(params.pop()));
		setNestedAttributes(part, filter);

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
}
