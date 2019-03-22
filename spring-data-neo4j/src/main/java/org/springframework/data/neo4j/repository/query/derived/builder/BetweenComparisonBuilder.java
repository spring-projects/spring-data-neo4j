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
 */
public class BetweenComparisonBuilder extends FilterBuilder {

	public BetweenComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		Filter gt = new Filter();
		gt.setPropertyName(propertyName());
		gt.setOwnerEntityType(entityType);
		gt.setBooleanOperator(booleanOperator);
		gt.setNegated(isNegated());
		gt.setComparisonOperator(ComparisonOperator.GREATER_THAN);
		gt.setFunction(new PropertyComparison(params.pop()));
		setNestedAttributes(part, gt);

		Filter lt = new Filter();
		lt.setPropertyName(propertyName());
		lt.setOwnerEntityType(entityType);
		lt.setBooleanOperator(BooleanOperator.AND);
		lt.setNegated(isNegated());
		lt.setComparisonOperator(ComparisonOperator.LESS_THAN);
		lt.setFunction(new PropertyComparison(params.pop()));
		setNestedAttributes(part, lt);

		return Arrays.asList(gt, lt);
	}
}
