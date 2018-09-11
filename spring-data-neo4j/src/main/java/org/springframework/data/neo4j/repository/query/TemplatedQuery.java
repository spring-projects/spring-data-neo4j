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
package org.springframework.data.neo4j.repository.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.Filters;
import org.springframework.data.neo4j.repository.query.filter.FilterBuilder;

/**
 * A template query based on filters. {@link #createExecutableQuery(Map)} is used to create an executable query from
 * resolved parameters.
 *
 * @author Luanne Misquitta
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class TemplatedQuery {

	private final List<FilterBuilder> filterBuilders;

	TemplatedQuery(List<FilterBuilder> filterBuilders) {
		this.filterBuilders = filterBuilders;
	}

	Query createExecutableQuery(Map<Integer, Object> resolvedParameters) {

		// building a stack of parameter values, so that the builders can pull them
		// according to their needs (zero, one or more parameters)
		// avoids to manage a current parameter index state here.
		Stack<Object> parametersStack = new Stack<>();
		if (!resolvedParameters.isEmpty()) {
			Integer maxParameterIndex = Collections.max(resolvedParameters.keySet());
			for (int i = 0; i <= maxParameterIndex; i++) {
				parametersStack.add(0, resolvedParameters.get(i));
			}
		}

		List<Filter> filters = new ArrayList<>();
		for (FilterBuilder filterBuilder : filterBuilders) {
			filters.addAll(filterBuilder.build(parametersStack));
		}

		return new Query(new Filters(filters));
	}
}
