/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.query.derived;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.neo4j.repository.query.derived.builder.BetweenComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.BooleanComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.DistanceComparisonBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.ExistsFilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.FilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.IsNullFilterBuilder;
import org.springframework.data.neo4j.repository.query.derived.builder.PropertyComparisonBuilder;
import org.springframework.data.repository.query.parser.Part;

/**
 * A {@link DerivedQueryDefinition} that builds a Cypher query.
 *
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

	private Class<?> entityType;
	private Part basePart;
	private List<FilterBuilder> filterBuilders = new ArrayList<>();

	CypherFinderQuery(Class<?> entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
	}

	@Override
	public Part getBasePart() { // because the OR is handled in a weird way. Luanne, explain better
		return basePart;
	}

	@Override
	public List<Filter> getFilters(Map<Integer, Object> params) {

		// buiding a stack of parameter values, so that the builders can pull them
		// according to their needs (zero, one or more parameters)
		// avoids to manage a current parameter index state here.
		Stack<Object> parametersStack = new Stack<>();
		if (!params.isEmpty()) {
			Integer maxParameterIndex = Collections.max(params.keySet());
			for (int i = 0; i <= maxParameterIndex; i++) {
				parametersStack.add(0, params.get(i));
			}
		}

		List<Filter> filters = new ArrayList<>();
		for (FilterBuilder filterBuilder : filterBuilders) {
			filters.addAll(filterBuilder.build(parametersStack));
		}
		return filters;
	}

	@Override
	public void addPart(Part part, BooleanOperator booleanOperator) {

		FilterBuilder builder = builderForPart(part, booleanOperator);
		filterBuilders.add(builder);
	}

	private FilterBuilder builderForPart(Part part, BooleanOperator booleanOperator) {
		switch (part.getType()) {
			case NEAR:
				return new DistanceComparisonBuilder(part, booleanOperator, entityType);
			case BETWEEN:
				return new BetweenComparisonBuilder(part, booleanOperator, entityType);
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
}
