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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.data.util.ClassTypeInformation;

/**
 * @author Michael J. Simons
 * @soundtrack Trettmann, KitschKrieg - Trettmann
 */
class DefaultNeo4jConverterTest {

	private final DefaultNeo4jConverter defaultNeo4jConverter = new DefaultNeo4jConverter(new Neo4jConversions(), null);

	@Nested
	class Reads {
		@Test
		void shouldCatchConversionErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class)
				.isThrownBy(() -> defaultNeo4jConverter.readValueForProperty(value, ClassTypeInformation.from(Date.class)))
				.withMessageStartingWith("Could not convert \"Das funktioniert nicht.\" into java.util.Date;")
				.withCauseInstanceOf(ConversionFailedException.class)
				.withRootCauseInstanceOf(DateTimeParseException.class);
		}

		@Test
		void shouldCatchUncoercibleErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class)
				.isThrownBy(() -> defaultNeo4jConverter.readValueForProperty(value, ClassTypeInformation.from(LocalDate.class)))
				.withMessageStartingWith("Could not convert \"Das funktioniert nicht.\" into java.time.LocalDate;")
				.withCauseInstanceOf(ConversionFailedException.class)
				.withRootCauseInstanceOf(Uncoercible.class);
		}

		@Test
		void shouldCatchUncoerfcibleErrors() {
			Value value = Values.value("Das funktioniert nicht.");

			assertThatExceptionOfType(TypeMismatchDataAccessException.class)
				.isThrownBy(
					() -> defaultNeo4jConverter.readValueForProperty(value, ClassTypeInformation.from(
						ReactiveNeo4jClient.class)))
				.withMessageStartingWith(
					"Could not convert \"Das funktioniert nicht.\" into org.springframework.data.neo4j.core.ReactiveNeo4jClient;")
				.withRootCauseInstanceOf(ConverterNotFoundException.class);
		}
	}
}
