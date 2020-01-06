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
package org.springframework.data.neo4j.repository.support;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for disabling default transactions using JavaConfig.
 *
 * @author Mark Angrish
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = DefaultTransactionDisablingIntegrationTests.Config.class)
public class DefaultTransactionDisablingIntegrationTests {

	@Autowired UserRepository repository;
	@Autowired TransactionalRepositoryTests.DelegatingTransactionManager txManager;

	@Test
	public void considersExplicitConfigurationOnRepositoryInterface() {

		repository.findById(1L);

		assertThat(txManager.getDefinition().isReadOnly()).isFalse();
	}

	@Test
	public void doesNotUseDefaultTransactionsOnNonRedeclaredMethod() {

		repository.findAll(PageRequest.of(0, 10));

		assertThat(txManager.getDefinition()).isNull();
	}

	@Test
	public void persistingAnEntityShouldThrowExceptionDueToMissingTransaction() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> repository.save(new User()))
				.withCauseInstanceOf(IllegalStateException.class);
	}

	@Configuration
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.domain.sample",
			repositoryPackages = "org.springframework.data.neo4j.repository.sample",
			transactionManagerRef = "delegatingTransactionManager", enableDefaultTransactions = false)
	static class Config {

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager delegatingTransactionManager(
				Neo4jTransactionManager neo4jTransactionManager) {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(neo4jTransactionManager);
		}
	}
}
