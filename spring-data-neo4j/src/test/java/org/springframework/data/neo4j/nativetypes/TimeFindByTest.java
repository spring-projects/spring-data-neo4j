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

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@ContextConfiguration(classes = TimeFindByTest.TimePersistenceContext.class)
@RunWith(SpringRunner.class)
public class TimeFindByTest extends MultiDriverTestClass {

	private static org.neo4j.ogm.config.Configuration configuration;

	@Autowired private TimeDomainRepository repository;

	private TimeDomain timeDomain;
	private Date date;
	private LocalDate localDate;
	private LocalDateTime localDateTime;
	private Period period;
	private TemporalAmount temporalAmount;
	private Duration duration;

	@Before
	public void setUpDomainObject() {
		Assume.assumeFalse(runsInHttpMode());
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

	private boolean runsInHttpMode() {
		return configuration.getURI().startsWith("http");
	}

	@Configuration
	@EnableNeo4jRepositories
	@EnableTransactionManagement
	static class TimePersistenceContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {

			configuration = getBaseConfiguration().useNativeTypes().build();

			return new SessionFactory(configuration, "org.springframework.data.neo4j.nativetypes");
		}
	}
}
