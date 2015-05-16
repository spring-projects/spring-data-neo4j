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
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Luanne Misquitta
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

	private Class entityType;
	private Part basePart;
	private List<Parameter> parameters = new ArrayList<>();
	private int paramPosition = 0;

	public CypherFinderQuery(Class entityType, Part basePart) {
		this.entityType = entityType;
		this.basePart = basePart;
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
		try { //todo this is crap. Use the classinfo
			org.neo4j.ogm.annotation.Property propertyAnnotation = entityType.getDeclaredField(property).getAnnotation(org.neo4j.ogm.annotation.Property.class);
			if (propertyAnnotation != null && propertyAnnotation.name() != null && propertyAnnotation.name().length() > 0) {
				property = propertyAnnotation.name();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Could not find property " + property + " on class " + entityType.getSimpleName() + ". Check spelling or use @Query.");
		}
		Parameter parameter = new Parameter();
		parameter.setPropertyPosition(paramPosition++);
		parameter.setPropertyName(property);
		parameter.setComparisonOperator(convertToComparisonOperator(part.getType()));
		parameter.setBooleanOperator(booleanOperator);
		parameters.add(parameter);
	}


	private ComparisonOperator convertToComparisonOperator(Part.Type type) {
		switch (type) {
			case GREATER_THAN: return ComparisonOperator.GREATER_THAN;
			case LESS_THAN: return ComparisonOperator.LESS_THAN;
			default: return ComparisonOperator.EQUALS;
		}
	}
}
