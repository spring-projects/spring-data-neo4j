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

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.ConvertWith;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

/**
 * Indicates SDN to store dates as long in the database.
 * Applicable to `java.util.Date` and `java.time.Instant`
 *
 * @author Michael J. Simons
 * @soundtrack Linkin Park - One More Light Live
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Inherited
@ConvertWith(converter = DateLongConverter.class)
@API(status = API.Status.STABLE, since = "6.0")
public @interface DateLong {
}

final class DateLongConverter implements Neo4jPersistentPropertyConverter<Date> {

	@Override
	public Value write(Date source) {
		return Values.value(source.getTime());
	}

	@Override
	public Date read(Value source) {
		return new Date(source.asLong());
	}
}
