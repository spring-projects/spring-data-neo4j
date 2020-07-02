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

import java.util.Map;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * This orchestrates the build-in store conversions and any additional Spring converters.
 *
 * @author Michael J. Simons
 * @soundtrack The Kleptones - A Night At The Hip-Hopera
 * @since 1.0
 */
public interface Neo4jConverter extends EntityReader<Object, Record>, EntityWriter<Object, Map<String, Object>> {

	/**
	 * Reads a {@link Value} returned by the driver and converts it into a {@link Neo4jSimpleTypes simple type} supported
	 * by Neo4j SDN/RX.
	 * If the value cannot be converted, a {@link TypeMismatchDataAccessException} will be thrown, it's cause indicating
	 * the failed conversion.
	 *
	 * @param value The value to be read, may be null.
	 * @param type  The type information describing the target type.
	 * @return A simple type or null, if the value was {@literal null} or {@link org.neo4j.driver.Values#NULL}.
	 * @throws TypeMismatchDataAccessException In case the value cannot be converted to the target type
	 */
	@Nullable
	Object readValueForProperty(@Nullable Value value, TypeInformation<?> type);

	/**
	 * Converts an {@link Object} to a driver's value object.
	 *
	 * @param value The value to get written, may be null.
	 * @param type  The type information describing the target type.
	 * @return A driver compatible value object.
	 */
	Value writeValueFromProperty(@Nullable Object value, TypeInformation<?> type);

}
