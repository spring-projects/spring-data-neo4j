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

package org.springframework.data.neo4j.integration.movies;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.*;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.movies.context.PersistenceContext;
import org.springframework.data.neo4j.integration.movies.domain.Cinema;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.integration.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.integration.movies.repo.UserRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class DerivedQueryTest {


	@ClassRule
	public static Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

	private static Session session;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CinemaRepository cinemaRepository;

	@Before
	public void init() throws IOException {
		session = new SessionFactory("org.springframework.data.neo4j.integration.movies.domain").openSession(neo4jRule.baseNeoUrl());
	}

	@After
	public void clearDatabase() {
		neo4jRule.clearDatabase();
	}

	private void executeUpdate(String cypher) {
		new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute(cypher);
	}

	@Ignore
	@Test //TODO fix
	public void shouldFindUsersByName() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		Collection<User> users = userRepository.findByName("Michal");
		Iterator<User> iterator = users.iterator();
		assertTrue(iterator.hasNext());
		User user = iterator.next();
		assertEquals("Michal", user.getName());
		assertEquals(1, user.getFriends().size());
		assertEquals("Adam", user.getFriends().iterator().next().getName());
		assertFalse(iterator.hasNext());
	}

	@Ignore
	@Test //TODO fix
	public void shouldFindUsersByMiddleName() {
		executeUpdate("CREATE (m:User {middleName:'Joseph'})<-[:FRIEND_OF]-(a:User {middleName:'Mary', name: 'Joseph'})");

		Collection<User> users = userRepository.findByMiddleName("Joseph");
		Iterator<User> iterator = users.iterator();
		assertTrue(iterator.hasNext());
		User user = iterator.next();
		assertEquals("Joseph", user.getMiddleName());
		assertEquals(1, user.getFriends().size());
		User friend = user.getFriends().iterator().next();
		assertEquals("Mary", friend.getMiddleName());
		assertEquals("Joseph", friend.getName());
		assertFalse(iterator.hasNext());
	}

	/**
	/* * @see DATAGRAPH-628
	 */
	@Test
	public void shouldFindNodeEntitiesWithLabels() {
		executeUpdate("CREATE (u:User {name:'Michal'}) CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'}) CREATE (u)-[:VISITED]->(p)");

		Collection<Cinema> cinemas = cinemaRepository.findByName("Picturehouse");
		Iterator<Cinema> iterator = cinemas.iterator();
		assertTrue(iterator.hasNext());
		Cinema cinema = iterator.next();
		assertEquals("Picturehouse", cinema.getName());
		assertEquals(1, cinema.getVisited().size());
		assertEquals("Michal", cinema.getVisited().iterator().next().getName());
		assertFalse(iterator.hasNext());

		List<Cinema> theatres = cinemaRepository.findByLocation("London");
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}

	@Test
	public void shouldFindNodeEntitiesMultipleAndedProperties() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameAndLocation("Ritzy", "London");
		assertEquals(1, theatres.size());
		assertEquals("Michal", theatres.get(0).getVisited().iterator().next().getName());
	}

	@Test
	public void shouldFindNodeEntititiesMultipleOredProperties() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameOrLocation("Ritzy", "London");
		assertEquals(2, theatres.size());
	}
}
