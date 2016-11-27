/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repository.query.derived.filter;

import java.util.Map;

import org.neo4j.ogm.cypher.function.FilterFunction;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;

/**
 * Adapter to the OGM FilterFunction interface. Adds the derived finder parameter count, and the ability to set the
 * function value from the derived finder argument structure.
 *
 * @author Jasper Blues
 */
public interface FunctionAdapter<T> {

	CypherFilter cypherFilter();

	FilterFunction<T> filterFunction();

	int parameterCount();

	void setValueFromArgs(Map<Integer, Object> params);

}
