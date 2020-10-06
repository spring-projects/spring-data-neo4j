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

import java.util.function.Function;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * This service orchestrates a standard Spring conversion service with {@link org.springframework.data.neo4j.core.convert.Neo4jConversions} registered.
 * It provides simple delegating functions that allow for an override of the converter being used.
 *
 * @author Michael J. Simons
 * @soundtrack Die Ärzte - Die Nacht der Dämonen
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jConversionService {

	/**
	 * Delegates to the underlying service, without the possibility to run a custom conversion.
	 *
	 * @param source     The source to be converted
	 * @param targetType The target type
	 * @param <T>        The type to be returned
	 * @return The converted value
	 */
	@Nullable <T> T convert(Object source, Class<T> targetType);

	/**
	 * Returns whether we have a custom conversion registered to read {@code sourceType} into a native type. The returned
	 * type might be a subclass of the given expected type though.
	 *
	 * @param sourceType must not be {@literal null}
	 * @return True if a custom write target exists.
	 * @see org.springframework.data.convert.CustomConversions#hasCustomWriteTarget(Class)
	 */
	boolean hasCustomWriteTarget(Class<?> sourceType);

	/**
	 * Reads a {@link Value} returned by the driver and converts it into a {@link Neo4jSimpleTypes simple type} supported
	 * by Neo4j SDN. If the value cannot be converted, a {@link TypeMismatchDataAccessException} will be thrown, it's
	 * cause indicating the failed conversion.
	 *
	 * <p>The returned object is generic as this method will take create target collections in case the incoming value describes a collection.
	 *
	 * @param source             The value to be read, may be null.
	 * @param targetType         The type information describing the target type.
	 * @param conversionOverride An optional conversion override.
	 * @return A simple type or null, if the value was {@literal null} or {@link org.neo4j.driver.Values#NULL}.
	 * @throws TypeMismatchDataAccessException In case the value cannot be converted to the target type
	 */
	@Nullable
	Object readValue(
			@Nullable Value source, TypeInformation<?> targetType, @Nullable Function<Value, Object> conversionOverride
	);

	/**
	 * Converts an {@link Object} to a driver's value object.
	 *
	 * @param value      The value to get written, may be null.
	 * @param sourceType The type information describing the target type.
	 * @return A driver compatible value object.
	 */
	Value writeValue(
			@Nullable Object value, TypeInformation<?> sourceType, @Nullable Function<Object, Value> conversionOverride
	);
}
