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
package org.springframework.data.neo4j.integration.issues.gh2474;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

import java.util.UUID;

/**
 * @author Stephen Jackson
 */
@Node
public class PersonModel {
	@Id
	@GeneratedValue(generatorClass = GeneratedValue.UUIDGenerator.class)
	private UUID personId;

	private String address;
	private String name;
	private String favoriteFood;

	public PersonModel() {
	}

	public UUID getPersonId() {
		return this.personId;
	}

	public String getAddress() {
		return this.address;
	}

	public String getName() {
		return this.name;
	}

	public String getFavoriteFood() {
		return this.favoriteFood;
	}

	public void setPersonId(UUID personId) {
		this.personId = personId;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setFavoriteFood(String favoriteFood) {
		this.favoriteFood = favoriteFood;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PersonModel)) {
			return false;
		}
		final PersonModel other = (PersonModel) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$personId = this.getPersonId();
		final Object other$personId = other.getPersonId();
		if (this$personId == null ? other$personId != null : !this$personId.equals(other$personId)) {
			return false;
		}
		final Object this$address = this.getAddress();
		final Object other$address = other.getAddress();
		if (this$address == null ? other$address != null : !this$address.equals(other$address)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$favoriteFood = this.getFavoriteFood();
		final Object other$favoriteFood = other.getFavoriteFood();
		if (this$favoriteFood == null ? other$favoriteFood != null : !this$favoriteFood.equals(other$favoriteFood)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof PersonModel;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $personId = this.getPersonId();
		result = result * PRIME + ($personId == null ? 43 : $personId.hashCode());
		final Object $address = this.getAddress();
		result = result * PRIME + ($address == null ? 43 : $address.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $favoriteFood = this.getFavoriteFood();
		result = result * PRIME + ($favoriteFood == null ? 43 : $favoriteFood.hashCode());
		return result;
	}

	public String toString() {
		return "PersonModel(personId=" + this.getPersonId() + ", address=" + this.getAddress() + ", name=" + this.getName() + ", favoriteFood=" + this.getFavoriteFood() + ")";
	}
}
