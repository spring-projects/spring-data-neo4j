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

import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import java.io.IOException;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.social.Individual;
import org.neo4j.ogm.domain.social.Mortal;
import org.neo4j.ogm.domain.social.Person;
import org.neo4j.ogm.domain.social.User;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

/**
 * @author Luanne Misquitta
 */
public class SocialRelationshipsIntegrationTest extends WrappingServerIntegrationTest {

	private Session session;

	@Override
	protected int neoServerPort() {
		return 7896;
	}

	@Before
	public void init() throws IOException {
		SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.social");
		session = sessionFactory.openSession("http://localhost:" + 7896);
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveUndirectedSavesOutgoingRelationship() {
		User userA = new User("A");
		User userB = new User("B");
		userA.getFriends().add(userB);
		session.save(userA);

		assertSameGraph(getDatabase(), "CREATE (a:User {name:'A'}) CREATE (b:User {name:'B'}) CREATE (a)-[:FRIEND]->(b)");
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveUnmarkedSavesOutgoingRelationship() {
		Individual individualA = new Individual();
		individualA.setName("A");
		Individual individualB = new Individual();
		individualB.setName("B");
		individualA.setFriends(Collections.singletonList(individualB));
		session.save(individualA);

		assertSameGraph(getDatabase(), "CREATE (a:Individual {name:'A', age: 0}) CREATE (b:Individual {name:'B', age:0}) CREATE (a)-[:FRIENDS]->(b)");
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveOutgoingSavesOutgoingRelationship() {
		Person personA = new Person("A");
		Person personB = new Person("B");
		personA.getPeopleILike().add(personB);
		session.save(personA);
		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) CREATE (a)-[:LIKES]->(b)");

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveIncomingSavesIncomingRelationship() {
		Mortal mortalA = new Mortal("A");
		Mortal mortalB = new Mortal("B");
		mortalA.getKnownBy().add(mortalB);
		session.save(mortalA);
		assertSameGraph(getDatabase(), "CREATE (a:Mortal {name:'A'}) CREATE (b:Mortal {name:'B'}) CREATE (a)<-[:KNOWN_BY]-(b)");

	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveOutgoingSavesOutgoingRelationshipInBothDirections() {
		Person personA = new Person("A");
		Person personB = new Person("B");
		personA.getPeopleILike().add(personB);
		personB.getPeopleILike().add(personA);
		session.save(personA);

		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) " +
				"CREATE (a)-[:LIKES]->(b) CREATE (b)-[:LIKES]->(a)");
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void saveOutgoingToExistingNodesSavesOutgoingRelationshipInBothDirections() {
		Person personA = new Person("A");
		Person personB = new Person("B");
		session.save(personA);
		session.save(personB);
		personA.getPeopleILike().add(personB);
		personB.getPeopleILike().add(personA);
		session.save(personA);
		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) " +
				"CREATE (a)-[:LIKES]->(b) CREATE (b)-[:LIKES]->(a)");
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void updateOutgoingRelSavesOutgoingRelationshipInBothDirections() {
		Person personA = new Person("A");
		Person personB = new Person("B");
		Person personC = new Person("C");
		personA.getPeopleILike().add(personB);
		personB.getPeopleILike().add(personA);
		session.save(personA);
		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) " +
				"CREATE (a)-[:LIKES]->(b) CREATE (b)-[:LIKES]->(a)");

		personA.getPeopleILike().clear();
		personA.getPeopleILike().add(personC);
		personC.getPeopleILike().add(personA);
		session.save(personA);
		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) CREATE (c:Person {name:'C'}) " +
				" CREATE (a)-[:LIKES]->(c) CREATE (c)-[:LIKES]->(a) CREATE (b)-[:LIKES]->(a)");
	}

	/**
	 * @see DATAGRAPH-594
	 */
	@Test
	public void updateOutgoingRelInListSavesOutgoingRelationshipInBothDirections() {
		Person personA = new Person("A");
		Person personB = new Person("B");
		Person personC = new Person("C");
		Person personD = new Person("D");
		personA.getPeopleILike().add(personB);
		personA.getPeopleILike().add(personC);
		personB.getPeopleILike().add(personA);
		personD.getPeopleILike().add(personA);

		session.save(personA);
		session.save(personB);
		session.save(personC);
		session.save(personD);
		assertSameGraph(getDatabase(), "CREATE (a:Person {name:'A'}) CREATE (b:Person {name:'B'}) CREATE (c:Person {name:'C'}) CREATE (d:Person {name:'D'})" +
				"CREATE (a)-[:LIKES]->(b) CREATE (a)-[:LIKES]->(c) CREATE (b)-[:LIKES]->(a) CREATE (d)-[:LIKES]->(a)");

	}





}
