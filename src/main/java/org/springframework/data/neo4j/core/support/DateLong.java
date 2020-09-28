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
package org.springframework.data.neo4j.core.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Date;
import java.util.function.Function;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.core.convert.converter.Converter;

/**
 * Indicates OGM to store dates as long in the database.
 * Applicable to `java.util.Date` and `java.time.Instant`
 *
 * @author Michael J. Simons
 * @soundtrack Linkin Park - One More Light Live
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Inherited
@ConvertAs(writingConverter = DateToLongValueConverter.class, readingConverter = LongValueToDateConverter.class)
public @interface DateLong {
}

class DateToLongValueConverter implements Function<Date, Value> {

	@Override
	public Value apply(Date source) {
		return source == null ? Values.NULL : Values.value(source.getTime());
	}
}


class LongValueToDateConverter implements Function<Value, Date> {

	@Override
	public Date apply(Value source) {
		return source == null ? null : new Date(source.asLong());
	}
}
