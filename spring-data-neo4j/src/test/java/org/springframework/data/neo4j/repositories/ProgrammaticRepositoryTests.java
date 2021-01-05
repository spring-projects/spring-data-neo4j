/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.repositories;

import static org.junit.Assert.*;
import static org.springframework.data.neo4j.test.GraphDatabaseServiceAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.data.neo4j.repositories.domain.Movie;
import org.springframework.data.neo4j.repositories.domain.User;
import org.springframework.data.neo4j.repositories.repo.MovieRepository;
import org.springframework.data.neo4j.repositories.repo.UserRepository;
import org.springframework.data.neo4j.repository.support.Neo4jRepositoryFactory;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Vince Bickers
 * @author Michael J. Simons
 */
public class ProgrammaticRepositoryTests {

	private static ServerControls serverControls;
	private static SessionFactory sessionFactory;
	private static TransactionTemplate transactionTemplate;

	private MovieRepository movieRepository;
	private Session session;

	@BeforeClass
	public static void oneTimeSetUp() {
		serverControls = TestServerBuilders.newInProcessBuilder().newServer();

		Configuration configuration = new Configuration.Builder() //
				.uri(serverControls.boltURI().toString()) //
				.build();
		sessionFactory = new SessionFactory(configuration, "org.springframework.data.neo4j.repositories.domain");

		PlatformTransactionManager platformTransactionManager = new Neo4jTransactionManager(sessionFactory);
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
	}

	@AfterClass
	public static void shutdownTestServer() {

		if (sessionFactory != null) {
			sessionFactory.close();
			sessionFactory = null;
		}

		if (serverControls != null) {
			serverControls.close();
			serverControls = null;
		}
	}

	@Before
	public void init() {
		session = sessionFactory.openSession();
		session.purgeDatabase();
	}

	@Test
	public void canInstantiateRepositoryProgrammatically() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

				movieRepository = factory.getRepository(MovieRepository.class);

				Movie movie = new Movie("PF");
				movieRepository.save(movie);
			}
		});

		Map<String, Object> params = new HashMap<>();
		params.put("title", "PF");
		assertThat(serverControls.graph()).containsNode("MATCH (n:Movie) WHERE n.title = $title RETURN n", params);

		assertEquals(1, StreamSupport.stream(movieRepository.findAll().spliterator(), false).count());
	}

	@Test // DATAGRAPH-847
	public void shouldBeAbleToDeleteAllViaRepository() {

		RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

		UserRepository userRepository = factory.getRepository(UserRepository.class);

		User userA = new User("A");
		User userB = new User("B");
		userRepository.save(userA);
		userRepository.save(userB);

		assertEquals(2, userRepository.count());

		userRepository.deleteAll();
		assertEquals(0, userRepository.count());
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteUserByNameAndReturnCountOfDeletedUsers() {

		RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

		UserRepository userRepository = factory.getRepository(UserRepository.class);

		User userA = new User("A");
		userA.setName("A");

		userRepository.save(userA);
		Assert.assertEquals(1, userRepository.count());

		Assert.assertEquals(new Long(1), userRepository.deleteByName("A"));
		Assert.assertEquals(0, userRepository.count());
	}

	@Test // DATAGRAPH-813
	public void shouldDeleteUserByNameAndReturnListOfDeletedUserIds() {

		RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

		UserRepository userRepository = factory.getRepository(UserRepository.class);

		User userA = new User("A");
		User userAClone = new User("A");

		userRepository.save(userA);
		userRepository.save(userAClone);

		Assert.assertEquals(2, userRepository.count());

		List<Long> deletedUserIds = userRepository.removeByName("A");
		Assert.assertEquals(2, deletedUserIds.size());

		Assertions.assertThat(deletedUserIds).containsExactlyInAnyOrder(userA.getId(), userAClone.getId());

		Assert.assertEquals(0, userRepository.count());
	}

	@Test
	public void shouldBeAbleToDeleteUserWithRelationships() {

		RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

		UserRepository userRepository = factory.getRepository(UserRepository.class);

		User userA = new User("A");
		User userB = new User("B");

		userA.getFriends().add(userB);
		userB.getFriends().add(userA);

		userRepository.save(userA);

		Assert.assertEquals(2, userRepository.count());

		userRepository.deleteByName("A");

		Assert.assertEquals(1, userRepository.count());
	}

	@Test // DATAGRAPH-813
	public void shouldCountUserByName() {

		RepositoryFactorySupport factory = new Neo4jRepositoryFactory(session);

		UserRepository userRepository = factory.getRepository(UserRepository.class);

		User userA = new User("A");
		userA.setName("A");

		userRepository.save(userA);

		Assert.assertEquals(new Long(1), userRepository.countByName("A"));
	}
}
