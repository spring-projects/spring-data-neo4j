/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.ContainsAnyComparison;
import org.springframework.data.repository.query.parser.Part;

/**
 * Filter for entities having a collection like property (not) containing a given element.
 *
 * @author Gerrit Meier
 */
public class ContainsComparisonBuilder extends FilterBuilder {

	public ContainsComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		final Object containingValue = params.pop();
		Filter containingFilter = new Filter(propertyName(), ComparisonOperator.IN, containingValue);
		containingFilter.setOwnerEntityType(entityType);
		containingFilter.setBooleanOperator(booleanOperator);
		containingFilter.setNegated(isNegated());
		containingFilter.setFunction(new ContainsAnyComparison(containingValue));
		setNestedAttributes(part, containingFilter);

		return Collections.singletonList(containingFilter);
	}

}
