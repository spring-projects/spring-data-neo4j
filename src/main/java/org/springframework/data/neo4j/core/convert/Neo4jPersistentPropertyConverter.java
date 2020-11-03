/*
 * Copyright 2011-2020 the original author or authors.
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
import org.neo4j.driver.Value;

/**
 * This  interface represents a pair of methods capable of converting values of type {@code T} to and from {@link Value values}.
 *
 * @param <T> The type of the property to convert (the type of the actual attribute).
 * @author Michael J. Simons
 * @soundtrack Antilopen Gang - Adrenochrom
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentPropertyConverter<T> {

	/**
	 * @param source The value to store, never null.
	 * @return The converted value, never null.
	 */
	Value write(T source);

	/**
	 * @param source The value to read, never null or {@link org.neo4j.driver.Values#NULL}
	 * @return The converted value, maybe null if {@code source} was equals to {@link org.neo4j.driver.Values#NULL}.
	 */
	T read(Value source);
}
