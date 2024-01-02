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

/**
 * @author Michael J. Simon
 */
public final class DtoPersonProjection {

	private final String name;
	private final String sameValue;
	private final String firstName;

	public DtoPersonProjection(String name, String sameValue, String firstName) {
		this.name = name;
		this.sameValue = sameValue;
		this.firstName = firstName;
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

	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof DtoPersonProjection)) {
			return false;
		}
		final DtoPersonProjection other = (DtoPersonProjection) o;
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
		return result;
	}

	public String toString() {
		return "DtoPersonProjection(name=" + this.getName() + ", sameValue=" + this.getSameValue() + ", firstName=" + this.getFirstName() + ")";
	}
}
