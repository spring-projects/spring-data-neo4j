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

package org.springframework.data.neo4j.repository.query.derived;

import java.util.List;
import java.util.Map;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.repository.query.parser.Part;

/**
 * The graph query created based on a derived query. /**
 *
 * @author Luanne Misquitta
 * @author Nicolas Mervaillie
 */
public interface DerivedQueryDefinition {

	/**
	 * Add a part as a parameter to the graph query.
	 *
	 * @param part the Part to be added
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the parameter to the query.
	 */
	void addPart(Part part, BooleanOperator booleanOperator);

	/**
	 * Get the base part i.e. the first parameter of the graph query.
	 *
	 * @return Part representing the base of the query.
	 */
	Part getBasePart();

	/**
	 * Gets all cypher filters for this query
	 *
	 * @return The OGM filters with bound parameter values
	 */
	List<Filter> getFilters(Map<Integer, Object> params);

}
