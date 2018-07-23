/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = { TransactionIntegrationTests.MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionIntegrationTests extends MultiDriverTestClass {

	@Autowired private UserRepository userRepository;

	@Autowired private UserService userService;

	private TransactionEventHandler.Adapter<Object> handler;

	@Before
	public void populateDatabase() {
		handler = new TransactionEventHandler.Adapter<Object>() {
			@Override
			public Object beforeCommit(TransactionData data) throws Exception {
				throw new TransactionInterceptException("Deliberate testing exception");
			}
		};
		getGraphDatabaseService().registerTransactionEventHandler(handler);
	}

	@After
	public void cleanupHandler() {
		getGraphDatabaseService().unregisterTransactionEventHandler(handler);
	}

	@Test(expected = Exception.class)
	public void whenImplicitTransactionFailsNothingShouldBeCreated() {
		userRepository.save(new User("Michal"));
	}

	@Test(expected = Exception.class)
	public void whenExplicitTransactionFailsNothingShouldBeCreated() {
		userService.saveWithTxAnnotationOnInterface(new User("Michal"));
	}

	@Test(expected = Exception.class)
	public void whenExplicitTransactionFailsNothingShouldBeCreated2() {
		userService.saveWithTxAnnotationOnImpl(new User("Michal"));
	}

	static class TransactionInterceptException extends Exception {

		public TransactionInterceptException(String msg) {
			super(msg);
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
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.movies.domain");
		}
	}
}
