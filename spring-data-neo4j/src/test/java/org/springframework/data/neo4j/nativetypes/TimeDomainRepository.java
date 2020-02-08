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
package org.springframework.data.neo4j.nativetypes;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;

public interface TimeDomainRepository extends Neo4jRepository<TimeDomain, Long> {
	List<TimeDomain> findByDate(Date date);

	List<TimeDomain> findByLocalDate(LocalDate localDate);

	List<TimeDomain> findByLocalDateTime(LocalDateTime localDateTime);

	List<TimeDomain> findByPeriod(Period period);

	List<TimeDomain> findByDuration(Duration duration);

	List<TimeDomain> findByTemporalAmount(TemporalAmount temporalAmount);
}
