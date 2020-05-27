/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.opencypherdsl;

/**
 * A container that exposes methods to add properties with values to nodes or relationships.
 *
 * @author Michael J. Simons
 * @param <T> type of the object holding the specified properties
 * @soundtrack Daft Punk - Homework
 * @since 1.1
 */
public interface ExposesProperties<T extends ExposesProperties<?> & PropertyContainer> {

	/**
	 * Creates a a copy of this property container with additional properties.
	 * Creates a property container without properties when no properties are passed to this method.
	 *
	 * @param newProperties the new properties (can be {@literal null} to remove exiting properties).
	 * @return The new property container.
	 */
	T withProperties(MapExpression<?> newProperties);

	/**
	 * Creates a a copy of this property container with additional properties.
	 * Creates a property container without properties when no properties are passed to this method.
	 *
	 * @param keysAndValues A list of key and values. Must be an even number, with alternating {@link String} and {@link Expression}.
	 * @return The new property container.
	 */
	T withProperties(Object... keysAndValues);
}
