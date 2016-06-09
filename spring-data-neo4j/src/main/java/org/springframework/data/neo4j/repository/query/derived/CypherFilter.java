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
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;

/**
 * A representation of a Neo4j-OGM Filter that contains no parameter/property values and only holds metadata
 * @author Luanne Misquitta
 */
public class CypherFilter {

	Integer propertyPosition;
	String propertyName;
	Class ownerEntityType;
	ComparisonOperator comparisonOperator;
	boolean negated;
	BooleanOperator booleanOperator;
	Class nestedPropertyType;
	String nestedPropertyName;

	public Integer getPropertyPosition() {
		return propertyPosition;
	}

	public void setPropertyPosition(Integer propertyPosition) {
		this.propertyPosition = propertyPosition;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public Class getOwnerEntityType() {
		return ownerEntityType;
	}

	public void setOwnerEntityType(Class ownerEntityType) {
		this.ownerEntityType = ownerEntityType;
	}

	public ComparisonOperator getComparisonOperator() {
		return comparisonOperator;
	}

	public void setComparisonOperator(ComparisonOperator comparisonOperator) {
		this.comparisonOperator = comparisonOperator;
	}

	public boolean isNegated() {
		return negated;
	}

	public void setNegated(boolean negated) {
		this.negated = negated;
	}

	public BooleanOperator getBooleanOperator() {
		return booleanOperator;
	}

	public void setBooleanOperator(BooleanOperator booleanOperator) {
		this.booleanOperator = booleanOperator;
	}

	public Class getNestedPropertyType() {
		return nestedPropertyType;
	}

	public void setNestedPropertyType(Class nestedPropertyType) {
		this.nestedPropertyType = nestedPropertyType;
	}

	public String getNestedPropertyName() {
		return nestedPropertyName;
	}

	public void setNestedPropertyName(String nestedPropertyName) {
		this.nestedPropertyName = nestedPropertyName;
	}

	Filter toFilter() {
		Filter filter = new Filter();
		filter.setPropertyPosition(propertyPosition);
		filter.setPropertyName(propertyName);
		filter.setOwnerEntityType(ownerEntityType);
		filter.setComparisonOperator(comparisonOperator);
		filter.setNegated(negated);
		filter.setBooleanOperator(booleanOperator);
		filter.setNestedPropertyType(nestedPropertyType);
		filter.setNestedPropertyName(nestedPropertyName);
		return filter;
	}
}
