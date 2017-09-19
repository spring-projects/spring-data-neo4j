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

import java.util.Arrays;
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
 * @author Gerrit Meier
 */
public class BetweenComparisonBuilder extends FilterBuilder {

	public BetweenComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		final Object value1 = params.pop();
		Filter gt = new Filter(propertyName(), ComparisonOperator.GREATER_THAN_EQUAL, value1);
		gt.setOwnerEntityType(entityType);
		gt.setBooleanOperator(booleanOperator);
		gt.setNegated(isNegated());
		gt.setFunction(new PropertyComparison(value1));
		setNestedAttributes(part, gt);

		final Object value2 = params.pop();
		Filter lt = new Filter(propertyName(), ComparisonOperator.LESS_THAN_EQUAL, value2);
		lt.setOwnerEntityType(entityType);
		lt.setBooleanOperator(BooleanOperator.AND);
		lt.setNegated(isNegated());
		lt.setFunction(new PropertyComparison(value2));
		setNestedAttributes(part, lt);

		return Arrays.asList(gt, lt);
	}
}
