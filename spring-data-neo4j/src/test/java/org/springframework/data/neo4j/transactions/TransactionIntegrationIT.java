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

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michal Bachman
 */
@ContextConfiguration(classes = { MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionIntegrationIT extends MultiDriverTestClass {

	private static GraphDatabaseService graphDatabaseService;

	@Autowired private UserRepository userRepository;

	@Autowired private UserService userService;

	@BeforeClass
	public static void beforeClass() {
		graphDatabaseService = getGraphDatabaseService();
	}

	@Before
	public void populateDatabase() {
		graphDatabaseService.registerTransactionEventHandler(new TransactionEventHandler.Adapter<Object>() {
			@Override
			public Object beforeCommit(TransactionData data) throws Exception {
				System.out.println("The request to commit is denied");
				throw new TransactionInterceptException("Deliberate testing exception");
			}
		});
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
}
