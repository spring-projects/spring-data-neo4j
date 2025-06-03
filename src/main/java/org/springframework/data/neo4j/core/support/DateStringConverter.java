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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

final class DateStringConverter implements Neo4jPersistentPropertyConverter<Date> {

	private final String format;

	DateStringConverter(String format) {
		this.format = format;
	}

	private SimpleDateFormat getFormat() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.format);
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(DateString.DEFAULT_ZONE_ID));
		return simpleDateFormat;
	}

	@Override
	public Value write(@Nullable Date source) {
		return (source != null) ? Values.value(getFormat().format(source)) : Values.NULL;
	}

	@Override
	@Nullable public Date read(@Nullable Value source) {
		try {
			return (source == null || TypeSystem.getDefault().NULL().isTypeOf(source)) ? null
					: getFormat().parse(source.asString());
		}
		catch (ParseException ex) {
			throw new RuntimeException(ex);
		}
	}

}
