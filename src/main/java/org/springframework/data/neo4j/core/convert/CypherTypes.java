/*
 * Copyright 2011-2024 the original author or authors.
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.IsoDuration;
import org.neo4j.driver.types.Point;
import org.springframework.data.convert.ConverterBuilder;

/**
 * Conversions for all known Cypher types, directly supported by the driver. See
 * <a href="https://neo4j.com/docs/java-manual/current/cypher-workflow/#java-driver-type-mapping">Working with Cypher values</a>.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class CypherTypes {

	static final List<?> CONVERTERS;

	static {

		List<ConverterBuilder.ConverterAware> hlp = new ArrayList<>();
		hlp.add(ConverterBuilder.reading(Value.class, Void.class, v -> null).andWriting(v -> Values.NULL));
		hlp.add(ConverterBuilder.reading(Value.class, void.class, v -> null).andWriting(v -> Values.NULL));
		hlp.add(ConverterBuilder.reading(Value.class, Boolean.class, Value::asBoolean).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, boolean.class, Value::asBoolean).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Long.class, Value::asLong).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, long.class, Value::asLong).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Double.class, Value::asDouble).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, double.class, Value::asDouble).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, String.class, Value::asString).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, byte[].class, Value::asByteArray).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, LocalDate.class, Value::asLocalDate).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, OffsetTime.class, Value::asOffsetTime).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, OffsetDateTime.class, Value::asOffsetDateTime).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, LocalTime.class, Value::asLocalTime).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, ZonedDateTime.class, Value::asZonedDateTime).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, LocalDateTime.class, Value::asLocalDateTime).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, IsoDuration.class, Value::asIsoDuration).andWriting(Values::value));
		hlp.add(ConverterBuilder.reading(Value.class, Point.class, Value::asPoint).andWriting(Values::value));

		CONVERTERS = Collections.unmodifiableList(hlp);
	}

	private CypherTypes() {}
}
