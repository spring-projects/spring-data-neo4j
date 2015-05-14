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

import org.neo4j.ogm.model.Property;

/**
 * @author Luanne Misquitta
 */
public class Parameter {

	private Property<String,Integer> property;
	private String comparisonOperator; //TODO fix datatype
	private String booleanOperator; //TODO fix datatype

	public Parameter(Property<String, Integer> property, String comparisonOperator, String booleanOperator) {
		this.property = property;
		this.comparisonOperator = comparisonOperator;
		this.booleanOperator = booleanOperator;
	}

	public Property<String, Integer> getProperty() {
		return property;
	}

	public String getComparisonOperator() {
		return comparisonOperator;
	}

	public String getBooleanOperator() {
		return booleanOperator;
	}
}
