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

import java.util.function.BiFunction;

import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.core.convert.ConversionService;
import org.springframework.lang.Nullable;

/**
 * Used to automatically map single valued records to a sensible Java type based on {@link Value#asObject()}.
 *
 * @author Michael J. Simons
 * @param <T> type of the domain class to map
 * @since 6.0
 */
final class SingleValueMappingFunction<T> implements BiFunction<TypeSystem, Record, T> {

	private final ConversionService conversionService;

	private final Class<T> targetClass;

	SingleValueMappingFunction(ConversionService conversionService, Class<T> targetClass) {
		this.conversionService = conversionService;
		this.targetClass = targetClass;
	}

	@Nullable
	@Override
	public T apply(TypeSystem typeSystem, Record record) {

		if (record.size() == 0) {
			throw new IllegalArgumentException("Record has no elements, cannot map nothing.");
		}

		if (record.size() > 1) {
			throw new IllegalArgumentException("Records with more than one value cannot be converted without a mapper.");
		}

		Value source = record.get(0);
		if (targetClass == Void.class || targetClass == void.class) {
			return null;
		}
		return source == null || source == Values.NULL ? null : conversionService.convert(source, targetClass);
	}
}
