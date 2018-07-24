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

package org.springframework.data.neo4j.integration.constructors.domain;

import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.conversion.PointConverter;
import org.springframework.util.Assert;

/**
 * @author Nicolas Mervaillie
 */
@NodeEntity
public class PersonWithCompositeAttribute {

	@Id private String name;

	@Convert(PointConverter.class) private Point location;

	public PersonWithCompositeAttribute(String name, Point location) {
		Assert.notNull(name, "name should not be null");
		Assert.notNull(location, "location should not be null");
		this.name = name;
		this.location = location;
	}

	public String getName() {
		return name;
	}

	public Point getLocation() {
		return location;
	}
}
