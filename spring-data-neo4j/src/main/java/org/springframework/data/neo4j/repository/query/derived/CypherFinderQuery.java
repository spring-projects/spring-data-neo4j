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

import static org.springframework.data.repository.query.parser.Part.Type.*;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.function.DistanceComparison;
import org.springframework.data.repository.query.parser.Part;

/**
 * A {@link DerivedQueryDefinition} that builds a Cypher query.
 *
 * @author Luanne Misquitta
 * @author Jasper Blues
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

	private Class<?> entityType;
	private Part basePart;
	private List<CypherFilter> cypherFilters = new ArrayList<>();
	private int paramPosition = 0;

	public CypherFinderQuery(Class<?> entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
	}

	@Override
	public Part getBasePart() { //because the OR is handled in a weird way. Luanne, explain better
		return basePart;
	}

	@Override
	public List<CypherFilter> getCypherFilters() {
		return cypherFilters;
	}

	@Override
	public void addPart(Part part, BooleanOperator booleanOperator) {
		String property = part.getProperty().getSegment();
		CypherFilter parameter = new CypherFilter();
		parameter.setPropertyPosition(paramPosition++);
		parameter.setPropertyName(property);
		parameter.setOwnerEntityType(entityType);
		parameter.setComparisonOperator(convertToComparisonOperator(part.getType()));
		parameter.setNegated(part.getType().name().startsWith("NOT"));
		parameter.setBooleanOperator(booleanOperator);

		if (part.getType() == NEAR) {
			parameter.setFunction(new DistanceComparison());
			parameter.setComparisonOperator(ComparisonOperator.LESS_THAN);
			paramPosition++;
		}

		if (part.getProperty().next() != null) {
			parameter.setOwnerEntityType(part.getProperty().getOwningType().getType());
			parameter.setNestedPropertyType(part.getProperty().getType());
			parameter.setPropertyName(part.getProperty().getLeafProperty().getSegment());
			parameter.setNestedPropertyName(part.getProperty().getSegment());
		}
		cypherFilters.add(parameter);

	}

	private ComparisonOperator convertToComparisonOperator(Part.Type type) {
		switch (type) {
			case GREATER_THAN:
				return ComparisonOperator.GREATER_THAN;
			case LESS_THAN:
				return ComparisonOperator.LESS_THAN;
			case REGEX:
				return ComparisonOperator.MATCHES;
			case LIKE:
				return ComparisonOperator.LIKE;
			case NOT_LIKE:
				return ComparisonOperator.LIKE;
			default:
				return ComparisonOperator.EQUALS;
		}
	}

}
