/*
 * Copyright 2011-2020 the original author or authors.
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

import static org.neo4j.ogm.cypher.ComparisonOperator.*;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.domain.Range;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class BetweenComparisonBuilder extends FilterBuilder {

	BetweenComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		final Object lowerBoundOrRange = params.pop();

		Object lowerBoundValue;
		Object upperBoundValue;

		boolean inclusiveLowerBound = true;
		boolean inclusiveUpperBound = true;

		if (lowerBoundOrRange instanceof Range) {
			Range range = (Range) lowerBoundOrRange;
			Range.Bound lowerBound = range.getLowerBound();
			lowerBoundValue = lowerBound.getValue();
			inclusiveLowerBound = lowerBound.isInclusive();

			Range.Bound upperBound = range.getUpperBound();
			upperBoundValue = upperBound.getValue();
			inclusiveUpperBound = upperBound.isInclusive();
		} else {
			lowerBoundValue = lowerBoundOrRange;
			upperBoundValue = params.pop();
		}

		NestedAttributes nestedAttributes = getNestedAttributes(part);

		Filter lowerBoundFilter = createLowerBoundFilter(lowerBoundValue, inclusiveLowerBound, nestedAttributes);
		Filter upperBoundFilter = createUpperBoundFilter(upperBoundValue, inclusiveUpperBound, nestedAttributes);

		return Arrays.asList(lowerBoundFilter, upperBoundFilter);
	}

	private Filter createLowerBoundFilter(Object value, boolean inclusive, NestedAttributes nestedAttributes) {
		return createBoundFilter(Bound.LOWER, value, inclusive, booleanOperator, nestedAttributes);
	}

	private Filter createUpperBoundFilter(Object value, boolean inclusive, NestedAttributes nestedAttributes) {
		return createBoundFilter(Bound.UPPER, value, inclusive, BooleanOperator.AND, nestedAttributes);
	}

	private Filter createBoundFilter(Bound bound, Object value, boolean inclusive, BooleanOperator operator,
			NestedAttributes nestedAttributes) {
		Filter filter = new Filter(nestedAttributes.isEmpty() ?
				propertyName() :
				nestedAttributes.getLeafPropertySegment(), deriveComparisonOperator(bound, inclusive), value);
		filter.setOwnerEntityType(entityType);
		filter.setNegated(isNegated());
		filter.setBooleanOperator(operator);
		filter.setNestedPath(nestedAttributes.getSegments());
		return filter;
	}

	private ComparisonOperator deriveComparisonOperator(Bound bound, boolean inclusive) {
		switch (bound) {
			case LOWER:
				return inclusive ? GREATER_THAN_EQUAL : GREATER_THAN;
			case UPPER:
				return inclusive ? LESS_THAN_EQUAL : LESS_THAN;
		}
		throw new IllegalArgumentException("unsupported bound");
	}

	private enum Bound {
		UPPER,
		LOWER
	}
}
