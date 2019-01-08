/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.domain.sample.User;
import org.springframework.data.neo4j.repository.sample.UserRepository;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Mark Angrish
 * @author Jens Schauder
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = TransactionalRepositoryTests.Config.class)
@RunWith(SpringRunner.class)
public class TransactionalRepositoryTests {

	@Autowired GraphDatabaseService graphDatabaseService;
	@Autowired TransactionTemplate transactionTemplate;

	@Autowired UserRepository repository;
	@Autowired DelegatingTransactionManager transactionManager;

	@Before
	public void setUp() {

		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		transactionManager.resetCount();
	}

	@After
	public void tearDown() {

		repository.deleteAll();
	}

	@Test
	public void simpleManipulatingOperation() {

		repository.save(new User("foo", "bar", "foo@bar.de"));
		assertThat(transactionManager.getTransactionRequests(), is(1));
	}

	@Test
	public void unannotatedFinder() {

		repository.findByEmailAddress("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests(), is(0));
	}

	@Test
	public void invokeTransactionalFinder() {

		repository.findByAnnotatedQuery("foo@bar.de");
		assertThat(transactionManager.getTransactionRequests(), is(1));
	}

	@Test
	public void invokeRedeclaredMethod() {

		repository.findById(1L);
		assertFalse(transactionManager.getDefinition().isReadOnly());
	}

	@Test
	public void invokeRedeclaredDeleteMethodWithoutTransactionDeclaration() {

		User user = repository.save(new User("foo", "bar", "foo@bar.de"));
		repository.deleteById(user.getId());

		assertFalse(transactionManager.getDefinition().isReadOnly());
	}

	static class DelegatingTransactionManager implements PlatformTransactionManager {

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
	@Neo4jIntegrationTest(domainPackages = "org.springframework.data.neo4j.domain.sample",
			transactionManagerRef = "delegatingTransactionManager",
			repositoryPackages = "org.springframework.data.neo4j.repository.sample")
	static class Config {

		@Bean
		public DelegatingTransactionManager delegatingTransactionManager(Neo4jTransactionManager neo4jTransactionManager) {
			return new DelegatingTransactionManager(neo4jTransactionManager);
		}
	}
}
