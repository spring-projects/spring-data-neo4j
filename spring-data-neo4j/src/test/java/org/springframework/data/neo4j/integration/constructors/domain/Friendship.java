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

import java.util.Date;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.annotation.typeconversion.DateLong;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.conversion.PointConverter;
import org.springframework.util.Assert;

/**
 * @author Nicolas Mervaillie
 */
@RelationshipEntity(type = "IS_FRIEND")
public class Friendship {

	@Id @GeneratedValue private Long id;

	@StartNode private Person personStartNode;

	@EndNode private Person personEndNode;

	@DateLong private Date timestamp;

	@Convert(PointConverter.class) private Point location;

	public Friendship(Person personStartNode, Person personEndNode, Date timestamp, Point location) {
		Assert.notNull(timestamp, "Timestamp cannot be null");
		Assert.notNull(location, "Location cannot be null");
		Assert.notNull(personStartNode, "personStartNode cannot be null");
		Assert.notNull(personEndNode, "personEndNode cannot be null");
		// start & end nodes are passed as null
		this.personStartNode = personStartNode;
		this.personEndNode = personEndNode;
		this.timestamp = timestamp;
		this.location = location;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Person getPersonStartNode() {
		return personStartNode;
	}

	public Person getPersonEndNode() {
		return personEndNode;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public Point getLocation() {
		return location;
	}
}
