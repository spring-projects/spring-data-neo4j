/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.integration.friendships;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.friendships.Friendship;
import org.neo4j.ogm.domain.friendships.Person;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Vince Bickers
 */
public class FriendshipsRelationshipEntityTest  extends InMemoryServerTest {

	private static SessionFactory sessionFactory;

	@Before
	public void init() throws IOException {
		setUp();
		sessionFactory = new SessionFactory("org.neo4j.ogm.domain.friendships");
		session = sessionFactory.openSession("http://localhost:" + neoPort);
	}

	@Test
	public void shouldSaveFromStartObjectSetsAllObjectIds() {

		Person mike = new Person("Mike");
		Person dave = new Person("Dave");

		// could use addFriend(...) but hey
		dave.getFriends().add(new Friendship(dave, mike, 5));

		session.save(dave);

		assertNotNull(dave.getId());
		assertNotNull(mike.getId());
		assertNotNull(dave.getFriends().get(0).getId());

	}

	@Test
	public void shouldSaveAndReloadAllSetsAllObjectIdsAndReferencesCorrectly() {

		Person mike = new Person("Mike");
		Person dave = new Person("Dave");
		dave.getFriends().add(new Friendship(dave, mike, 5));

		session.save(dave);

		Collection<Person> personList = session.loadAll(Person.class);

		int expected = 2;
		assertEquals(expected, personList.size());
		for (Person person : personList) {
			if (person.getName().equals("Dave")) {
				expected--;
				assertEquals("Mike", person.getFriends().get(0).getFriend().getName());
			}
			else if (person.getName().equals("Mike")) {
				expected--;
				assertEquals("Dave", person.getFriends().get(0).getPerson().getName());
			}
		}
		assertEquals(0, expected);
	}

	@Test
	public void shouldSaveFromRelationshipEntitySetsAllObjectIds() {

		Person mike = new Person("Mike");
		Person dave = new Person("Dave");

		Friendship friendship = new Friendship(dave, mike, 5);
		dave.getFriends().add(friendship);

		session.save(friendship);

		assertNotNull(dave.getId());
		assertNotNull(mike.getId());
		assertNotNull(dave.getFriends().get(0).getId());

	}

	@Test
	public void shouldLoadStartObjectHydratesProperly() {

		Person mike = new Person("Mike");
		Person dave = new Person("Dave");
		Friendship friendship = new Friendship(dave, mike, 5);
		dave.getFriends().add(friendship);

		session.save(dave);

		Person daveCopy = session.load(Person.class, dave.getId());
		Friendship friendshipCopy = daveCopy.getFriends().get(0);
		Person mikeCopy = friendshipCopy.getFriend();

		assertNotNull(daveCopy.getId());
		assertNotNull(mikeCopy.getId());
		assertNotNull(friendshipCopy.getId());

		assertEquals("Dave", daveCopy.getName());
		assertEquals("Mike", mikeCopy.getName());
		assertEquals(5, friendshipCopy.getStrength());

	}

	@Test
	public void shouldLoadRelationshipEntityObjectHydratesProperly() {

		Person mike = new Person("Mike");
		Person dave = new Person("Dave");
		Friendship friendship = new Friendship(dave, mike, 5);
		dave.getFriends().add(friendship);

		session.save(dave);

		Friendship friendshipCopy = session.load(Friendship.class, friendship.getId());
		Person daveCopy = friendshipCopy.getPerson();
		Person mikeCopy = friendshipCopy.getFriend();

		assertNotNull(daveCopy.getId());
		assertNotNull(mikeCopy.getId());
		assertNotNull(friendshipCopy.getId());

		assertEquals("Dave", daveCopy.getName());
		assertEquals("Mike", mikeCopy.getName());
		assertEquals(5, friendshipCopy.getStrength());

	}
}
