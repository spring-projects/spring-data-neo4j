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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

/**
 * @author Gerrit Meier
 */
@Node
public class Pet {

	private final String name;

	@Id
	@GeneratedValue
	private Long id;

	@Relationship("Has")
	private Set<Hobby> hobbies;

	@Relationship("Has")
	private List<Pet> friends;

	@Relationship(value = "Hated_by", direction = Relationship.Direction.INCOMING)
	private List<Pet> otherPets;

	@Relationship("Has")
	private List<ThingWithAssignedId> things;

	public Pet(long id, String name) {
		this(name);
		this.id = id;
	}

	@PersistenceCreator
	public Pet(String name) {
		this.name = name;
	}

	public Set<Hobby> getHobbies() {
		return this.hobbies;
	}

	public void setHobbies(Set<Hobby> hobbies) {
		this.hobbies = hobbies;
	}

	public List<Pet> getFriends() {
		return this.friends;
	}

	public void setFriends(List<Pet> friends) {
		this.friends = friends;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public List<Pet> getOtherPets() {
		return this.otherPets;
	}

	public List<ThingWithAssignedId> getThings() {
		return this.things;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof Pet;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Pet)) {
			return false;
		}
		final Pet other = (Pet) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		return Objects.equals(this$name, other$name);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
		final Object $name = this.getName();
		result = (result * PRIME) + (($name != null) ? $name.hashCode() : 43);
		return result;
	}

}
