/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.core.annotation.AliasFor;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

/**
 * Indicates SDN 6 to store dates as {@link String} in the database. Applicable to {@link Date} and
 * {@link java.time.Instant}.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - S&M2
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
@ConvertWith(converterFactory = DateStringConverterFactory.class)
@API(status = API.Status.STABLE, since = "6.0")
public @interface DateString {

	String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

	String DEFAULT_ZONE_ID = "UTC";

	@AliasFor("format")
	String value() default ISO_8601;

	@AliasFor("value")
	String format() default ISO_8601;

	/**
	 * Some temporals like {@link java.time.Instant}, representing an instantaneous point in time cannot be formatted
	 * with a given {@link java.time.ZoneId}. In case you want to format an instant or similar with a default pattern,
	 * we assume a zone with the given id and default to {@literal UTC} which is the same assumption that the predefined
	 * patterns in {@link java.time.format.DateTimeFormatter} take.
	 *
	 * @return The zone id to use when applying a custom pattern to an instant temporal.
	 */
	String zoneId() default DEFAULT_ZONE_ID;
}

final class DateStringConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	@Override
	public Neo4jPersistentPropertyConverter<?> getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		if (persistentProperty.getActualType() == Date.class) {
			DateString config = persistentProperty.getRequiredAnnotation(DateString.class);
			return new DateStringConverter(config.value());
		} else {
			throw new UnsupportedOperationException(
					"Other types than java.util.Date are not yet supported. Please file a ticket.");
		}
	}
}

final class DateStringConverter implements Neo4jPersistentPropertyConverter<Date> {

	private final String format;

	DateStringConverter(String format) {
		this.format = format;
	}

	private SimpleDateFormat getFormat() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(DateString.DEFAULT_ZONE_ID));
		return simpleDateFormat;
	}

	@Override
	public Value write(Date source) {
		return Values.value(getFormat().format(source));
	}

	@Override
	public Date read(Value source) {
		try {
			return getFormat().parse(source.asString());
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}

}
