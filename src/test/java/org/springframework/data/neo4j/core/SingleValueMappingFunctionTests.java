/*
 * Copyright 2011-present the original author or authors.
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

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.TypeSystem;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class SingleValueMappingFunctionTests {

	private final TypeSystem typeSystem;

	private final Record record;

	private final ConversionService conversionService;

	SingleValueMappingFunctionTests(@Mock TypeSystem typeSystem, @Mock Record record) {
		this.typeSystem = typeSystem;
		this.record = record;
		this.conversionService = new DefaultConversionService();
		new Neo4jConversions().registerConvertersIn((ConverterRegistry) this.conversionService);

	}

	@Test
	void shouldWorkWithNullValues() {

		given(this.record.size()).willReturn(1);
		given(this.record.get(0)).willReturn(Values.NULL);

		SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(this.conversionService,
				String.class);
		assertThat(mappingFunction.apply(this.typeSystem, this.record)).isNull();
	}

	@Test
	void shouldCheckReturnType() {

		given(this.record.size()).willReturn(1);
		given(this.record.get(0)).willReturn(Values.value("Guten Tag."));

		SingleValueMappingFunction<Period> mappingFunction = new SingleValueMappingFunction<>(this.conversionService,
				Period.class);

		assertThatExceptionOfType(ConversionFailedException.class)
			.isThrownBy(() -> mappingFunction.apply(this.typeSystem, this.record))
			.withMessageContainingAll("Failed to convert from type", "org.neo4j.driver.internal.value.StringValue",
					"to type [java.time.Period] for value");
	}

	@Test
	void mappingShouldWorkForSupportedTypes() {

		LocalDate aDate = LocalDate.of(2019, 4, 10);

		given(this.record.size()).willReturn(1);
		given(this.record.get(0)).willReturn(Values.value(aDate));

		SingleValueMappingFunction<LocalDate> mappingFunction = new SingleValueMappingFunction<>(this.conversionService,
				LocalDate.class);
		assertThat(mappingFunction.apply(this.typeSystem, this.record)).isEqualTo(aDate);
	}

	@Nested
	class ShouldCheckForRecordSize {

		@Test
		void shouldNotMapNothing() {

			given(SingleValueMappingFunctionTests.this.record.size()).willReturn(0);

			SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(
					SingleValueMappingFunctionTests.this.conversionService, String.class);
			assertThatIllegalArgumentException()
				.isThrownBy(() -> mappingFunction.apply(SingleValueMappingFunctionTests.this.typeSystem,
						SingleValueMappingFunctionTests.this.record))
				.withMessage("Record has no elements, cannot map nothing");
		}

		@Test
		void shouldNotMapAmbiguousThings() {

			given(SingleValueMappingFunctionTests.this.record.size()).willReturn(23);

			SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(
					SingleValueMappingFunctionTests.this.conversionService, String.class);
			assertThatIllegalArgumentException()
				.isThrownBy(() -> mappingFunction.apply(SingleValueMappingFunctionTests.this.typeSystem,
						SingleValueMappingFunctionTests.this.record))
				.withMessage("Records with more than one value cannot be converted without a mapper");
		}

	}

}
