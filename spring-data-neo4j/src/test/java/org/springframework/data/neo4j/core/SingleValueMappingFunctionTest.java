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
package org.springframework.data.neo4j.core;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class SingleValueMappingFunctionTest {

	private final Record record;

	SingleValueMappingFunctionTest(@Mock Record record) {
		this.record = record;
	}

	@Nested
	class ShouldCheckForRecordSize {

		@Test
		void shouldNotMapNothing() {

			when(record.size()).thenReturn(0);

			SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(String.class);
			assertThatIllegalArgumentException().isThrownBy(() -> mappingFunction.apply(record))
				.withMessage("Record has no elements, cannot map nothing.");
		}

		@Test
		void shouldNotMapAmbiguousThings() {

			when(record.size()).thenReturn(23);

			SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(String.class);
			assertThatIllegalArgumentException().isThrownBy(() -> mappingFunction.apply(record))
				.withMessage("Records with more than one value cannot be converted without a mapper.");
		}
	}

	@Test
	void shouldWorkWithNullValues() {

		when(record.size()).thenReturn(1);
		when(record.get(0)).thenReturn(Values.NULL);

		SingleValueMappingFunction<String> mappingFunction = new SingleValueMappingFunction<>(String.class);
		assertThat(mappingFunction.apply(record)).isNull();
	}

	@Test
	void shouldCheckReturnType() {

		when(record.size()).thenReturn(1);
		when(record.get(0)).thenReturn(Values.value("Guten Tag."));

		SingleValueMappingFunction<Period> mappingFunction = new SingleValueMappingFunction<>(Period.class);
		assertThatIllegalArgumentException().isThrownBy(() -> mappingFunction.apply(record))
			.withMessage("java.time.Period is not assignable from java.lang.String");
	}

	@Test
	void mappingShouldWorkForSupportedTypes() {

		LocalDate aDate = LocalDate.of(2019, 4, 10);

		when(record.size()).thenReturn(1);
		when(record.get(0)).thenReturn(Values.value(aDate));

		SingleValueMappingFunction<LocalDate> mappingFunction = new SingleValueMappingFunction<>(LocalDate.class);
		assertThat(mappingFunction.apply(record)).isEqualTo(aDate);
	}
}
