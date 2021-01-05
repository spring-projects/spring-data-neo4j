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
package org.springframework.data.neo4j.nativetypes;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = NativeTypesContextConfiguration.class)
public class TimeFindByTests {

	@Autowired
	private TimeDomainRepository repository;

	private TimeDomain timeDomain;
	private Date date;
	private LocalDate localDate;
	private LocalDateTime localDateTime;
	private Period period;
	private TemporalAmount temporalAmount;
	private Duration duration;

	@Before
	public void setUpDomainObject() {
		repository.deleteAll();

		timeDomain = new TimeDomain();

		date = new Date();
		localDate = LocalDate.now();
		localDateTime = LocalDateTime.now();
		period = Period.ofMonths(2);
		temporalAmount = period;
		duration = Duration.ofHours(3);

		timeDomain.setDate(date);
		timeDomain.setLocalDate(localDate);
		timeDomain.setLocalDateTime(localDateTime);

		timeDomain.setPeriod(period);
		timeDomain.setDuration(duration);

		timeDomain.setTemporalAmount(temporalAmount);

		timeDomain.setArrayOfDurations(new Duration[]{Duration.ofMinutes(23)});
		timeDomain.setListOfDurations(Collections.singletonList(Duration.ofMinutes(42)));

		repository.save(timeDomain);
	}

	@Test
	public void findByDate() {
		List<TimeDomain> result = repository.findByDate(date);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByLocalDate() {
		List<TimeDomain> result = repository.findByLocalDate(localDate);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByLocalDateTime() {
		List<TimeDomain> result = repository.findByLocalDateTime(localDateTime);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByPeriod() {
		List<TimeDomain> result = repository.findByPeriod(period);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByDuration() {
		List<TimeDomain> result = repository.findByDuration(duration);
		assertThat(result).hasSize(1);
	}

	@Test
	public void findByTemporalAmount() {
		List<TimeDomain> result = repository.findByTemporalAmount(temporalAmount);
		assertThat(result).hasSize(1);
	}
}
