/*
 * Copyright 2011-2019 the original author or authors.
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
