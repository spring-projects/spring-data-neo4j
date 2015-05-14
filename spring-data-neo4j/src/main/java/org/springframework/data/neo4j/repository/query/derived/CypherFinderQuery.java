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

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.model.Property;
import org.springframework.data.neo4j.repository.query.derived.strategy.CypherNodeFinderStatements;
import org.springframework.data.neo4j.repository.query.derived.strategy.FinderStatements;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Luanne Misquitta
 */
public class CypherFinderQuery implements DerivedQueryDefinition {

	private Class entityType;
	private List<Parameter> parameters = new ArrayList<>();
	private int paramPosition = 0;

	public CypherFinderQuery(Class entityType) {
		this.entityType = entityType;
	}

	@Override
	public void addPart(Part part, String booleanOperator) {
		String property = part.getProperty().getSegment();
		try { //todo this is crap. Use the classinfo
			org.neo4j.ogm.annotation.Property propertyAnnotation = entityType.getDeclaredField(property).getAnnotation(org.neo4j.ogm.annotation.Property.class);
			if (propertyAnnotation != null && propertyAnnotation.name() != null && propertyAnnotation.name().length() > 0) {
				property = propertyAnnotation.name();
			}
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Could not find property " + property + " on class " + entityType.getSimpleName() + ". Check spelling or use @Query.");
		}
		parameters.add(new Parameter(new Property<>(property, paramPosition++), "=", booleanOperator)); //todo magic =
	}

	@Override
	public String toQueryString() {
		FinderStatements finder = new CypherNodeFinderStatements(); //todo get the right one depending on whether it's a node or RE
		return finder.findByProperties(getLabelOrType(entityType), parameters);
	}

	private String getLabelOrType(Class entityType) { //todo get rid of this crap and use the metadata
		NodeEntity annotation = (NodeEntity) entityType.getAnnotation(NodeEntity.class);
		if (annotation != null && annotation.label() != null && annotation.label().length() > 0) {
			return annotation.label();
		}
		return entityType.getSimpleName();
	}
}
