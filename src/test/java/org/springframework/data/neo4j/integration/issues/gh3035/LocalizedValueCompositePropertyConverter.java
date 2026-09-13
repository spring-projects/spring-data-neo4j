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
package org.springframework.data.neo4j.integration.issues.gh3035;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

/**
 * @author arimu1
 */
public final class LocalizedValueCompositePropertyConverter
		implements Neo4jPersistentPropertyToMapConverter<String, LocalizedValue<String>> {

	private static final String VALUE = "value";

	@Override
	public Map<String, Value> decompose(LocalizedValue<String> property,
			Neo4jConversionService neo4jConversionService) {
		Map<String, Value> values = new HashMap<>();
		values.put(VALUE, Values.value(property.getValue()));
		return values;
	}

	@Override
	public LocalizedValue<String> compose(Map<String, Value> source, Neo4jConversionService neo4jConversionService) {
		String value = Optional.ofNullable(source.get(VALUE)).map(Value::asString).orElse(null);
		return new LocalizedValue<>(value);
	}

}
