/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.integration.shared;

import java.util.Map;
import java.util.Set;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Relationship;

/**
 * @author Gerrit Meier
 * @author Philipp Tölle
 */
@Node
public class PersonWithRelationshipWithProperties {

	@Id @GeneratedValue private Long id;

	private final String name;

	@Relationship("LIKES")
	private Map<Hobby, LikesHobbyRelationship> hobbies;

	@Relationship("OWNS")
	private Set<Pet> pets;

	public PersonWithRelationshipWithProperties(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Map<Hobby, LikesHobbyRelationship> getHobbies() {
		return hobbies;
	}

	public void setHobbies(Map<Hobby, LikesHobbyRelationship> hobbies) {
		this.hobbies = hobbies;
	}
}
