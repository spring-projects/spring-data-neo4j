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
package org.neo4j.springframework.data.core.schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Path;
import org.neo4j.driver.types.Point;
import org.neo4j.driver.types.Relationship;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * A list of Neo4j simple types: All attributes that can be mapped to a property. Some special logic has to be applied
 * for domain attributes of the collection types {@link java.util.List} and {@link java.util.Map}. Those can be mapped
 * to simple properties as well as to relationships to other things.
 * <p>
 * The Java driver itself has a good overview of the supported types:
 * <a href="https://neo4j.com/docs/driver-manual/1.7/cypher-values/#driver-neo4j-type-system">The Cypher type system</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class Neo4jSimpleTypes {

	public static final Set<Class<?>> NEO4J_NATIVE_TYPES;

	static {
		Set<Class<?>> neo4jNativeTypes = new HashSet<>();
		neo4jNativeTypes.add(void.class);
		neo4jNativeTypes.add(Void.class);
		neo4jNativeTypes.add(Map.class);
		neo4jNativeTypes.add(boolean.class);
		neo4jNativeTypes.add(Boolean.class);
		neo4jNativeTypes.add(long.class);
		neo4jNativeTypes.add(Long.class);
		neo4jNativeTypes.add(String.class);
		neo4jNativeTypes.add(byte[].class);
		neo4jNativeTypes.add(LocalDate.class);
		neo4jNativeTypes.add(OffsetTime.class);
		neo4jNativeTypes.add(LocalTime.class);
		neo4jNativeTypes.add(ZonedDateTime.class);
		neo4jNativeTypes.add(LocalDateTime.class);
		neo4jNativeTypes.add(IsoDuration.class);
		neo4jNativeTypes.add(Point.class);
		neo4jNativeTypes.add(Node.class);
		neo4jNativeTypes.add(Relationship.class);
		neo4jNativeTypes.add(Path.class);

		NEO4J_NATIVE_TYPES = Collections.unmodifiableSet(neo4jNativeTypes);
	}

	/**
	 * The simple types we support plus all the simple types recognized by Spring.
	 */
	// TODO We need a conversion for some.
	// TODO Add some special treatment for Period/Duration vs IsoDuration as well as the spatial types.
	public static final SimpleTypeHolder SIMPLE_TYPE_HOLDER = new SimpleTypeHolder(NEO4J_NATIVE_TYPES, true);

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

		return o.filter(v -> targetClass.isAssignableFrom(v.getClass()))
			.map(targetClass::cast)
			.orElseThrow(() -> new IllegalArgumentException(
				String.format("%s is not assignable from %s", targetClass.getName(), o.get().getClass().getName())));
	}

	private Neo4jSimpleTypes() {
	}
}
