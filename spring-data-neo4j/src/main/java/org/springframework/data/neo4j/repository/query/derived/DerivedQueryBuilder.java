/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.query.derived;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Luanne Misquitta
 */
public class DerivedQueryBuilder {

	private DerivedQueryDefinition query;

	public DerivedQueryBuilder(Class entityType, Part basePart) {
		query = new CypherFinderQuery(entityType, basePart);
	}

	public void addPart(Part part, BooleanOperator booleanOperator) {
		query.addPart(part, booleanOperator);
	}

	public void addPart(DerivedQueryBuilder fromBuilder, BooleanOperator booleanOperator) {
		query.addPart(fromBuilder.query.getBasePart(),booleanOperator);
	}

	public DerivedQueryDefinition buildQuery() {
		return query;
	}

}
