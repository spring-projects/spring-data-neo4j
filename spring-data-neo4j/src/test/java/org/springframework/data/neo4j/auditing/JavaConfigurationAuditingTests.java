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
package org.springframework.data.neo4j.auditing;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.annotation.EnableNeo4jAuditing;
import org.springframework.data.neo4j.auditing.domain.User;
import org.springframework.data.neo4j.auditing.domain.UserCustomIdStrategy;
import org.springframework.data.neo4j.auditing.repository.UserCustomIdStrategyRepository;
import org.springframework.data.neo4j.auditing.repository.UserRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Frantisek Hartman
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JavaConfigurationAuditingTests extends MultiDriverTestClass {

	@Configuration
	@EnableNeo4jAuditing(modifyOnCreate = false)
	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class)
	static class Neo4jConfiguration {

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), User.class.getPackage().getName());
		}

		@Bean
		public Neo4jTransactionManager transactionManager() {
			return new Neo4jTransactionManager();
		}

		@Bean
		public AuditorAware<String> auditorAware() {
			return new AuditorAware<String>() {

				@Override
				public Optional<String> getCurrentAuditor() {
					return of("userId");
				}
			};
		}
	}

	@Autowired private UserRepository userRepository;

	@Autowired private UserCustomIdStrategyRepository userCustomIdStrategyRepository;

	@Test
	public void whenSaveEntity_thenSetCreatedAndCreatedBy() throws Exception {
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
	public void whenUpdateEntity_thenSetModifiedAndModifiedBy() throws Exception {
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

	@Test // DATAGRAPH-1212
	public void shouldAuditEntitiesWithCustomIdStrategyOnCreation() {

		UserCustomIdStrategy user = new UserCustomIdStrategy("John Doe");
		userCustomIdStrategyRepository.save(user);

		Optional<UserCustomIdStrategy> loaded = userCustomIdStrategyRepository.findById(user.getId());
		assertThat(loaded).hasValueSatisfying(found -> {

			assertThat(found.getCreated()).isNotNull().isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
			assertThat(found.getCreatedBy()).isEqualTo("userId");

			assertThat(found.getModified()).isNull();
			assertThat(found.getModifiedBy()).isNull();
		});
	}

	@Test // DATAGRAPH-1212
	public void shouldAuditEntitiesWithCustomIdStrategyOnUpdate() {

		UserCustomIdStrategy user = new UserCustomIdStrategy("John Doe");
		userCustomIdStrategyRepository.save(user);

		user.setName("Johan Doe");
		userCustomIdStrategyRepository.save(user);

		Optional<UserCustomIdStrategy> loaded = userCustomIdStrategyRepository.findById(user.getId());
		assertThat(loaded).hasValueSatisfying(found -> {

			assertThat(found.getModified()).isNotNull().isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.SECONDS));
			assertThat(found.getModifiedBy()).isEqualTo("userId");
		});
	}
}
