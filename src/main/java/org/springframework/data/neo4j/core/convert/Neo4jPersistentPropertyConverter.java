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
package org.springframework.data.neo4j.core.convert;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;

/**
 * This interface represents a pair of methods capable of converting values of type
 * {@code T} to and from {@link Value values}.
 *
 * @param <T> the type of the property to convert (the type of the actual attribute).
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentPropertyConverter<T> {

	/**
	 * Writes a property to a Neo4j value.
	 * @param source the value to store. We might pass {@literal null}, if your converter
	 * is not able to handle that, this is ok, we do handle {@link NullPointerException
	 * null pointer exceptions}
	 * @return the converted value, never null. To represent {@literal null}, use
	 * {@link org.neo4j.driver.Values#NULL}
	 */
	Value write(@Nullable T source);

	/**
	 * Reads a property from a Neo4j value.
	 * @param source the value to read, never null or {@link org.neo4j.driver.Values#NULL}
	 * @return the converted value, maybe null if {@code source} was equals to
	 * {@link org.neo4j.driver.Values#NULL}.
	 */
	@Nullable T read(@Nullable Value source);

}
