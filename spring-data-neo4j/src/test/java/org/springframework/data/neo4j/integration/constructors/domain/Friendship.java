/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
