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
package org.springframework.data.neo4j.core;

import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.core.convert.ConversionService;

/**
 * Used to automatically map single valued records to a sensible Java type based on
 * {@link Value#asObject()}.
 *
 * @param <T> type of the domain class to map
 * @author Michael J. Simons
 * @since 6.0
 */
final class SingleValueMappingFunction<T> implements BiFunction<TypeSystem, Record, T> {

	private final ConversionService conversionService;

	private final Class<T> targetClass;

	SingleValueMappingFunction(ConversionService conversionService, Class<T> targetClass) {
		this.conversionService = conversionService;
		this.targetClass = targetClass;
	}

	@Override
	@Nullable public T apply(TypeSystem typeSystem, Record record) {

		if (record.size() == 0) {
			throw new IllegalArgumentException("Record has no elements, cannot map nothing");
		}

		if (record.size() > 1) {
			throw new IllegalArgumentException("Records with more than one value cannot be converted without a mapper");
		}

		return convertValue(record.get(0));
	}

	@Nullable T convertValue(Value source) {
		if (this.targetClass == Void.class || this.targetClass == void.class) {
			return null;
		}
		return (source == null || source == Values.NULL) ? null
				: this.conversionService.convert(source, this.targetClass);
	}

}
