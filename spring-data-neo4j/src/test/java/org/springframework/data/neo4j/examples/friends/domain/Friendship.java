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

package org.springframework.data.neo4j.examples.friends.domain;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Luanne Misquitta
 */
@RelationshipEntity(type = "IS_FRIEND")
public class Friendship {

	@GraphId private Long id;

	@StartNode private Person personStartNode;
	@EndNode private Person personEndNode;

	private long timestamp;

	public Friendship() {}

	public Friendship(Person personStartNode, Person personEndNode) {
		this.personStartNode = personStartNode;
		this.personEndNode = personEndNode;
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

	public void setPersonStartNode(Person personStartNode) {
		this.personStartNode = personStartNode;
	}

	public Person getPersonEndNode() {
		return personEndNode;
	}

	public void setPersonEndNode(Person personEndNode) {
		this.personEndNode = personEndNode;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
