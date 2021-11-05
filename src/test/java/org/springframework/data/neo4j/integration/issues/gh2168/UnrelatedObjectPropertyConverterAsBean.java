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
package org.springframework.data.neo4j.integration.issues.gh2168;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

/**
 * @author Michael J. Simons
 */
public class UnrelatedObjectPropertyConverterAsBean implements Neo4jPersistentPropertyConverter<UnrelatedObject> {

	@Autowired
	private Driver driver;

	@Override
	public Value write(UnrelatedObject source) {

		return Values.value(source.isABooleanValue() + ";" + source.getALongValue());
	}

	@Override
	public UnrelatedObject read(Value source) {

		if (!driver.defaultTypeSystem().STRING().isTypeOf(source)) {
			throw new IllegalArgumentException("Unsupported value");
		}

		String[] concatenatedValues = source.asString().split(";");
		if (concatenatedValues.length == 2) {
			return new UnrelatedObject(Boolean.parseBoolean(concatenatedValues[0]),
					Long.parseLong(concatenatedValues[1]));
		}
		return null;
	}
}
