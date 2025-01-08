/*
 * Copyright 2011-2025 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Michael J. Simons
 */
@Node
public class PersonWithStringlyTypedRelatives {

	@Id
	@GeneratedValue
	private Long id;

	private final String name;

	private Map<String, Person> relatives = new HashMap<>();

	private Map<String, List<Pet>> pets = new HashMap<>();

	private Map<String, List<HobbyRelationship>> hobbies = new HashMap<>();

	private Map<String, ClubRelationship> clubs = new HashMap<>();

	public PersonWithStringlyTypedRelatives(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public Map<String, Person> getRelatives() {
		return relatives;
	}

	public Map<String, List<Pet>> getPets() {
		return pets;
	}

	public Map<String, List<HobbyRelationship>> getHobbies() {
		return hobbies;
	}

	public void setHobbies(Map<String, List<HobbyRelationship>> hobbies) {
		this.hobbies = hobbies;
	}

	public Map<String, ClubRelationship> getClubs() {
		return clubs;
	}

	public void setClubs(Map<String, ClubRelationship> clubs) {
		this.clubs = clubs;
	}
}
