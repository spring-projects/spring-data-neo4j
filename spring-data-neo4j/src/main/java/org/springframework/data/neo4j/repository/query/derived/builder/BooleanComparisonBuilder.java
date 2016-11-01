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


package org.springframework.data.neo4j.repository.query.derived.builder;

import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 */
public class BooleanComparisonBuilder extends CypherFilterBuilder {

	public BooleanComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<CypherFilter> build() {
		List<CypherFilter> filters = new ArrayList<>();

		CypherFilter filter = new CypherFilter();
		filter.setPropertyName(propertyName());
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated() || part.getType() == FALSE);
		filter.setComparisonOperator(ComparisonOperator.IS_TRUE);
		setNestedAttributes(part, filter);

		filters.add(filter);

		return filters;
	}

}
