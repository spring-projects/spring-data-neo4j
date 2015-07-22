/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.examples.friends;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.friends.context.FriendContext;
import org.springframework.data.neo4j.examples.friends.domain.Friendship;
import org.springframework.data.neo4j.examples.friends.domain.Person;
import org.springframework.data.neo4j.examples.friends.repo.FriendshipRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {FriendContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class FriendTest {

	@Autowired Session session;
	@Autowired FriendshipRepository friendshipRepository;
	@Autowired FriendService friendService;

	/**
	 * @see DATAGRAPH-703
	 */
	@Test
	public void savingPersonWhenTransactionalShouldWork() {
		friendService.createPersonAndFriends();

		session.clear();
		Person john = session.loadAll(Person.class, new Filter("firstName", "John")).iterator().next();
		assertNotNull(john);
		assertEquals(2, john.getFriendships().size());;
	}

	/**
	 * @see DATAGRAPH-694
	 */
	@Test
	public void circularParamtersShouldNotProduceInfiniteRecursion() {
		Person john = new Person();
		john.setFirstName("John");
		session.save(john);

		Person bob = new Person();
		bob.setFirstName("Bob");
		session.save(bob);

		Friendship friendship1 = john.addFriend(bob);
		friendship1.setTimestamp(System.currentTimeMillis());
		session.save(john);

		Friendship queriedFriendship = friendshipRepository.getFriendship(john,bob);
		assertNotNull(queriedFriendship);
		assertEquals("John",queriedFriendship.getPersonStartNode().getFirstName());
		assertEquals("Bob",queriedFriendship.getPersonEndNode().getFirstName());

	}
}
