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
package org.springframework.data.neo4j.core.convert;

import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Values;

/**
 * @author Michael J. Simons
 */
class TemporalAmountAdapterTest {
	@Test
	public void internallyCreatedTypesShouldBeConvertedCorrect() {
		final TemporalAmountAdapter adapter = new TemporalAmountAdapter();

		assertThat(adapter.apply(Values.isoDuration(1, 0, 0, 0).asIsoDuration())).isEqualTo(Period.ofMonths(1));
		assertThat(adapter.apply(Values.isoDuration(1, 1, 0, 0).asIsoDuration()))
			.isEqualTo(Period.ofMonths(1).plusDays(1));
		assertThat(adapter.apply(Values.isoDuration(1, 1, 1, 0).asIsoDuration()))
			.isEqualTo(Values.isoDuration(1, 1, 1, 0).asIsoDuration());
		assertThat(adapter.apply(Values.isoDuration(0, 0, 120, 1).asIsoDuration()))
			.isEqualTo(Duration.ofMinutes(2).plusNanos(1));
	}

	@Test
	public void durationsShouldStayDurations() {
		final TemporalAmountAdapter adapter = new TemporalAmountAdapter();

		Duration duration =
			MONTHS.getDuration().multipliedBy(13).plus(DAYS.getDuration().multipliedBy(32)).plusHours(25)
				.plusMinutes(120);

		assertThat(adapter.apply(Values.value(duration).asIsoDuration())).isEqualTo(duration);
	}

	@Test
	public void periodsShouldStayPeriods() {
		final TemporalAmountAdapter adapter = new TemporalAmountAdapter();

		Period period = Period.between(LocalDate.of(2018, 11, 15), LocalDate.of(2020, 12, 24));

		assertThat(adapter.apply(Values.value(period).asIsoDuration())).isEqualTo(period.normalized());
	}
}
