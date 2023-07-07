/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @@author Michael J. Simons
 */
@Node
public class AltHobby {
	@Id
	@GeneratedValue
	private Long id;

	private String name;

	@Relationship(type = "LIKES", direction = Relationship.Direction.INCOMING)
	private List<AltLikedByPersonRelationship> likedBy = new ArrayList<>();

	@Relationship(type = "CHILD", direction = Relationship.Direction.INCOMING)
	private List<AltHobby> memberOf = new ArrayList<>();

	public List<AltHobby> getMemberOf() {
		return memberOf;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<AltLikedByPersonRelationship> getLikedBy() {
		return likedBy;
	}
}
