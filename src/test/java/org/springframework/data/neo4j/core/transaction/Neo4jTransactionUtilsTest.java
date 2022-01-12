/*
 * Copyright 2011-2022 the original author or authors.
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
package org.springframework.data.neo4j.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.neo4j.driver.TransactionConfig;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Neo4jTransactionUtilsTest {

	@Nested
	class DatabaseNameComparision {

		@ParameterizedTest
		@CsvSource({ ",", "a,a" })
		void nameComparisionShouldWorkForNamesTargetingTheSame(String name1, String name2) {

			Assertions.assertThat(Neo4jTransactionUtils.namesMapToTheSameDatabase(name1, name2)).isTrue();
		}

		@ParameterizedTest
		@CsvSource({ "a,", ",b", "a,b" })
		void nameComparisionShouldWorkForNamesTargetingOther(String name1, String name2) {

			Assertions.assertThat(Neo4jTransactionUtils.namesMapToTheSameDatabase(name1, name2)).isFalse();
		}
	}

	@ParameterizedTest // GH-2463
	@ValueSource(ints = { Integer.MIN_VALUE, -1, 0, DefaultTransactionDefinition.TIMEOUT_DEFAULT })
	void shouldNotApplyNegativeOrZeroTimeOuts(int value) {

		DefaultTransactionDefinition springDef = new DefaultTransactionDefinition();
		springDef.setTimeout(DefaultTransactionDefinition.TIMEOUT_DEFAULT);
		TransactionConfig driverConfig = Neo4jTransactionUtils.createTransactionConfigFrom(springDef, value);
		assertThat(driverConfig.timeout()).isNull();
	}

	@ParameterizedTest // GH-2463
	@ValueSource(ints = { Integer.MIN_VALUE, -1, 0 })
	void shouldPreferTxDef(int value) {

		DefaultTransactionDefinition springDef = new DefaultTransactionDefinition();
		springDef.setTimeout(2);
		TransactionConfig driverConfig = Neo4jTransactionUtils.createTransactionConfigFrom(springDef, value);
		assertThat(driverConfig.timeout()).isEqualTo(Duration.ofSeconds(2));
	}

	@Test // GH-2463
	void shouldFallbackToTxManagerDefault() {

		DefaultTransactionDefinition springDef = new DefaultTransactionDefinition();
		springDef.setTimeout(DefaultTransactionDefinition.TIMEOUT_DEFAULT);
		TransactionConfig driverConfig = Neo4jTransactionUtils.createTransactionConfigFrom(springDef, 3);
		assertThat(driverConfig.timeout()).isEqualTo(Duration.ofSeconds(3));
	}
}
