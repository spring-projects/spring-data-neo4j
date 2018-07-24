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
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Integration tests for disabling default transactions using JavaConfig.
 *
 * @author Mark Angrish
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = DefaultTransactionDisablingIntegrationTests.Config.class)
public class DefaultTransactionDisablingIntegrationTests extends MultiDriverTestClass {

	public @Rule ExpectedException exception = ExpectedException.none();

	@Autowired UserRepository repository;
	@Autowired TransactionalRepositoryTests.DelegatingTransactionManager txManager;

	/**
	 */
	@Test
	public void considersExplicitConfigurationOnRepositoryInterface() {

		repository.findById(1L);

		assertThat(txManager.getDefinition().isReadOnly(), is(false));
	}

	/**
	 */
	@Test
	public void doesNotUseDefaultTransactionsOnNonRedeclaredMethod() {

		repository.findAll(new PageRequest(0, 10));

		assertThat(txManager.getDefinition(), is(nullValue()));
	}

	/**
	 */
	@Test
	public void persistingAnEntityShouldThrowExceptionDueToMissingTransaction() {

		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectCause(is(Matchers.<Throwable> instanceOf(IllegalStateException.class)));

		repository.save(new User());
	}

	@Configuration
	@EnableNeo4jRepositories(basePackageClasses = UserRepository.class, enableDefaultTransactions = false)
	@EnableTransactionManagement
	static class Config {

		@Bean
		public TransactionalRepositoryTests.DelegatingTransactionManager transactionManager() throws Exception {
			return new TransactionalRepositoryTests.DelegatingTransactionManager(
					new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.domain.sample");
		}
	}
}
