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
package org.springframework.data.neo4j.core;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apiguardian.api.API;
import org.neo4j.cypherdsl.core.Cypher;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @soundtrack Bananafishbones - Viva Conputa
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
final class NamedParameters {

	private final Map<String, Object> parameters = new HashMap<>();

	/**
	 * Adds all of the values contained in {@code newParameters} to this list of named parameters.
	 *
	 * @param newParameters Additional parameters to add
	 * @throws IllegalStateException when any value in {@code newParameters} exists under the same name in the current
	 *           parameters.
	 */
	void addAll(Map<String, Object> newParameters) {
		newParameters.forEach(this::add);
	}

	/**
	 * Adds a new parameter under the key {@code name} with the value {@code value}.
	 *
	 * @param name The name of the new parameter
	 * @param value The value of the new parameter
	 * @throws IllegalStateException when a parameter with the given name already exists
	 */
	void add(String name, @Nullable Object value) {

		if (this.parameters.containsKey(name)) {
			Object previousValue = this.parameters.get(name);
			throw new IllegalArgumentException(String.format(
					"Duplicate parameter name: '%s' already in the list of named parameters with value '%s'. New value would be '%s'",
					name, previousValue == null ? "null" : previousValue.toString(), value == null ? "null" : value.toString()));
		}
		this.parameters.put(name, value);
	}

	/**
	 * @return An unmodifiable copy of this list's values.
	 */
	Map<String, Object> get() {
		return Collections.unmodifiableMap(parameters);
	}

	public boolean isEmpty() {
		return parameters.isEmpty();
	}

	@Override
	public String toString() {
		return parameters.entrySet().stream().map(e -> String.format("%s: %s", e.getKey(), formatValue(e.getValue())))
				.collect(Collectors.joining(", ", ":params {", "}"));
	}

	@Nullable
	private static String formatValue(@Nullable Object value) {
		if (value == null) {
			return null;
		} else if (value instanceof String) {
			return Cypher.quote((String) value);
		} else if (value instanceof Map) {
			return ((Map<?, ?>) value).entrySet().stream()
					.map(e -> String.format("%s: %s", e.getKey(), formatValue(e.getValue()))).collect(
							Collectors.joining(", ", "{", "}"));
		} else if (value instanceof Collection) {
			return ((Collection<?>) value).stream().map(NamedParameters::formatValue).collect(
					Collectors.joining(", ", "[", "]"));
		}

		return value.toString();
	}
}
