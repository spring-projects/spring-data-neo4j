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

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @soundtrack Die Ärzte - Die Nacht der Dämonen
 * @since 6.0
 */
final class DefaultNeo4jConversionService implements Neo4jConversionService {

	private final ConversionService conversionService;
	private final Predicate<Class<?>> hasCustomWriteTargetPredicate;

	DefaultNeo4jConversionService(Neo4jConversions neo4jConversions) {

		final ConfigurableConversionService configurableConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(configurableConversionService);

		this.conversionService = configurableConversionService;
		this.hasCustomWriteTargetPredicate = neo4jConversions::hasCustomWriteTarget;
	}

	@Override
	@Nullable
	public <T> T convert(Object source, Class<T> targetType) {
		return conversionService.convert(source, targetType);
	}

	@Override
	public boolean hasCustomWriteTarget(Class<?> sourceType) {
		return hasCustomWriteTargetPredicate.test(sourceType);
	}

	@Override
	@Nullable
	public Object readValue(@Nullable Value source, TypeInformation<?> targetType,
			@Nullable Function<Value, Object> conversionOverride) {

		BiFunction<Value, Class<?>, Object> conversion = conversionOverride == null ?
				(v, t) -> conversionService.convert(v, t) :
				(v, t) -> conversionOverride.apply(v);

		return readValueImpl(source, targetType, conversion);
	}

	@Nullable
	private Object readValueImpl(@Nullable Value value, TypeInformation<?> type,
			BiFunction<Value, Class<?>, Object> conversion) {

		boolean valueIsLiteralNullOrNullValue = value == null || value == Values.NULL;

		try {
			Class<?> rawType = type.getType();

			if (!valueIsLiteralNullOrNullValue && isCollection(type)) {
				Collection<Object> target = CollectionFactory
						.createCollection(rawType, type.getComponentType().getType(), value.size());
				value.values()
						.forEach(element -> target.add(conversion.apply(element, type.getComponentType().getType())));
				return target;
			}

			return conversion.apply(valueIsLiteralNullOrNullValue ? null : value, rawType);
		} catch (Exception e) {
			String msg = String.format("Could not convert %s into %s", value, type.toString());
			throw new TypeMismatchDataAccessException(msg, e);
		}
	}

	@Override
	public Value writeValue(@Nullable Object value, TypeInformation<?> sourceType,
			@Nullable Function<Object, Value> writingConverter) {

		Function<Object, Value> conversion =
				writingConverter == null ? v -> conversionService.convert(v, Value.class) : writingConverter;
		return writeValueImpl(value, sourceType, conversion);
	}

	private Value writeValueImpl(@Nullable Object value, TypeInformation<?> type,
			Function<Object, Value> conversion) {

		if (value == null) {
			return Values.NULL;
		}

		if (isCollection(type)) {
			Collection<?> sourceCollection = (Collection<?>) value;
			Object[] targetCollection = (sourceCollection).stream()
					.map(conversion::apply).toArray();
			return Values.value(targetCollection);
		}

		return conversion.apply(value);
	}

	private static boolean isCollection(TypeInformation<?> type) {
		return Collection.class.isAssignableFrom(type.getType());
	}
}
