/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repository.query.filter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public abstract class FilterBuilder {

	protected Part part;
	protected BooleanOperator booleanOperator;
	protected Class<?> entityType;

	public static FilterBuilder forPartAndEntity(Part part, Class<?> entityType, BooleanOperator booleanOperator) {
		switch (part.getType()) {
			case NEAR:
				return new DistanceComparisonBuilder(part, booleanOperator, entityType);
			case BETWEEN:
				return new BetweenComparisonBuilder(part, booleanOperator, entityType);
			case NOT_CONTAINING:
			case CONTAINING:
				return resolveMatchingContainsFilterBuilder(part, entityType, booleanOperator);
			case IS_NULL:
			case IS_NOT_NULL:
				return new IsNullFilterBuilder(part, booleanOperator, entityType);
			case EXISTS:
				return new ExistsFilterBuilder(part, booleanOperator, entityType);
			case TRUE:
			case FALSE:
				return new BooleanComparisonBuilder(part, booleanOperator, entityType);
			default:
				return new PropertyComparisonBuilder(part, booleanOperator, entityType);
		}
	}

	FilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		this.part = part;
		this.booleanOperator = booleanOperator;
		this.entityType = entityType;
	}

	public abstract List<Filter> build(Stack<Object> params);

	boolean isNegated() {
		return part.getType().name().startsWith("NOT");
	}

	protected String propertyName() {
		return part.getProperty().getSegment();
	}

	protected void setNestedAttributes(Part part, Filter filter) {
		List<Filter.NestedPathSegment> segments = new ArrayList<>();
		PropertyPath property = part.getProperty();
		if (property.hasNext()) {
			filter.setOwnerEntityType(property.getOwningType().getType());
			segments.add(new Filter.NestedPathSegment(property.getSegment(), property.getType()));
			segments.addAll(deepNestedProperty(property));
			filter.setPropertyName(property.getLeafProperty().getSegment());
			filter.setNestedPath(segments.toArray(new Filter.NestedPathSegment[0]));
		}

	}

	private List<Filter.NestedPathSegment> deepNestedProperty(PropertyPath path) {
		List<Filter.NestedPathSegment> segments = new ArrayList<>();
		if (path.hasNext()) {
			PropertyPath next = path.next();
			if (!next.equals(next.getLeafProperty())) {
				segments.add(new Filter.NestedPathSegment(next.getSegment(), next.getType()));
				segments.addAll(deepNestedProperty(next));
			}
		}
		return segments;
	}

	private static FilterBuilder resolveMatchingContainsFilterBuilder(Part part, Class<?> entityType,
			BooleanOperator booleanOperator) {
		boolean usePropertyComparison = !part.getProperty().getTypeInformation().isCollectionLike();
		if (usePropertyComparison) {
			return new PropertyComparisonBuilder(part, booleanOperator, entityType);
		}
		return new ContainsComparisonBuilder(part, booleanOperator, entityType);
	}
}
