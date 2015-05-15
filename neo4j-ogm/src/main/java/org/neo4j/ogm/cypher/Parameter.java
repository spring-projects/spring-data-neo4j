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

package org.neo4j.ogm.cypher;

/**
 * @author Luanne Misquitta
 */
public class Parameter {

	private String propertyName;
	private Object propertyValue;
	private Integer propertyPosition;
	private String comparisonOperator = "="; //TODO fix datatype
	private String booleanOperator = null; //TODO fix datatype

	public Parameter() {
	}

	public Parameter(String propertyName, Object propertyValue) {
		this.propertyName = propertyName;
		this.propertyValue = propertyValue;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	public Object getPropertyValue() {
		return propertyValue;
	}

	public void setPropertyValue(Object propertyValue) {
		this.propertyValue = propertyValue;
	}

	public Integer getPropertyPosition() {
		return propertyPosition;
	}

	public void setPropertyPosition(Integer propertyPosition) {
		this.propertyPosition = propertyPosition;
	}

	public String getComparisonOperator() {
		return comparisonOperator;
	}

	public void setComparisonOperator(String comparisonOperator) {
		this.comparisonOperator = comparisonOperator;
	}

	public String getBooleanOperator() {
		return booleanOperator;
	}

	public void setBooleanOperator(String booleanOperator) {
		this.booleanOperator = booleanOperator;
	}
}
