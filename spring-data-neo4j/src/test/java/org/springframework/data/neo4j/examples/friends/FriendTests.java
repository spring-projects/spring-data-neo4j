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

package org.springframework.data.neo4j.examples.friends;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.examples.friends.repo.FriendshipRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 */
@ContextConfiguration(classes = { FriendTests.FriendContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class FriendTests extends MultiDriverTestClass {

	@Autowired Session session;
	@Autowired FriendshipRepository friendshipRepository;
	@Autowired FriendService friendService;

	@Before
	public void cleanUpDatabase() {
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	/**
	 * @see DATAGRAPH-703
	 */
	@Test
	@Transactional
	public void savingPersonWhenTransactionalShouldWork() {
		friendService.createPersonAndFriends();

		session.clear();
		Person john = session.loadAll(Person.class, new Filter("firstName", ComparisonOperator.EQUALS, "John")).iterator()
				.next();
		assertNotNull(john);
		assertEquals(2, john.getFriendships().size());
	}

	/**
	 * @see DATAGRAPH-694
	 */
	@Test
	@Transactional
	public void circularParametersShouldNotProduceInfiniteRecursion() {
		Person john = new Person();
		john.setFirstName("John");
		session.save(john);

		Person bob = new Person();
		bob.setFirstName("Bob");
		session.save(bob);

		Friendship friendship1 = john.addFriend(bob);
		friendship1.setTimestamp(System.currentTimeMillis());
		session.save(john);

		Friendship queriedFriendship = friendshipRepository.getFriendship(john, bob);

		assertNotNull(queriedFriendship);
		assertEquals("John", queriedFriendship.getPersonStartNode().getFirstName());
		assertEquals("Bob", queriedFriendship.getPersonEndNode().getFirstName());
	}

	@Configuration
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.friends.repo")
	@ComponentScan(basePackageClasses = FriendService.class)
	@EnableTransactionManagement
	static class FriendContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.friends.domain");
		}
	}

}
