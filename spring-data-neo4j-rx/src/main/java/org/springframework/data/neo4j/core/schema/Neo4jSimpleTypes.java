/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.schema;

import java.util.Optional;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;

/**
 * A list of Neo4j simple types: All attributes that can be mapped to a property. There is never a relationship
 * established for attributes of a node that are simple types.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Neo4jSimpleTypes {

	/**
	 * Converts the given Neo4j driver value into the designated type.
	 *
	 * @param value       The value to convert.
	 * @param targetClass The target class to convert into
	 * @param <T>         The type of the target class
	 * @return The converted value or {@literal null} when the values has been {@literal null}
	 * @throws IllegalArgumentException when the value cannot be converted into an object of the given target class
	 */
	public static <T> T asObject(Value value, Class<T> targetClass) {

		Optional<Object> o = Optional.ofNullable(value).map(Value::asObject);
		if (!o.isPresent()) {
			return null;
		}

		// TODO Add some special treatment for Period/Duration vs IsoDuration as well as the spatial types.

		return o.filter(v -> targetClass.isAssignableFrom(v.getClass()))
			.map(targetClass::cast)
			.orElseThrow(() -> new IllegalArgumentException(
				String.format("%s is not assignable from %s", targetClass.getName(), o.get().getClass().getName())));
	}

	private Neo4jSimpleTypes() {
	}
}
