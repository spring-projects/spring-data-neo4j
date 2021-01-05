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
		MetaDataDrivenConversionService.EntityToGraphTypeMapping entityToGraphTypeMapping = MetaDataDrivenConversionService
				.getEntityToGraphTypeMapping(new Converters.DoubleToStringConverter());

		assertThat(entityToGraphTypeMapping.entityType).isEqualTo(Double.class);
		assertThat(entityToGraphTypeMapping.graphType).isEqualTo(String.class);
	}

	@Test
	public void shouldDetermineConvertersForTypedClasses() {
		MetaDataDrivenConversionService.EntityToGraphTypeMapping entityToGraphTypeMapping = MetaDataDrivenConversionService
				.getEntityToGraphTypeMapping(new Converters.ListToStringConverter());

		assertThat(entityToGraphTypeMapping.entityType).isEqualTo(Double.class);
		assertThat(entityToGraphTypeMapping.graphType).isEqualTo(String.class);
	}

	@Test // DATAGRAPH-1131
	public void shouldWorkWithConvertersInvolvingAbstractBaseClasses() {
		MetaDataDrivenConversionService.EntityToGraphTypeMapping entityToGraphTypeMapping = MetaDataDrivenConversionService
				.getEntityToGraphTypeMapping(new Converters.ConvertedClassToStringConverter());

		assertThat(entityToGraphTypeMapping.entityType).isEqualTo(ConvertedClass.class);
		assertThat(entityToGraphTypeMapping.graphType).isEqualTo(String.class);
	}

}
