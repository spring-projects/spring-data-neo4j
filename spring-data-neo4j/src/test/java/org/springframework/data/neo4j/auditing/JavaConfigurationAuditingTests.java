/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 *  conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.auditing;

import static java.util.Optional.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.annotation.EnableNeo4jAuditing;
import org.springframework.data.neo4j.auditing.domain.User;
import org.springframework.data.neo4j.auditing.repository.UserRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Frantisek Hartman
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration
public class JavaConfigurationAuditingTests {

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.auditing.domain",
			repositoryPackages = "org.springframework.data.neo4j.auditing.repository")
	@EnableNeo4jAuditing(modifyOnCreate = false)
	static class Neo4jConfiguration {

		@Bean
		public AuditorAware<String> auditorAware() {
			return () -> of("userId");
		}
	}

	@Autowired private UserRepository userRepository;

	@Test
	public void whenSaveEntity_thenSetCreatedAndCreatedBy() {
		User user = new User("John Doe");
		userRepository.save(user);

		Optional<User> loaded = userRepository.findById(user.getId());
		assertThat(loaded).isPresent();

		User found = loaded.get();
		assertThat(found.getCreated()).isNotNull().isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
		assertThat(found.getCreatedBy()).isEqualTo("userId");

		assertThat(found.getModified()).isNull();
		assertThat(found.getModifiedBy()).isNull();
	}

	@Test
	public void whenUpdateEntity_thenSetModifiedAndModifiedBy() {
		User user = new User("John Doe");
		userRepository.save(user);

		user.setName("Johan Doe");
		userRepository.save(user);

		Optional<User> loaded = userRepository.findById(user.getId());
		assertThat(loaded).isPresent();

		User found = loaded.get();

		assertThat(found.getModified()).isNotNull().isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
		assertThat(found.getModifiedBy()).isEqualTo("userId");
	}
}
