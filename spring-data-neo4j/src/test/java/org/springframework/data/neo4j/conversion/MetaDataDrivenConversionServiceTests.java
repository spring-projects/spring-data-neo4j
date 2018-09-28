/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.conversion;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.data.neo4j.conversion.support.ConvertedClass;
import org.springframework.data.neo4j.conversion.support.Converters;

/**
 * @author Michael J. Simons
 * @soundtrack Murray Gold - Doctor Who Season 9
 */
public class MetaDataDrivenConversionServiceTests extends MultiDriverTestClass {

	@Test
	public void shouldDetermineConvertersForClasses() {
		MetaDataDrivenConversionService.PairOfTypes pairOfTypes = MetaDataDrivenConversionService
				.getPairOfTypes(new Converters.DoubleToStringConverter());

		assertThat(pairOfTypes.sourceType).isEqualTo(Double.class);
		assertThat(pairOfTypes.targetType).isEqualTo(String.class);
	}

	@Test
	public void shouldDetermineConvertersForTypedClasses() {
		MetaDataDrivenConversionService.PairOfTypes pairOfTypes = MetaDataDrivenConversionService
				.getPairOfTypes(new Converters.ListToStringConverter());

		assertThat(pairOfTypes.sourceType).isEqualTo(Double.class);
		assertThat(pairOfTypes.targetType).isEqualTo(String.class);
	}

	@Test // DATAGRAPH-1131
	public void shouldWorkWithConvertersInvolvingAbstractBaseClasses() {
		MetaDataDrivenConversionService.PairOfTypes pairOfTypes = MetaDataDrivenConversionService
				.getPairOfTypes(new Converters.ConvertedClassToStringConverter());

		assertThat(pairOfTypes.sourceType).isEqualTo(ConvertedClass.class);
		assertThat(pairOfTypes.targetType).isEqualTo(String.class);
	}

}
