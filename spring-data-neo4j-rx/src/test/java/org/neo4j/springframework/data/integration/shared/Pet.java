/*
 * Copyright (c) 2019-2020 "Neo4j,"
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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Set;

import org.neo4j.springframework.data.core.schema.GeneratedValue;
import org.neo4j.springframework.data.core.schema.Id;
import org.neo4j.springframework.data.core.schema.Node;
import org.neo4j.springframework.data.core.schema.Relationship;
import org.springframework.data.annotation.PersistenceConstructor;

/**
 * @author Gerrit Meier
 */
@RequiredArgsConstructor(onConstructor = @__(@PersistenceConstructor))
@Getter
@EqualsAndHashCode(of = {"id", "name"})
@Node
public class Pet {

	@Id @GeneratedValue private Long id;

	private final String name;

	public Pet(long id, String name) {
		this(name);
		this.id = id;
	}

	@Relationship("Has")
	private Set<Hobby> hobbies;

	@Relationship("Has")
	private List<Pet> friends;

	@Relationship("Has")
	private List<ThingWithAssignedId> things;

	public Set<Hobby> getHobbies() {
		return hobbies;
	}

	public void setHobbies(Set<Hobby> hobbies) {
		this.hobbies = hobbies;
	}

	public List<Pet> getFriends() {
		return friends;
	}

	public void setFriends(List<Pet> friends) {
		this.friends = friends;
	}

	public Long getId() {
		return id;
	}
}


