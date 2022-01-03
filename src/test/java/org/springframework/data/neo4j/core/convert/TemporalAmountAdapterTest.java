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
package org.springframework.data.neo4j.core.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.IsoDuration;

/**
 * @author Michael J. Simons
 */
class TemporalAmountAdapterTest {

	private final TemporalAmountAdapter underTest = new TemporalAmountAdapter();

	@Test
	public void internallyCreatedTypesShouldBeConvertedCorrect() {

		assertThat(underTest.apply(Values.isoDuration(1, 0, 0, 0).asIsoDuration())).isEqualTo(Period.ofMonths(1));
		assertThat(underTest.apply(Values.isoDuration(1, 1, 0, 0).asIsoDuration())).isEqualTo(Period.ofMonths(1).plusDays(1));
		assertThat(underTest.apply(Values.isoDuration(1, 1, 1, 0).asIsoDuration()))
				.isEqualTo(Values.isoDuration(1, 1, 1, 0).asIsoDuration());
		assertThat(underTest.apply(Values.isoDuration(0, 0, 120, 1).asIsoDuration()))
				.isEqualTo(Duration.ofMinutes(2).plusNanos(1));
	}

	@Test
	public void durationsShouldStayDurations() {

		Duration duration = ChronoUnit.MONTHS.getDuration().multipliedBy(13).plus(ChronoUnit.DAYS.getDuration().multipliedBy(32)).plusHours(25)
				.plusMinutes(120);

		assertThat(underTest.apply(Values.value(duration).asIsoDuration())).isEqualTo(duration);
	}

	@Test
	public void periodsShouldStayPeriods() {

		Period period = Period.between(LocalDate.of(2018, 11, 15), LocalDate.of(2020, 12, 24));

		assertThat(underTest.apply(Values.value(period).asIsoDuration())).isEqualTo(period.normalized());
	}

	@Test // GH-2324
	public void zeroDurationShouldReturnTheIsoDuration() {

		IsoDuration zeroDuration = Values.isoDuration(0, 0, 0, 0).asIsoDuration();
		assertThat(underTest.apply(zeroDuration)).isSameAs(zeroDuration);
	}
}
