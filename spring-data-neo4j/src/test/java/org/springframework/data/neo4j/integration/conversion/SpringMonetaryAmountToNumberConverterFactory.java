/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
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
