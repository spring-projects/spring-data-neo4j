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
package org.springframework.data.neo4j.integration.shared.conversion;

import lombok.Value;

import java.util.Arrays;
import java.util.HashSet;
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
@Value
@Node
public class PersonWithCustomId {

	/**
	 * Custom ID type for a person object.
	 */
	@Value
	public static class PersonId {

		// Be aware that this is not the native (aka generated) Neo4j id.
		// Natively generated IDs are only possible directly on a long field.
		// This is an assigned id.
		private final Long id;
	}

	/**
	 * Converted needed to deal with the above custom type. Without that converter, an association would be assumed.
	 */
	public static class CustomPersonIdConverter implements GenericConverter {

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return new HashSet<>(Arrays.asList(
					new ConvertiblePair(PersonId.class, org.neo4j.driver.Value.class),
					new ConvertiblePair(org.neo4j.driver.Value.class, PersonId.class)
			));
		}

		@Override
		public Object convert(Object o, TypeDescriptor type1, TypeDescriptor type2) {
			if (o == null) {
				return null;
			}

			if (PersonId.class.isAssignableFrom(type1.getType())) {
				return Values.value(((PersonId) o).getId());
			} else {
				return new PersonId(((org.neo4j.driver.Value) o).asLong());
			}
		}
	}

	@Id
	private final PersonId id;

	private final String name;
}
