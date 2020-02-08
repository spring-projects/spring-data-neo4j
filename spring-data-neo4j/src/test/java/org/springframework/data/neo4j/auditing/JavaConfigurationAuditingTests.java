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
