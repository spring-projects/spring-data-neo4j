/*
 * Copyright 2011-present the original author or authors.
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

import java.util.Date;

import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverterFactory;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty;

final class DateStringConverterFactory implements Neo4jPersistentPropertyConverterFactory {

	@Override
	public Neo4jPersistentPropertyConverter<?> getPropertyConverterFor(Neo4jPersistentProperty persistentProperty) {

		if (persistentProperty.getActualType() == Date.class) {
			DateString config = persistentProperty.getRequiredAnnotation(DateString.class);
			return new DateStringConverter(config.value());
		}
		else {
			throw new UnsupportedOperationException(
					"Other types than java.util.Date are not yet supported; please file a ticket");
		}
	}

}
