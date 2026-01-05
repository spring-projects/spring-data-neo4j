/*
 * Copyright 2011-present the original author or authors.
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

import org.neo4j.driver.types.Point;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@SuppressWarnings("HiddenField")
@Node
public class PersonWithAllConstructor {

	@Id
	@GeneratedValue
	private final Long id;

	private final String name;

	@Property("first_name")
	private String firstName;

	private final String sameValue;

	private final Boolean cool;

	private final Long personNumber;

	private final LocalDate bornOn;

	private String nullable;

	private List<String> things;

	private final Point place;

	private final Instant createdAt;

	public PersonWithAllConstructor(Long id, String name, String firstName, String sameValue, Boolean cool, Long personNumber, LocalDate bornOn, String nullable, List<String> things, Point place, Instant createdAt) {
		this.id = id;
		this.name = name;
		this.firstName = firstName;
		this.sameValue = sameValue;
		this.cool = cool;
		this.personNumber = personNumber;
		this.bornOn = bornOn;
		this.nullable = nullable;
		this.things = things;
		this.place = place;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getSameValue() {
		return this.sameValue;
	}

	public Boolean getCool() {
		return this.cool;
	}

	public Long getPersonNumber() {
		return this.personNumber;
	}

	public LocalDate getBornOn() {
		return this.bornOn;
	}

	public String getNullable() {
		return this.nullable;
	}

	public List<String> getThings() {
		return this.things;
	}

	public Point getPlace() {
		return this.place;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setNullable(String nullable) {
		this.nullable = nullable;
	}

	public void setThings(List<String> things) {
		this.things = things;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PersonWithAllConstructor)) {
			return false;
		}
		final PersonWithAllConstructor other = (PersonWithAllConstructor) o;
		if (!other.canEqual((Object) this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$firstName = this.getFirstName();
		final Object other$firstName = other.getFirstName();
		if (this$firstName == null ? other$firstName != null : !this$firstName.equals(other$firstName)) {
			return false;
		}
		final Object this$sameValue = this.getSameValue();
		final Object other$sameValue = other.getSameValue();
		if (this$sameValue == null ? other$sameValue != null : !this$sameValue.equals(other$sameValue)) {
			return false;
		}
		final Object this$cool = this.getCool();
		final Object other$cool = other.getCool();
		if (this$cool == null ? other$cool != null : !this$cool.equals(other$cool)) {
			return false;
		}
		final Object this$personNumber = this.getPersonNumber();
		final Object other$personNumber = other.getPersonNumber();
		if (this$personNumber == null ? other$personNumber != null : !this$personNumber.equals(other$personNumber)) {
			return false;
		}
		final Object this$bornOn = this.getBornOn();
		final Object other$bornOn = other.getBornOn();
		if (this$bornOn == null ? other$bornOn != null : !this$bornOn.equals(other$bornOn)) {
			return false;
		}
		final Object this$nullable = this.getNullable();
		final Object other$nullable = other.getNullable();
		if (this$nullable == null ? other$nullable != null : !this$nullable.equals(other$nullable)) {
			return false;
		}
		final Object this$things = this.getThings();
		final Object other$things = other.getThings();
		if (this$things == null ? other$things != null : !this$things.equals(other$things)) {
			return false;
		}
		final Object this$place = this.getPlace();
		final Object other$place = other.getPlace();
		if (this$place == null ? other$place != null : !this$place.equals(other$place)) {
			return false;
		}
		final Object this$createdAt = this.getCreatedAt();
		final Object other$createdAt = other.getCreatedAt();
		if (this$createdAt == null ? other$createdAt != null : !this$createdAt.equals(other$createdAt)) {
			return false;
		}
		return true;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof PersonWithAllConstructor;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + ($id == null ? 43 : $id.hashCode());
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $firstName = this.getFirstName();
		result = result * PRIME + ($firstName == null ? 43 : $firstName.hashCode());
		final Object $sameValue = this.getSameValue();
		result = result * PRIME + ($sameValue == null ? 43 : $sameValue.hashCode());
		final Object $cool = this.getCool();
		result = result * PRIME + ($cool == null ? 43 : $cool.hashCode());
		final Object $personNumber = this.getPersonNumber();
		result = result * PRIME + ($personNumber == null ? 43 : $personNumber.hashCode());
		final Object $bornOn = this.getBornOn();
		result = result * PRIME + ($bornOn == null ? 43 : $bornOn.hashCode());
		final Object $nullable = this.getNullable();
		result = result * PRIME + ($nullable == null ? 43 : $nullable.hashCode());
		final Object $things = this.getThings();
		result = result * PRIME + ($things == null ? 43 : $things.hashCode());
		final Object $place = this.getPlace();
		result = result * PRIME + ($place == null ? 43 : $place.hashCode());
		final Object $createdAt = this.getCreatedAt();
		result = result * PRIME + ($createdAt == null ? 43 : $createdAt.hashCode());
		return result;
	}

	public String toString() {
		return "PersonWithAllConstructor(id=" + this.getId() + ", name=" + this.getName() + ", firstName=" + this.getFirstName() + ", sameValue=" + this.getSameValue() + ", cool=" + this.getCool() + ", personNumber=" + this.getPersonNumber() + ", bornOn=" + this.getBornOn() + ", nullable=" + this.getNullable() + ", things=" + this.getThings() + ", place=" + this.getPlace() + ", createdAt=" + this.getCreatedAt() + ")";
	}

	public PersonWithAllConstructor withId(Long id) {
		return this.id == id ? this : new PersonWithAllConstructor(id, this.name, this.firstName, this.sameValue, this.cool, this.personNumber, this.bornOn, this.nullable, this.things, this.place, this.createdAt);
	}
}
