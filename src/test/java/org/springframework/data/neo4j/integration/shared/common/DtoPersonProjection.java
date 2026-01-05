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

import java.util.Objects;

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

	@Override
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
		if (!Objects.equals(this$name, other$name)) {
			return false;
		}
		final Object this$sameValue = this.getSameValue();
		final Object other$sameValue = other.getSameValue();
		if (!Objects.equals(this$sameValue, other$sameValue)) {
			return false;
		}
		final Object this$firstName = this.getFirstName();
		final Object other$firstName = other.getFirstName();
		return Objects.equals(this$firstName, other$firstName);
	}

	@Override
	public int hashCode() {
		final int PRIME = 59;
		int result = 1;
		final Object $name = this.getName();
		result = result * PRIME + (($name != null) ? $name.hashCode() : 43);
		final Object $sameValue = this.getSameValue();
		result = result * PRIME + (($sameValue != null) ? $sameValue.hashCode() : 43);
		final Object $firstName = this.getFirstName();
		result = result * PRIME + (($firstName != null) ? $firstName.hashCode() : 43);
		return result;
	}

	@Override
	public String toString() {
		return "DtoPersonProjection(name=" + this.getName() + ", sameValue=" + this.getSameValue() + ", firstName="
				+ this.getFirstName() + ")";
	}

}
