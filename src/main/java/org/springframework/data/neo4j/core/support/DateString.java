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
package org.springframework.data.neo4j.core.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;

import org.apiguardian.api.API;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.neo4j.core.convert.ConvertWith;

/**
 * Indicates SDN 6 to store dates as {@link String} in the database. Applicable to
 * {@link Date} and {@link java.time.Instant}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
@ConvertWith(converterFactory = DateStringConverterFactory.class)
@API(status = API.Status.STABLE, since = "6.0")
public @interface DateString {

	/**
	 * Pattern conforming to an ISO 8601 date time string (without timezone).
	 */
	String ISO_8601 = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

	/**
	 * The ID of the default timezone to use.
	 */
	String DEFAULT_ZONE_ID = "UTC";

	@AliasFor("format")
	String value() default ISO_8601;

	@AliasFor("value")
	String format() default ISO_8601;

	/**
	 * Some temporals like {@link java.time.Instant}, representing an instantaneous point
	 * in time cannot be formatted with a given {@link java.time.ZoneId}. In case you want
	 * to format an instant or similar with a default pattern, we assume a zone with the
	 * given id and default to {@literal UTC} which is the same assumption that the
	 * predefined patterns in {@link java.time.format.DateTimeFormatter} take.
	 * @return The zone id to use when applying a custom pattern to an instant temporal.
	 */
	String zoneId() default DEFAULT_ZONE_ID;

}
