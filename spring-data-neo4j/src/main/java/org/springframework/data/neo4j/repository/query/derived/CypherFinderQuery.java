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

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Parameter;
import org.springframework.data.neo4j.mapping.Neo4jMappingContext;
import org.springframework.data.repository.query.parser.Part;

/**
 * A {@link DerivedQueryDefinition} that builds a Cypher query.
 *
 * @author Luanne Misquitta
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

	private final Neo4jMappingContext mappingContext;
	private Class entityType;
	private Part basePart;
	private List<Parameter> parameters = new ArrayList<>();
	private int paramPosition = 0;

	public CypherFinderQuery(Class entityType, Part basePart, Neo4jMappingContext mappingContext) {
		this.entityType = entityType;
		this.basePart = basePart;
		this.mappingContext = mappingContext;
	}


	@Override
	public Part getBasePart() { //because the OR is handled in a weird way. Luanne, explain better
		return basePart;
	}

	@Override
	public List<Parameter> getQueryParameters() {
		return parameters;
	}

	@Override
	public void addPart(Part part, BooleanOperator booleanOperator) {
		String property = part.getProperty().getSegment();

		Parameter parameter = new Parameter();
		parameter.setPropertyPosition(paramPosition++);
		parameter.setPropertyName(property);
		parameter.setComparisonOperator(convertToComparisonOperator(part.getType()));
		parameter.setBooleanOperator(booleanOperator);
		parameters.add(parameter);
	}


	private ComparisonOperator convertToComparisonOperator(Part.Type type) {
		switch (type) {
			case GREATER_THAN:
				return ComparisonOperator.GREATER_THAN;
			case LESS_THAN:
				return ComparisonOperator.LESS_THAN;
			default:
				return ComparisonOperator.EQUALS;
		}
	}
}
