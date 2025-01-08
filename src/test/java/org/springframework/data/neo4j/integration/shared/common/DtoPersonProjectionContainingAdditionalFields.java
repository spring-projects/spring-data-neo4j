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

/**
 * @author Michael J. Simons
 */
public final class DtoPersonProjectionContainingAdditionalFields {

	private final String name;
	private final String sameValue;
	private final String firstName;

	private final List<PersonWithAllConstructor> otherPeople;

	private final Long someLongValue;

	private final List<Double> someDoubles;

	public DtoPersonProjectionContainingAdditionalFields(String name, String sameValue, String firstName, List<PersonWithAllConstructor> otherPeople, Long someLongValue, List<Double> someDoubles) {
		this.name = name;
		this.sameValue = sameValue;
		this.firstName = firstName;
		this.otherPeople = otherPeople;
		this.someLongValue = someLongValue;
		this.someDoubles = someDoubles;
	}

	public String getName() {
		return this.name;
	}

	public String getSameValue() {
		return this.sameValue;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public List<PersonWithAllConstructor> getOtherPeople() {
		return this.otherPeople;
	}

	public Long getSomeLongValue() {
		return this.someLongValue;
	}

	public List<Double> getSomeDoubles() {
		return this.someDoubles;
	}

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof DtoPersonProjectionContainingAdditionalFields)) {
			return false;
		}
		final DtoPersonProjectionContainingAdditionalFields other = (DtoPersonProjectionContainingAdditionalFields) o;
		final Object this$name = this.getName();
		final Object other$name = other.getName();
		if (this$name == null ? other$name != null : !this$name.equals(other$name)) {
			return false;
		}
		final Object this$sameValue = this.getSameValue();
		final Object other$sameValue = other.getSameValue();
		if (this$sameValue == null ? other$sameValue != null : !this$sameValue.equals(other$sameValue)) {
			return false;
		}
		final Object this$firstName = this.getFirstName();
		final Object other$firstName = other.getFirstName();
		if (this$firstName == null ? other$firstName != null : !this$firstName.equals(other$firstName)) {
			return false;
		}
		final Object this$otherPeople = this.getOtherPeople();
		final Object other$otherPeople = other.getOtherPeople();
		if (this$otherPeople == null ? other$otherPeople != null : !this$otherPeople.equals(other$otherPeople)) {
			return false;
		}
		final Object this$someLongValue = this.getSomeLongValue();
		final Object other$someLongValue = other.getSomeLongValue();
		if (this$someLongValue == null ? other$someLongValue != null : !this$someLongValue.equals(other$someLongValue)) {
			return false;
		}
		final Object this$someDoubles = this.getSomeDoubles();
		final Object other$someDoubles = other.getSomeDoubles();
		if (this$someDoubles == null ? other$someDoubles != null : !this$someDoubles.equals(other$someDoubles)) {
			return false;
		}
		return true;
	}

	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + ($name == null ? 43 : $name.hashCode());
		final Object $sameValue = this.getSameValue();
		result = result * PRIME + ($sameValue == null ? 43 : $sameValue.hashCode());
		final Object $firstName = this.getFirstName();
		result = result * PRIME + ($firstName == null ? 43 : $firstName.hashCode());
		final Object $otherPeople = this.getOtherPeople();
		result = result * PRIME + ($otherPeople == null ? 43 : $otherPeople.hashCode());
		final Object $someLongValue = this.getSomeLongValue();
		result = result * PRIME + ($someLongValue == null ? 43 : $someLongValue.hashCode());
		final Object $someDoubles = this.getSomeDoubles();
		result = result * PRIME + ($someDoubles == null ? 43 : $someDoubles.hashCode());
		return result;
	}

	public String toString() {
		return "DtoPersonProjectionContainingAdditionalFields(name=" + this.getName() + ", sameValue=" + this.getSameValue() + ", firstName=" + this.getFirstName() + ", otherPeople=" + this.getOtherPeople() + ", someLongValue=" + this.getSomeLongValue() + ", someDoubles=" + this.getSomeDoubles() + ")";
	}
}
