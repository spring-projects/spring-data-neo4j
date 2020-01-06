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
package org.springframework.data.neo4j.transaction;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.data.neo4j.queries.MoviesContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * See DATAGRAPH-967 DATAGRAPH-997 DATAGRAPH-995 DATAGRAPH-989 DATAGRAPH-952.
 *
 * @author Nicolas Mervaillie
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
public class TransactionRollbackTests {

	@Autowired private GraphDatabaseService graphDatabaseService;

	@Autowired private UserRepository userRepository;

	@Autowired private UserService userService;

	@Autowired private Session session;

	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@AfterTransaction
	public void checkOgmTransactionIsCleanedup() {
		assertThat(session.getTransaction()).isNull();
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
					assertThat(TestTransaction.isFlaggedForRollback()).isTrue();
				}
				return null;
			});
			fail("Should have thrown DataAccessException");
		} catch (Exception e) {
			// expected
		}
		assertThat(TestTransaction.isActive()).isFalse();
		try (Transaction tx = graphDatabaseService.beginTx()) {
			assertThat(graphDatabaseService.getAllNodes().iterator().hasNext()).isFalse();
			tx.success();
		}
	}

	@Test // GH-10098
	@Ignore("When an error occurs when doing a request, Neo rollbacks. See https://github.com/neo4j/neo4j/issues/10098")
	public void catchedExceptionShouldNotRollback() {

		transactionTemplate.execute((TransactionStatus tx) -> {
			userRepository.save(new User("foo"));
			try {
				userRepository.invalidQuery();
				fail("Should have failed because of invalid query");
			} catch (Exception e) {
				assertThat(TestTransaction.isActive()).isTrue();
			}
			userRepository.save(new User("bar"));
			return null;
		});
		try (Transaction tx = graphDatabaseService.beginTx()) {
			assertThat(graphDatabaseService.getAllNodes().stream().count()).isEqualTo(2);
			tx.success();
		}
	}
}
