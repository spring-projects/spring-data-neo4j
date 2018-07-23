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

package org.springframework.data.neo4j.transactions;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Nicolas Mervaillie
 * @see DATAGRAPH-967 DATAGRAPH-997 DATAGRAPH-995 DATAGRAPH-989 DATAGRAPH-952
 */
@ContextConfiguration(classes = { TransactionRollbackTests.MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionRollbackTests extends MultiDriverTestClass {

	@Autowired private UserRepository userRepository;

	@Autowired private UserService userService;

	@Autowired private Session session;

	@Autowired private PlatformTransactionManager platformTransactionManager;

	private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@AfterTransaction
	public void checkOgmTransactionIsCleanedup() {
		assertNull(session.getTransaction());
	}

	@Test
	public void queryErrorShouldRollbackAndThrowDataAccessException() {

		try {
			transactionTemplate.execute((TransactionStatus tx) -> {
				User user = new User("foo");
				userRepository.save(user);
				try {
					userRepository.invalidQuery();
				} finally {
					assertTrue(TestTransaction.isFlaggedForRollback());
				}
				return null;
			});
			fail("Should have thrown DataAccessException");
		} catch (Exception e) {
			// expected
		}
		assertFalse(TestTransaction.isActive());
		try (Transaction tx = getGraphDatabaseService().beginTx()) {
			assertFalse(getGraphDatabaseService().getAllNodes().iterator().hasNext());
			tx.success();
		}
	}

	// see https://github.com/neo4j/neo4j/issues/10098
	@Test
	@Ignore("When an error occurs when doing a request, Neo rollbacks. See https://github.com/neo4j/neo4j/issues/10098")
	public void catchedExceptionShouldNotRollback() {

		transactionTemplate.execute((TransactionStatus tx) -> {
			userRepository.save(new User("foo"));
			try {
				userRepository.invalidQuery();
				fail("Should have failed because of invalid query");
			} catch (Exception e) {
				assertTrue(TestTransaction.isActive());
			}
			userRepository.save(new User("bar"));
			return null;
		});
		try (Transaction tx = getGraphDatabaseService().beginTx()) {
			assertEquals(2, getGraphDatabaseService().getAllNodes().stream().count());
			tx.success();
		}
	}

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.examples.movies.service" })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.movies.repo")
	@EnableTransactionManagement
	static class MoviesContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory("org.springframework.data.neo4j.examples.movies.domain");
		}
	}
}
