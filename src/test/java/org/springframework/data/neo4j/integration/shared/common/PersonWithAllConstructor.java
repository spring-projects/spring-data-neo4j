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

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.neo4j.driver.types.Point;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

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

	private final String sameValue;

	private final Boolean cool;

	private final Long personNumber;

	private final LocalDate bornOn;

	private final Point place;

	private final Instant createdAt;

	@Property("first_name")
	private String firstName;

	private String nullable;

	private List<String> things;

	public PersonWithAllConstructor(Long id, String name, String firstName, String sameValue, Boolean cool,
			Long personNumber, LocalDate bornOn, String nullable, List<String> things, Point place, Instant createdAt) {
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

	public void setFirstName(String firstName) {
		this.firstName = firstName;
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

	public void setNullable(String nullable) {
		this.nullable = nullable;
	}

	public List<String> getThings() {
		return this.things;
	}

	public void setThings(List<String> things) {
		this.things = things;
	}

	public Point getPlace() {
		return this.place;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	protected boolean canEqual(final Object other) {
		return other instanceof PersonWithAllConstructor;
	}

	public PersonWithAllConstructor withId(Long id) {
		return Objects.equals(this.id, id) ? this
				: new PersonWithAllConstructor(id, this.name, this.firstName, this.sameValue, this.cool,
						this.personNumber, this.bornOn, this.nullable, this.things, this.place, this.createdAt);
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PersonWithAllConstructor)) {
			return false;
		}
		final PersonWithAllConstructor other = (PersonWithAllConstructor) o;
		if (!other.canEqual(this)) {
			return false;
		}
		final Object this$id = this.getId();
		final Object other$id = other.getId();
		if (!Objects.equals(this$id, other$id)) {
			return false;
		}
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (!Objects.equals(this$name, other$name)) {
			return false;
		}
		final Object this$firstName = this.getFirstName();
		final Object other$firstName = other.getFirstName();
		if (!Objects.equals(this$firstName, other$firstName)) {
			return false;
		}
		final Object this$sameValue = this.getSameValue();
		final Object other$sameValue = other.getSameValue();
		if (!Objects.equals(this$sameValue, other$sameValue)) {
			return false;
		}
		final Object this$cool = this.getCool();
		final Object other$cool = other.getCool();
		if (!Objects.equals(this$cool, other$cool)) {
			return false;
		}
		final Object this$personNumber = this.getPersonNumber();
		final Object other$personNumber = other.getPersonNumber();
		if (!Objects.equals(this$personNumber, other$personNumber)) {
			return false;
		}
		final Object this$bornOn = this.getBornOn();
		final Object other$bornOn = other.getBornOn();
		if (!Objects.equals(this$bornOn, other$bornOn)) {
			return false;
		}
		final Object this$nullable = this.getNullable();
		final Object other$nullable = other.getNullable();
		if (!Objects.equals(this$nullable, other$nullable)) {
			return false;
		}
		final Object this$things = this.getThings();
		final Object other$things = other.getThings();
		if (!Objects.equals(this$things, other$things)) {
			return false;
		}
		final Object this$place = this.getPlace();
		final Object other$place = other.getPlace();
		if (!Objects.equals(this$place, other$place)) {
			return false;
		}
		final Object this$createdAt = this.getCreatedAt();
		final Object other$createdAt = other.getCreatedAt();
		return Objects.equals(this$createdAt, other$createdAt);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $id = this.getId();
		result = result * PRIME + (($id != null) ? $id.hashCode() : 43);
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		final Object $firstName = this.getFirstName();
		result = result * PRIME + (($firstName != null) ? $firstName.hashCode() : 43);
		final Object $sameValue = this.getSameValue();
		result = result * PRIME + (($sameValue != null) ? $sameValue.hashCode() : 43);
		final Object $cool = this.getCool();
		result = result * PRIME + (($cool != null) ? $cool.hashCode() : 43);
		final Object $personNumber = this.getPersonNumber();
		result = result * PRIME + (($personNumber != null) ? $personNumber.hashCode() : 43);
		final Object $bornOn = this.getBornOn();
		result = result * PRIME + (($bornOn != null) ? $bornOn.hashCode() : 43);
		final Object $nullable = this.getNullable();
		result = result * PRIME + (($nullable != null) ? $nullable.hashCode() : 43);
		final Object $things = this.getThings();
		result = result * PRIME + (($things != null) ? $things.hashCode() : 43);
		final Object $place = this.getPlace();
		result = result * PRIME + (($place != null) ? $place.hashCode() : 43);
		final Object $createdAt = this.getCreatedAt();
		result = result * PRIME + (($createdAt != null) ? $createdAt.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "PersonWithAllConstructor(id=" + this.getId() + ", name=" + this.getName() + ", firstName="
				+ this.getFirstName() + ", sameValue=" + this.getSameValue() + ", cool=" + this.getCool()
				+ ", personNumber=" + this.getPersonNumber() + ", bornOn=" + this.getBornOn() + ", nullable="
				+ this.getNullable() + ", things=" + this.getThings() + ", place=" + this.getPlace() + ", createdAt="
				+ this.getCreatedAt() + ")";
	}

}
