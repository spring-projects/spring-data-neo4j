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

import java.math.BigInteger;

import org.springframework.core.convert.converter.Converter;

/**
 * Nonsensical Spring converter for testing purposes
 *
 * @author Luanne Misquitta
 */
public class SpringBooleanToBigIntegerConverter implements Converter<Boolean, BigInteger> {

	@Override
	public BigInteger convert(Boolean source) {
		if (source) {
			return BigInteger.valueOf(101);
		}
		return BigInteger.valueOf(1);
	}
}
