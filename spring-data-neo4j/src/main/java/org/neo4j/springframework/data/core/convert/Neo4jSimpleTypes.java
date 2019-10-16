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
package org.neo4j.springframework.data.core.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Point;
import org.neo4j.springframework.data.types.CartesianPoint2d;
import org.neo4j.springframework.data.types.CartesianPoint3d;
import org.neo4j.springframework.data.types.GeographicPoint2d;
import org.neo4j.springframework.data.types.GeographicPoint3d;
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

	private static final Set<Class<?>> NEO4J_NATIVE_TYPES;

	static {
		Set<Class<?>> neo4jNativeTypes = new HashSet<>();

		neo4jNativeTypes.add(Instant.class);
		neo4jNativeTypes.add(IsoDuration.class);
		neo4jNativeTypes.add(LocalDate.class);
		neo4jNativeTypes.add(LocalDateTime.class);
		neo4jNativeTypes.add(LocalTime.class);
		neo4jNativeTypes.add(Map.class);
		neo4jNativeTypes.add(OffsetTime.class);
		neo4jNativeTypes.add(Point.class);
		neo4jNativeTypes.add(Void.class);
		neo4jNativeTypes.add(ZonedDateTime.class);
		neo4jNativeTypes.add(void.class);
		neo4jNativeTypes.add(UUID.class);

		neo4jNativeTypes.add(BigDecimal.class);
		neo4jNativeTypes.add(BigInteger.class);

		neo4jNativeTypes.add(org.springframework.data.geo.Point.class);
		neo4jNativeTypes.add(GeographicPoint2d.class);
		neo4jNativeTypes.add(GeographicPoint3d.class);
		neo4jNativeTypes.add(CartesianPoint2d.class);
		neo4jNativeTypes.add(CartesianPoint3d.class);

		neo4jNativeTypes.add(Value.class);

		NEO4J_NATIVE_TYPES = Collections.unmodifiableSet(neo4jNativeTypes);
	}

	/**
	 * The simple types we support plus all the simple types recognized by Spring.
	 */
	public static final SimpleTypeHolder HOLDER = new SimpleTypeHolder(NEO4J_NATIVE_TYPES, true);

	private Neo4jSimpleTypes() {
	}
}
