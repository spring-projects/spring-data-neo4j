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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Mark Angrish
 * @author Jens Schauder
 */
@ContextConfiguration(classes = { TransactionalRepositoryTests.Config.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionalRepositoryTests extends MultiDriverTestClass {

	@Autowired TransactionTemplate transactionTemplate;

	@Autowired UserRepository repository;
	@Autowired DelegatingTransactionManager transactionManager;

	@Before
	public void setUp() {

		transactionTemplate = new TransactionTemplate(transactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		transactionManager.resetCount();
	}

	@After
	public void tearDown() {

		repository.deleteAll();
	}

	@Test
	public void simpleManipulatingOperation() throws Exception {

		repository.save(new User("foo", "bar", "foo@bar.de"));
		assertThat(transactionManager.getTransactionRequests(), is(1));
	}

	@Test
	public void unannotatedFinder() throws Exception {

		repository.findByEmailAddress("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests(), is(0));
	}

	@Test
	public void invokeTransactionalFinder() throws Exception {

		repository.findByAnnotatedQuery("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests(), is(1));
	}

	@Test
	public void invokeRedeclaredMethod() throws Exception {

		repository.findById(1L);
		assertFalse(transactionManager.getDefinition().isReadOnly());
	}

	@Test
	public void invokeRedeclaredDeleteMethodWithoutTransactionDeclaration() throws Exception {

		User user = repository.save(new User("foo", "bar", "foo@bar.de"));
		repository.deleteById(user.getId());

		assertFalse(transactionManager.getDefinition().isReadOnly());
	}

	public static class DelegatingTransactionManager implements PlatformTransactionManager {

		private PlatformTransactionManager txManager;
		private int transactionRequests;
		private TransactionDefinition definition;

		public DelegatingTransactionManager(PlatformTransactionManager txManager) {

			this.txManager = txManager;
		}

		public void commit(TransactionStatus status) throws TransactionException {

			txManager.commit(status);
		}

		public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {

			this.transactionRequests++;
			this.definition = definition;

			return txManager.getTransaction(definition);
		}

		public int getTransactionRequests() {

			return transactionRequests;
		}

		public TransactionDefinition getDefinition() {

			return definition;
		}

		public void resetCount() {

			this.transactionRequests = 0;
			this.definition = null;
		}

		public void rollback(TransactionStatus status) throws TransactionException {

			txManager.rollback(status);
		}
	}

	@Configuration
	@EnableNeo4jRepositories("org.springframework.data.neo4j.repository.sample")
	@EnableTransactionManagement
	static class Config {

		@Bean
		public DelegatingTransactionManager transactionManager() {
			return new DelegatingTransactionManager(new Neo4jTransactionManager(sessionFactory()));
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(), "org.springframework.data.neo4j.domain.sample");
		}

		@Bean
		public TransactionTemplate transactionTemplate() {
			return new TransactionTemplate(transactionManager());
		}
	}
}
