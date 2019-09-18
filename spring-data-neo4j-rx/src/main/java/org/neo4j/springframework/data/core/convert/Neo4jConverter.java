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

import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.model.ParameterValueProvider;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * This orchestrates the build-in store conversions and any additional Spring converters.
 *
 * @author Michael J. Simons
 * @soundtrack The Kleptones - A Night At The Hip-Hopera
 * @since 1.0
 */
public interface Neo4jConverter {

	/**
	 * Reads a {@link Value} returned by the driver and converts it into a {@link Neo4jSimpleTypes simple type} supported
	 * by Neo4j SDN/RX.
	 * If the value cannot be converted, a {@link TypeMismatchDataAccessException} will be thrown, it's cause indicating
	 * the failed conversion.
	 *
	 * @param value The value to be read, may be null.
	 * @param type  The type information describing the target type
	 * @return A simple type or null, if the value was {@literal null} or {@link org.neo4j.driver.Values#NULL}.
	 * @throws TypeMismatchDataAccessException In case the value cannot be converted to the target type
	 */
	@Nullable
	Object readValue(@Nullable Value value, TypeInformation<?> type);

	@Nullable
	Value writeValue(@Nullable Object value, TypeInformation<?> type);

	/**
	 * Returns a {@link PersistentPropertyAccessor} that delegates to {@code targetPropertyAccessor} and applies
	 * all known conversions before returning a value.
	 *
	 * @param targetPropertyAccessor The property accessor to delegate to, must not be {@code null}.
	 * @param <T>                    The type of the entity to operate on.
	 * @return A {@link PersistentPropertyAccessor} guaranteed to be not {@code null}.
	 */
	<T> PersistentPropertyAccessor<T> decoratePropertyAccessor(TypeSystem typeSystem, PersistentPropertyAccessor<T> targetPropertyAccessor);

	/**
	 * Returns a {@link ParameterValueProvider} that delegates to {@code targetParameterValueProvider} and applies
	 * all known conversions before returning a value.
	 *
	 * @param targetParameterValueProvider The parameter value provider to delegate to, must not be {@code null}.
	 * @param <T>                          The type of the entity to operate on.
	 * @return A {@link ParameterValueProvider} guaranteed to be not {@code null}.
	 */
	<T extends PersistentProperty<T>> ParameterValueProvider<T> decorateParameterValueProvider(
		ParameterValueProvider<T> targetParameterValueProvider);
}
