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
package org.springframework.data.neo4j.core.mapping;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.MapAccessor;

/**
 * A shim to adapt between {@link Record} (respectively {@link org.neo4j.driver.types.MapAccessorWithDefaultValue}
 * and {@link MapAccessor}.
 *
 * @author Michael J. Simons
 * @soundtrack The Prodigy - Music For The Jilted Generation
 */
final class RecordMapAccessor implements MapAccessor {

	private final Record delegate;

	RecordMapAccessor(Record delegate) {
		this.delegate = delegate;
	}

	@Override
	public Iterable<String> keys() {
		return this.delegate.keys();
	}

	@Override
	public boolean containsKey(String key) {
		return this.delegate.containsKey(key);
	}

	@Override
	public Value get(String key) {
		return this.delegate.get(key);
	}

	@Override
	public int size() {
		return this.delegate.size();
	}

	@Override
	public Iterable<Value> values() {
		return this.delegate.values();
	}

	@Override
	public <T> Iterable<T> values(Function<Value, T> mapFunction) {
		return this.delegate.values().stream().map(mapFunction).collect(Collectors.toList());
	}

	@Override
	public Map<String, Object> asMap() {
		return this.delegate.asMap();
	}

	@Override
	public <T> Map<String, T> asMap(Function<Value, T> mapFunction) {
		return this.delegate.asMap(mapFunction);
	}

	@Override
	public String toString() {
		return this.delegate.toString();
	}
}
