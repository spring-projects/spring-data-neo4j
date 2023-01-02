/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.integration.conversion_imperative.compose_as_ids;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 */
public final class CompositeValue {
	private final String value1;
	private final Integer value2;

	public CompositeValue(String value1, Integer value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	public String value1() {
		return value1;
	}

	public Integer value2() {
		return value2;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		CompositeValue that = (CompositeValue) obj;
		return Objects.equals(this.value1, that.value1) &&
				Objects.equals(this.value2, that.value2);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value1, value2);
	}

	static class Converter implements Neo4jPersistentPropertyToMapConverter<String, CompositeValue> {

		@NonNull
		@Override
		public Map<String, Value> decompose(@Nullable CompositeValue property, Neo4jConversionService conversionService) {

			final HashMap<String, Value> decomposed = new HashMap<>();
			if (property == null) {
				decomposed.put("value1", Values.NULL);
				decomposed.put("value2", Values.NULL);
			} else {
				decomposed.put("value1", Values.value(property.value1));
				decomposed.put("value2", Values.value(property.value2));
			}
			return decomposed;
		}

		@Override
		public CompositeValue compose(Map<String, Value> source, Neo4jConversionService conversionService) {
			return source.isEmpty() ?
					null :
					new CompositeValue(source.get("value1").asString(), source.get("value2").asInt());
		}
	}
}
