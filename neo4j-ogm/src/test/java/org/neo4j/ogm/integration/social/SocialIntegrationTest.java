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

package org.neo4j.ogm.integration.social;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.social.Individual;
import org.neo4j.ogm.domain.social.Mortal;
import org.neo4j.ogm.domain.social.Person;
import org.neo4j.ogm.domain.social.User;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Luanne Misquitta
 */
public class SocialIntegrationTest extends InMemoryServerTest {

	@Before
	public void init() throws IOException {
		setUp();
		session = new SessionFactory("org.neo4j.ogm.domain.social").openSession(neo4jRule.baseNeoUrl());
	}

	@After
	public void clearDatabase() {
	    neo4jRule.clearDatabase();
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFetchOnlyPeopleILike() {
		session.execute("create (p1:Person {name:'A'}) create (p2:Person {name:'B'}) create (p3:Person {name:'C'})" +
				" create (p4:Person {name:'D'}) create (p1)-[:LIKES]->(p2) create (p1)-[:LIKES]->(p3) create (p4)-[:LIKES]->(p1)", Collections.EMPTY_MAP);

		Person personA = session.loadByProperty(Person.class, new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(personA);
		assertEquals(2, personA.getPeopleILike().size());

		Person personD = session.loadByProperty(Person.class, new Property<String, Object>("name","D")).iterator().next();
		assertNotNull(personD);
		assertEquals(1, personD.getPeopleILike().size());
		assertEquals(personA,personD.getPeopleILike().get(0));

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFetchFriendsInBothDirections() {
		session.execute("create (p1:Individual {name:'A'}) create (p2:Individual {name:'B'}) create (p3:Individual {name:'C'})" +
				" create (p4:Individual {name:'D'}) create (p1)-[:FRIENDS]->(p2) create (p1)-[:FRIENDS]->(p3) create (p4)-[:FRIENDS]->(p1)", Collections.EMPTY_MAP);

		Individual individualA = session.loadByProperty(Individual.class, new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(individualA);
		assertEquals(3, individualA.getFriends().size());

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFetchFriendsForUndirectedRelationship() {
		session.execute("create (p1:User {name:'A'}) create (p2:User {name:'B'}) create (p3:User {name:'C'})" +
				" create (p4:User {name:'D'}) create (p1)-[:FRIEND]->(p2) create (p1)-[:FRIEND]->(p3) create (p4)-[:FRIEND]->(p1)", Collections.EMPTY_MAP);

		User userA = session.loadByProperty(User.class, new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(userA);
		assertEquals(3, userA.getFriends().size());

		User userB = session.loadByProperty(User.class, new Property<String, Object>("name", "B")).iterator().next();
		assertNotNull(userB);
		assertEquals(1, userB.getFriends().size());
		assertEquals(userA, userB.getFriends().get(0));

		User userD = session.loadByProperty(User.class, new Property<String, Object>("name", "D")).iterator().next();
		assertNotNull(userD);
		assertEquals(1, userD.getFriends().size());
		assertEquals(userA, userD.getFriends().get(0));
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldSaveUndirectedFriends() {
		User userA = new User("A");
		User userB = new User("B");
		User userC = new User("C");
		User userD = new User("D");

		userA.getFriends().add(userB);
		userA.getFriends().add(userC);
		userD.getFriends().add(userA);

		session.save(userA);
		session.save(userB);
		session.save(userC);
		session.save(userD);

		session.clear();

		userA = session.loadByProperty(User.class, new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(userA);
		assertEquals(3, userA.getFriends().size());

		userB = session.loadByProperty(User.class, new Property<String, Object>("name", "B")).iterator().next();
		assertNotNull(userB);
		assertEquals(1, userB.getFriends().size());
		assertEquals(userA.getName(), userB.getFriends().get(0).getName());

		userD = session.loadByProperty(User.class, new Property<String, Object>("name", "D")).iterator().next();
		assertNotNull(userD);
		assertEquals(1, userD.getFriends().size());
		assertEquals(userA.getName(), userD.getFriends().get(0).getName());
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldSaveUndirectedFriendsInBothDirections() {
		Person userA = new Person("A");
		Person userB = new Person("B");

		userA.getPeopleILike().add(userB);
		userB.getPeopleILike().add(userA);

		session.save(userA);

		session.clear();
		userA = session.loadByProperty(Person.class, new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(userA);
		assertEquals(1, userA.getPeopleILike().size());
		session.clear();
		userB = session.loadByProperty(Person.class, new Property<String, Object>("name", "B")).iterator().next();
		assertNotNull(userB);
		assertEquals(1, userB.getPeopleILike().size());
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldSaveIncomingKnownMortals() {
		Mortal mortalA = new Mortal("A");
		Mortal mortalB = new Mortal("B");
		Mortal mortalC = new Mortal("C");
		Mortal mortalD = new Mortal("D");

		mortalA.getKnownBy().add(mortalB);
		mortalA.getKnownBy().add(mortalC);
		mortalD.getKnownBy().add(mortalA);

		session.save(mortalA);
		session.save(mortalB);
		session.save(mortalC);
		session.save(mortalD);

		session.clear();

		mortalA = session.loadByProperty(Mortal.class,new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(mortalA);
		assertEquals(2, mortalA.getKnownBy().size());

		mortalB = session.loadByProperty(Mortal.class, new Property<String, Object>("name","B")).iterator().next();
		assertNotNull(mortalB);
		assertEquals(0, mortalB.getKnownBy().size());

		mortalC = session.loadByProperty(Mortal.class, new Property<String, Object>("name", "C")).iterator().next();
		assertNotNull(mortalC);
		assertEquals(0, mortalC.getKnownBy().size());

		mortalD = session.loadByProperty(Mortal.class, new Property<String, Object>("name", "D")).iterator().next();
		assertNotNull(mortalD);
		assertEquals(1, mortalD.getKnownBy().size());
		assertEquals("A", mortalD.getKnownBy().iterator().next().getName());
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void shouldFetchIncomingKnownMortals() {
		session.execute("create (m1:Mortal {name:'A'}) create (m2:Mortal {name:'B'}) create (m3:Mortal {name:'C'})" +
				" create (m4:Mortal {name:'D'}) create (m1)<-[:KNOWN_BY]-(m2) create (m1)<-[:KNOWN_BY]-(m3) create (m4)<-[:KNOWN_BY]-(m1)", Collections.EMPTY_MAP);

		Mortal mortalA = session.loadByProperty(Mortal.class,new Property<String, Object>("name","A")).iterator().next();
		assertNotNull(mortalA);
		assertEquals(2, mortalA.getKnownBy().size());

		Mortal mortalB = session.loadByProperty(Mortal.class, new Property<String, Object>("name","B")).iterator().next();
		assertNotNull(mortalB);
		assertEquals(0, mortalB.getKnownBy().size());

		Mortal mortalC = session.loadByProperty(Mortal.class, new Property<String, Object>("name", "C")).iterator().next();
		assertNotNull(mortalC);
		assertEquals(0, mortalC.getKnownBy().size());

		Mortal mortalD = session.loadByProperty(Mortal.class, new Property<String, Object>("name", "D")).iterator().next();
		assertNotNull(mortalD);
		assertEquals(1, mortalD.getKnownBy().size());
		assertEquals("A", mortalD.getKnownBy().iterator().next().getName());
	}
}