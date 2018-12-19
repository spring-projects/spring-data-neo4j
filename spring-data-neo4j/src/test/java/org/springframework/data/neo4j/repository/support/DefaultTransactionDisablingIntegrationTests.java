/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

	public @Rule ExpectedException exception = ExpectedException.none();

	@Autowired UserRepository repository;
	@Autowired TransactionalRepositoryTests.DelegatingTransactionManager txManager;

	@Test
	public void considersExplicitConfigurationOnRepositoryInterface() {

		repository.findById(1L);

		assertThat(txManager.getDefinition().isReadOnly(), is(false));
	}

	@Test
	public void doesNotUseDefaultTransactionsOnNonRedeclaredMethod() {

		repository.findAll(PageRequest.of(0, 10));

		assertThat(txManager.getDefinition(), is(nullValue()));
	}

	@Test
	public void persistingAnEntityShouldThrowExceptionDueToMissingTransaction() {

		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectCause(is(Matchers.<Throwable> instanceOf(IllegalStateException.class)));

		repository.save(new User());
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
