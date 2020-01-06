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

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.ContainsAnyComparison;
import org.springframework.data.repository.query.parser.Part;

/**
 * Filter for entities having a collection like property (not) containing a given element.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class ContainsComparisonBuilder extends FilterBuilder {

	ContainsComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {

		NestedAttributes nestedAttributes = getNestedAttributes(part);

		final Object containingValue = params.pop();
		Filter containingFilter = new Filter(
				nestedAttributes.isEmpty() ? propertyName() : nestedAttributes.getLeafPropertySegment(),
				new ContainsAnyComparison(containingValue));
		containingFilter.setOwnerEntityType(entityType);
		containingFilter.setBooleanOperator(booleanOperator);
		containingFilter.setNegated(isNegated());
		containingFilter.setNestedPath(nestedAttributes.getSegments());

		return Collections.singletonList(containingFilter);
	}
}
