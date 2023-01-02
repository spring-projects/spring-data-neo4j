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

import java.util.List;
import java.util.Set;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 * @author Philipp Tölle
 * @author Michael J. Simons
 */
@Node
public class PersonWithRelationshipWithProperties {

	@Id @GeneratedValue private Long id;

	private final String name;

	@Relationship("LIKES") private final List<LikesHobbyRelationship> hobbies;

	@Relationship("WORKS_IN") private final WorksInClubRelationship club;

	@Relationship("OWNS") private Set<Pet> pets;

	@Relationship("OWNS") private List<ClubRelationship> clubs;

	@PersistenceCreator
	public PersonWithRelationshipWithProperties(Long id, String name, List<LikesHobbyRelationship> hobbies, WorksInClubRelationship club) {
		this.id = id;
		this.name = name;
		this.hobbies = hobbies;
		this.club = club;
	}

	public PersonWithRelationshipWithProperties(String name, List<LikesHobbyRelationship> hobbies, WorksInClubRelationship club) {
		this.name = name;
		this.hobbies = hobbies;
		this.club = club;
	}

	public PersonWithRelationshipWithProperties withId(Long newId) {
		return new PersonWithRelationshipWithProperties(newId, this.name, this.hobbies, this.club);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<LikesHobbyRelationship> getHobbies() {
		return hobbies;
	}

	public WorksInClubRelationship getClub() {
		return club;
	}

	public Set<Pet> getPets() {
		return pets;
	}

	public List<ClubRelationship> getClubs() {
		return clubs;
	}
}
