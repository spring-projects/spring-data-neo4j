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
package org.springframework.data.neo4j.integration.conversion;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConditionalConverter;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.data.neo4j.integration.conversion.domain.MonetaryAmount;

/**
 * Not a properly-implemented factory, but it does serve the purpose of helping to verify that converters defined for
 * abstract supertypes can still be used correctly.
 *
 * @author Adam George
 */
public class SpringMonetaryAmountToNumberConverterFactory
		implements ConverterFactory<MonetaryAmount, Number>, ConditionalConverter {

	private final SpringMonetaryAmountToIntegerConverter wrappedConverter = new SpringMonetaryAmountToIntegerConverter();

	@Override
	public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
		return MonetaryAmount.class.isAssignableFrom(sourceType.getType())
				&& Number.class.isAssignableFrom(targetType.getType());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends Number> Converter<MonetaryAmount, T> getConverter(Class<T> targetType) {
		return (Converter<MonetaryAmount, T>) this.wrappedConverter;
	}

}
