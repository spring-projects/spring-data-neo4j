/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
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
