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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

/**
 * @author Michael J. Simons
 */
public final class UnrelatedObjectCompositePropertyConverter implements Neo4jPersistentPropertyToMapConverter<String, UnrelatedObject> {

	private static final String A_BOOLEAN_VALUE = "aBooleanValue";
	private static final String A_LONG_VALUE = "aLongValue";

	@Override
	public Map<String, Value> decompose(UnrelatedObject property, Neo4jConversionService neo4jConversionService) {
		Map<String, Value> values = new HashMap<>();
		values.put(A_BOOLEAN_VALUE, Values.value(property.isABooleanValue()));
		values.put(A_LONG_VALUE, Values.value(property.getALongValue()));
		return values;
	}

	@Override
	public UnrelatedObject compose(Map<String, Value> source, Neo4jConversionService neo4jConversionService) {
		boolean aBooleanValue = Optional.ofNullable(source.get(A_BOOLEAN_VALUE))
				.map(Value::asBoolean)
				.orElse(false);

		Long aLongValue = Optional.ofNullable(source.get(A_LONG_VALUE))
				.map(Value::asLong)
				.orElse(0L);

		return new UnrelatedObject(aBooleanValue, aLongValue);
	}
}
