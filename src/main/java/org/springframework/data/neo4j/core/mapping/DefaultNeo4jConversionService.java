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
package org.springframework.data.neo4j.core.mapping;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;

import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.core.TypeInformation;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.core.convert.Neo4jConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyConverter;

/**
 * Default implementation for all {@link Neo4jConversionService Neo4j specific conversion
 * services}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class DefaultNeo4jConversionService implements Neo4jConversionService {

	private final ConversionService conversionService;

	private final Predicate<Class<?>> hasCustomWriteTargetPredicate;

	private final SimpleTypeHolder simpleTypes;

	DefaultNeo4jConversionService(Neo4jConversions neo4jConversions) {

		final ConfigurableConversionService configurableConversionService = new DefaultConversionService();
		neo4jConversions.registerConvertersIn(configurableConversionService);

		this.conversionService = configurableConversionService;
		this.hasCustomWriteTargetPredicate = neo4jConversions::hasCustomWriteTarget;
		this.simpleTypes = neo4jConversions.getSimpleTypeHolder();
	}

	private static boolean isCollection(TypeInformation<?> type) {
		return Collection.class.isAssignableFrom(type.getType());
	}

	@Override
	@Nullable public <T> T convert(Object source, Class<T> targetType) {
		return this.conversionService.convert(source, targetType);
	}

	@Override
	public boolean hasCustomWriteTarget(Class<?> sourceType) {
		return this.hasCustomWriteTargetPredicate.test(sourceType);
	}

	@Override
	@Nullable public Object readValue(@Nullable Value source, TypeInformation<?> targetType,
			@Nullable Neo4jPersistentPropertyConverter<?> conversionOverride) {

		BiFunction<Value, Class<?>, Object> conversion;
		boolean applyConversionToCompleteCollection = false;
		if (conversionOverride == null) {
			conversion = this.conversionService::convert;
		}
		else {
			applyConversionToCompleteCollection = conversionOverride instanceof NullSafeNeo4jPersistentPropertyConverter
					&& ((NullSafeNeo4jPersistentPropertyConverter<?>) conversionOverride).isForCollection();
			conversion = (v, t) -> conversionOverride.read(v);
		}

		return readValueImpl(source, targetType, conversion, applyConversionToCompleteCollection);
	}

	@Nullable private Object readValueImpl(@Nullable Value value, TypeInformation<?> type,
			BiFunction<Value, Class<?>, Object> conversion, boolean applyConversionToCompleteCollection) {

		boolean valueIsLiteralNullOrNullValue = value == null || value == Values.NULL;

		try {
			Class<?> rawType = type.getType();

			if (!valueIsLiteralNullOrNullValue && isCollection(type) && !applyConversionToCompleteCollection) {
				// value can't be null at this point in time
				@SuppressWarnings("NullAway")
				Collection<Object> target = CollectionFactory.createCollection(rawType,
						Objects.requireNonNull(type.getComponentType()).getType(), value.size());
				value.values()
					.forEach(element -> target.add(conversion.apply(element, type.getComponentType().getType())));
				return target;
			}
			return valueIsLiteralNullOrNullValue ? null : conversion.apply(value, rawType);
		}
		catch (Exception ex) {
			String msg = String.format("Could not convert %s into %s", value, type);
			throw new TypeMismatchDataAccessException(msg, ex);
		}
	}

	@Override
	public Value writeValue(@Nullable Object value, TypeInformation<?> sourceType,
			@Nullable Neo4jPersistentPropertyConverter<?> writingConverter) {

		Function<Object, Value> conversion;
		boolean applyConversionToCompleteCollection = false;
		if (writingConverter == null) {
			conversion = v -> this.conversionService.convert(v, Value.class);
		}
		else {
			@SuppressWarnings("unchecked")
			Neo4jPersistentPropertyConverter<Object> hlp = (Neo4jPersistentPropertyConverter<Object>) writingConverter;
			applyConversionToCompleteCollection = writingConverter instanceof NullSafeNeo4jPersistentPropertyConverter
					&& ((NullSafeNeo4jPersistentPropertyConverter<?>) writingConverter).isForCollection();
			conversion = hlp::write;
		}

		return writeValueImpl(value, sourceType, conversion, applyConversionToCompleteCollection);
	}

	private Value writeValueImpl(@Nullable Object value, TypeInformation<?> type, Function<Object, Value> conversion,
			boolean applyConversionToCompleteCollection) {

		if (value == null) {
			try {
				// Some conversion services may treat null special, so we pass it anyway
				// and ask for forgiveness
				return conversion.apply(null);
			}
			catch (NullPointerException ex) {
				return Values.NULL;
			}
		}

		if (isCollection(type) && !applyConversionToCompleteCollection) {
			Collection<?> sourceCollection = (Collection<?>) value;
			Object[] targetCollection = (sourceCollection).stream().map(conversion::apply).toArray();
			return Values.value(targetCollection);
		}

		return conversion.apply(value);
	}

	@Override
	public boolean isSimpleType(Class<?> type) {
		return this.simpleTypes.isSimpleType(type);
	}

}
