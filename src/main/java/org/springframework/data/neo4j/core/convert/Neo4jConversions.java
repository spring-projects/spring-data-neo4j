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
package org.springframework.data.neo4j.core.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apiguardian.api.API;

import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.data.convert.CustomConversions;

/**
 * Manages all build-in Neo4j conversions: Cypher types, some additional types and the
 * shared set of Spring Data and Neo4j spatial types.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public final class Neo4jConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;

	private static final List<Object> STORE_CONVERTERS;

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
	 * @param converters must not be {@literal null}.
	 */
	public Neo4jConversions(Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

	@Override
	public void registerConvertersIn(ConverterRegistry conversionService) {
		super.registerConvertersIn(conversionService);
		conversionService.addConverter(new AdditionalTypes.EnumArrayConverter());
	}

}
