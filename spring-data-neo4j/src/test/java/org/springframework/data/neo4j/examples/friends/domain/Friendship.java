/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.examples.friends.domain;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.GeneratedValue;
import org.neo4j.ogm.annotation.Id;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Luanne Misquitta
 */
@RelationshipEntity(type = "IS_FRIEND")
public class Friendship {

	@Id @GeneratedValue private Long id;

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
