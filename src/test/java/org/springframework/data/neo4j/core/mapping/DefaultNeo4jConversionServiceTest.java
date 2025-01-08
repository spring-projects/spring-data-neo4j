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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAmount;
import java.util.Date;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.exceptions.value.Uncoercible;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.dao.TypeMismatchDataAccessException;
import org.springframework.data.neo4j.core.ReactiveNeo4jClient;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.util.TypeInformation;

/**
 * @author Michael J. Simons
 * @soundtrack Trettmann, KitschKrieg - Trettmann
 */
class DefaultNeo4jConversionServiceTest {

	private final DefaultNeo4jConversionService defaultNeo4jEntityAccessor = new DefaultNeo4jConversionService(new Neo4jConversions());

	@Nested
	class Reads {

		@Test // GH-2324
		void shouldDealWith0IsoDurationsAsPeriods() {
			Value zeroDuration = Values.isoDuration(0, 0, 0, 0);

			Period period = (Period) defaultNeo4jEntityAccessor.readValue(zeroDuration, TypeInformation.of(Period.class), null);
			assertThat(period.isZero()).isTrue();
		}

		@Test // GH-2324
		void shouldDealWith0IsoDurationsAsDurations() {
			Value zeroDuration = Values.isoDuration(0, 0, 0, 0);

			Duration duration = (Duration) defaultNeo4jEntityAccessor.readValue(zeroDuration, TypeInformation.of(Duration.class), null);
			assertThat(duration).isZero();
		}

		@Test // GH-2324
		void shouldDealWithNullTemporalValueOnRead() {
			Duration duration = (Duration) defaultNeo4jEntityAccessor.readValue(null, TypeInformation.of(Duration.class), null);
			assertThat(duration).isNull();
		}

		@Test // GH-2324
		void shouldDealWithNullTemporalValueOnWrite() {
			Value value = defaultNeo4jEntityAccessor.writeValue(null, TypeInformation.of(TemporalAmount.class), null);
			assertThat(value).isNull();
		}

		@Test
		void shouldCatchConversionErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class)
					.isThrownBy(() -> defaultNeo4jEntityAccessor.readValue(value, TypeInformation.of(Date.class), null))
					.withMessageStartingWith("Could not convert \"Das funktioniert nicht.\" into java.util.Date")
					.withCauseInstanceOf(ConversionFailedException.class).withRootCauseInstanceOf(DateTimeParseException.class);
		}

		@Test
		void shouldCatchUncoercibleErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class)
					.isThrownBy(
							() -> defaultNeo4jEntityAccessor.readValue(value, TypeInformation.of(LocalDate.class), null))
					.withMessageStartingWith("Could not convert \"Das funktioniert nicht.\" into java.time.LocalDate")
					.withCauseInstanceOf(ConversionFailedException.class).withRootCauseInstanceOf(Uncoercible.class);
		}

		@Test
		void shouldCatchCoercibleErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class).isThrownBy(
					() -> defaultNeo4jEntityAccessor.readValue(value, TypeInformation.of(ReactiveNeo4jClient.class), null))
					.withMessageStartingWith(
							"Could not convert \"Das funktioniert nicht.\" into org.springframework.data.neo4j.core.ReactiveNeo4jClient")
					.withRootCauseInstanceOf(ConverterNotFoundException.class);
		}
	}
}
