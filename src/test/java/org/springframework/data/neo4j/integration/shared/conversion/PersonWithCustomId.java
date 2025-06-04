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
package org.springframework.data.neo4j.integration.shared.conversion;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.neo4j.driver.Values;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * @author Rosetta Roberts
 * @author Michael J. Simons
 */
@Node
public final class PersonWithCustomId {

	@Id
	private final PersonId id;

	private final String name;

	public PersonWithCustomId(PersonId id, String name) {
		this.id = id;
		this.name = name;
	}

	public PersonId getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof PersonWithCustomId)) {
			return false;
		}
		final PersonWithCustomId other = (PersonWithCustomId) o;
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

	@Override
	public String toString() {
		return "PersonWithCustomId(id=" + this.getId() + ", name=" + this.getName() + ")";
	}

	/**
	 * Custom ID type for a person object.
	 */
	public static final class PersonId {

		// Be aware that this is not the native (aka generated) Neo4j id.
		// Natively generated IDs are only possible directly on a long field.
		// This is an assigned id.
		private final Long id;

		public PersonId(Long id) {
			this.id = id;
		}

		public Long getId() {
			return this.id;
		}

		@Override
		public boolean equals(final Object o) {
			if (o == this) {
				return true;
			}
			if (!(o instanceof PersonId)) {
				return false;
			}
			final PersonId other = (PersonId) o;
			final Object this$id = this.getId();
			final Object other$id = other.getId();
			return Objects.equals(this$id, other$id);
		}

		@Override
		public int hashCode() {
			final int PRIME = 59;
			int result = 1;
			final Object $id = this.getId();
			result = (result * PRIME) + (($id != null) ? $id.hashCode() : 43);
			return result;
		}

		@Override
		public String toString() {
			return "PersonWithCustomId.PersonId(id=" + this.getId() + ")";
		}

	}

	/**
	 * Converted needed to deal with the above custom type. Without that converter, an
	 * association would be assumed.
	 */
	public static class CustomPersonIdConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return new HashSet<>(Arrays.asList(new ConvertiblePair(PersonId.class, org.neo4j.driver.Value.class),
					new ConvertiblePair(org.neo4j.driver.Value.class, PersonId.class)));
		}

		@Override
		public Object convert(Object o, TypeDescriptor type1, TypeDescriptor type2) {
			if (o == null) {
				return null;
			}

			if (PersonId.class.isAssignableFrom(type1.getType())) {
				return Values.value(((PersonId) o).getId());
			}
			else {
				return new PersonId(((org.neo4j.driver.Value) o).asLong());
			}
		}

	}

}
