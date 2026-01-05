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
package org.springframework.data.neo4j.core.schema;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;

/**
 * Dedicated and highly specialized converter for reading and writing {@link Map} with
 * either enum or string keys into multiple properties of Nodes or Relationships inside
 * the Neo4j database. This is an internal API only.
 *
 * @param <K> the type of the key
 * @param <P> the type of the property value
 * @author Michael J. Simons
 */
final class CompositePropertyConverter<K, P> implements Neo4jPersistentPropertyConverter<P> {

	private final Neo4jPersistentPropertyToMapConverter<K, P> delegate;

	private final String prefixWithDelimiter;

	private final Neo4jConversionService neo4jConversionService;

	private final Class<?> typeOfKeys;

	private final Function<K, String> keyWriter;

	private final Function<String, K> keyReader;

	CompositePropertyConverter(Neo4jPersistentPropertyToMapConverter<K, P> delegate, String prefixWithDelimiter,
			Neo4jConversionService neo4jConversionService, Class<?> typeOfKeys, Function<K, String> keyWriter,
			Function<String, K> keyReader) {
		this.delegate = delegate;
		this.prefixWithDelimiter = prefixWithDelimiter;
		this.neo4jConversionService = neo4jConversionService;
		this.typeOfKeys = typeOfKeys;
		this.keyWriter = keyWriter;
		this.keyReader = keyReader;
	}

	@Override
	public Value write(@Nullable P property) {

		Map<K, Value> source = this.delegate.decompose(property, this.neo4jConversionService);
		Map<String, Object> temp = new HashMap<>();
		source.forEach((key, value) -> temp.put(this.prefixWithDelimiter + this.keyWriter.apply(key), value));
		return Values.value(temp);
	}

	@Override
	@Nullable public P read(@Nullable Value source) {

		if (source == null || TypeSystem.getDefault().NULL().isTypeOf(source)) {
			return null;
		}

		Map<K, Value> temp = new HashMap<>();
		source.keys().forEach(k -> {
			if (k.startsWith(this.prefixWithDelimiter)) {
				K key = this.keyReader.apply(k.substring(this.prefixWithDelimiter.length()));
				temp.put(key, source.get(k));
			}
		});
		return this.delegate.compose(temp, this.neo4jConversionService);
	}

	/**
	 * Internally used via reflection.
	 * @return the type of the underlying delegate.
	 */
	@SuppressWarnings("unused")
	Class<?> getClassOfDelegate() {
		return this.delegate.getClass();
	}

}
