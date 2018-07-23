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

import org.neo4j.ogm.cypher.BooleanOperator;
import org.springframework.data.repository.query.parser.Part;

/**
 * The graph query builder.
 *
 * @author Luanne Misquitta
 */
public class DerivedQueryBuilder {

	private DerivedQueryDefinition query;

	public DerivedQueryBuilder(Class<?> entityType, Part basePart) {
		query = new CypherFinderQuery(entityType, basePart);
	}

	/**
	 * Add a part as a parameter to the graph query.
	 *
	 * @param part the Part to be added
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the parameter to the query.
	 */
	public void addPart(Part part, BooleanOperator booleanOperator) {
		query.addPart(part, booleanOperator);
	}

	/**
	 * Add criteria from an intermediate builder to the query
	 *
	 * @param fromBuilder the intermediate builder
	 * @param booleanOperator the {@link BooleanOperator} to be used when appending the criteria to the query
	 */
	public void addPart(DerivedQueryBuilder fromBuilder, BooleanOperator booleanOperator) {
		query.addPart(fromBuilder.query.getBasePart(), booleanOperator);
	}

	/**
	 * Builds the final query
	 *
	 * @return the final query
	 */
	public DerivedQueryDefinition buildQuery() {
		return query;
	}
}
