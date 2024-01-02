/*
 * Copyright 2011-2024 the original author or authors.
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

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Michael J. Simons
 */
@Node
public class DoritoEatingPerson {

	@Id
	private long id;

	@Property
	private String name;

	@Property
	private boolean eatsDoritos;

	@Property
	private boolean friendsAlsoEatDoritos;

	@Relationship
	private Set<DoritoEatingPerson> friends = new HashSet<>();

	public DoritoEatingPerson(String name) {
		this.name = name;
	}

	public DoritoEatingPerson(long id, String name, boolean eatsDoritos, boolean friendsAlsoEatDoritos, Set<DoritoEatingPerson> friends) {
		this.id = id;
		this.name = name;
		this.eatsDoritos = eatsDoritos;
		this.friendsAlsoEatDoritos = friendsAlsoEatDoritos;
		this.friends = friends;
	}

	public DoritoEatingPerson() {
	}

	public long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public boolean isEatsDoritos() {
		return this.eatsDoritos;
	}

	public boolean isFriendsAlsoEatDoritos() {
		return this.friendsAlsoEatDoritos;
	}

	public Set<DoritoEatingPerson> getFriends() {
		return this.friends;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEatsDoritos(boolean eatsDoritos) {
		this.eatsDoritos = eatsDoritos;
	}

	public void setFriendsAlsoEatDoritos(boolean friendsAlsoEatDoritos) {
		this.friendsAlsoEatDoritos = friendsAlsoEatDoritos;
	}

	public void setFriends(Set<DoritoEatingPerson> friends) {
		this.friends = friends;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof DoritoEatingPerson)) {
			return false;
		}
		final DoritoEatingPerson other = (DoritoEatingPerson) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		if (this.getId() != other.getId()) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		if (this.isEatsDoritos() != other.isEatsDoritos()) {
			return false;
		}
		if (this.isFriendsAlsoEatDoritos() != other.isFriendsAlsoEatDoritos()) {
			return false;
		}
		final Object this$friends = this.getFriends();
		final Object other$friends = other.getFriends();
		if (this$friends == null ? other$friends != null : !this$friends.equals(other$friends)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof DoritoEatingPerson;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final long $id = this.getId();
		result = result * PRIME + (int) ($id >>> 32 ^ $id);
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		result = result * PRIME + (this.isEatsDoritos() ? 79 : 97);
		result = result * PRIME + (this.isFriendsAlsoEatDoritos() ? 79 : 97);
		final Object $friends = this.getFriends();
		result = result * PRIME + ($friends == null ? 43 : $friends.hashCode());
		return result;
	}

	public String toString() {
		return "DoritoEatingPerson(id=" + this.getId() + ", name=" + this.getName() + ", eatsDoritos=" + this.isEatsDoritos() + ", friendsAlsoEatDoritos=" + this.isFriendsAlsoEatDoritos() + ")";
	}

	/**
	 * Projection containing ambiguous name
	 */
	public interface PropertiesProjection1 {

		boolean getEatsDoritos();

		boolean getFriendsAlsoEatDoritos();
	}

	/**
	 * Projection not containing ambiguous name
	 */
	public interface PropertiesProjection2 {

		boolean getEatsDoritos();
	}
}
