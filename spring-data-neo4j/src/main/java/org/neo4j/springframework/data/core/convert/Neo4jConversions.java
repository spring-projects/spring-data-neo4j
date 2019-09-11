/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.core.convert;

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.core.CollectionFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.lang.Nullable;

/**
 * @author Michael J. Simons
 * @soundtrack The Kleptones - A Night At The Hip-Hopera
 * @since 1.0
 */
@API(status = API.Status.STABLE, since = "1.0")
public final class Neo4jConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;
	private static final List<Object> STORE_CONVERTERS;
	private static final TypeDescriptor TYPE_DESCRIPTOR_OF_VALUE = TypeDescriptor.valueOf(Value.class);

	static {

		List<Object> converters = new ArrayList<>();

		converters.addAll(CypherTypes.CONVERTERS);
		converters.addAll(AdditionalTypes.CONVERTERS);
		converters.addAll(SpatialTypes.CONVERTERS);

		STORE_CONVERTERS = Collections.unmodifiableList(converters);
		STORE_CONVERSIONS = StoreConversions.of(Neo4jSimpleTypes.HOLDER, STORE_CONVERTERS);
	}

	/**
	 * Creates a {@link Neo4jConversions} object without custom converters.
	 */
	public Neo4jConversions() {
		this(Collections.emptyList());
	}

	/**
	 * Creates a new {@link CustomConversions} instance registering the given converters.
	 *
	 * @param converters must not be {@literal null}.
	 */
	public Neo4jConversions(Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

	@Override
	public void registerConvertersIn(ConverterRegistry conversionService) {
		super.registerConvertersIn(conversionService);

		// Those can only be added at this point, as they will delegate to the target conversion service.
		conversionService.addConverter(new ValueToCollectionConverter((ConversionService) conversionService));
		conversionService.addConverter(new CollectionToValueConverter((ConversionService) conversionService));
	}

	private static class ValueToCollectionConverter implements GenericConverter {

		private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = Collections
			.singleton(new ConvertiblePair(Value.class, Collection.class));
		private final ConversionService conversionService;

		ValueToCollectionConverter(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		@Override
		@Nullable
		public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return null;
			}

			Value value = (Value) source;
			TypeDescriptor elementDesc = targetType.getElementTypeDescriptor();
			Collection<Object> target = CollectionFactory.createCollection(targetType.getType(),
				(elementDesc != null ? elementDesc.getType() : null), value.size());

			if (elementDesc == null) {
				target.addAll(value.asList());
			} else {
				value.values().forEach(sourceElement -> target.add(this.conversionService.convert(sourceElement,
					TYPE_DESCRIPTOR_OF_VALUE, elementDesc)));
			}

			return target;
		}
	}

	private static class CollectionToValueConverter implements GenericConverter {

		private static final Set<ConvertiblePair> CONVERTIBLE_TYPES = Collections
			.singleton(new ConvertiblePair(Collection.class, Value.class));

		private final ConversionService conversionService;

		CollectionToValueConverter(ConversionService conversionService) {
			this.conversionService = conversionService;
		}

		@Override
		public Set<ConvertiblePair> getConvertibleTypes() {
			return CONVERTIBLE_TYPES;
		}

		@Override
		@Nullable
		public Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
			if (source == null) {
				return null;
			}
			Collection<?> sourceCollection = (Collection<?>) source;

			return Values.value((sourceCollection).stream().map(v -> conversionService
				.convert(v, sourceType.elementTypeDescriptor(v), TYPE_DESCRIPTOR_OF_VALUE))
				.collect(toList()));
		}
	}
}
