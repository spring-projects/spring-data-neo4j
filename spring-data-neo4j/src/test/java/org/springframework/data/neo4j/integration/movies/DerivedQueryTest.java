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

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.movies.context.PersistenceContext;
import org.springframework.data.neo4j.integration.movies.domain.Cinema;
import org.springframework.data.neo4j.integration.movies.domain.Rating;
import org.springframework.data.neo4j.integration.movies.domain.TempMovie;
import org.springframework.data.neo4j.integration.movies.domain.User;
import org.springframework.data.neo4j.integration.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.integration.movies.repo.RatingRepository;
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

	@Autowired
	private RatingRepository ratingRepository;

	@Before
	public void init() throws IOException {
		session = new SessionFactory("org.springframework.data.neo4j.integration.movies.domain").openSession(neo4jRule.url());
	}

	@After
	public void clearDatabase() {
		neo4jRule.clearDatabase();
	}

	private void executeUpdate(String cypher) {
		new ExecutionEngine(neo4jRule.getGraphDatabaseService()).execute(cypher);
	}

	@Test
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

	@Test
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

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntitiesMultipleAndedProperties() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameAndLocation("Ritzy", "London");
		assertEquals(1, theatres.size());
		assertEquals("Michal", theatres.get(0).getVisited().iterator().next().getName());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesMultipleOredProperties() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameOrLocation("Ritzy", "London");
		assertEquals(2, theatres.size());
	}


	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldReturnNoResultsCorrectly() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		Collection<Cinema> theatres = cinemaRepository.findByName("Does not exist");
		assertEquals(0, theatres.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindREWithSingleProperty() {
		User critic = new User("Gary");
		TempMovie film = new TempMovie("Fast and Furious XVII");
		Rating filmRating = critic.rate(film, 2, "They've made far too many of these films now!");


		userRepository.save(critic);

		List<Rating> ratings = ratingRepository.findByStars(2);
		assertNotNull(ratings);
		Rating loadedRating = ratings.get(0);
		assertNotNull("The loaded rating shouldn't be null", loadedRating);
		assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
		assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
		assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithComparisonOperators() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByCapacityGreaterThan(3000);
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));

		theatres = cinemaRepository.findByCapacityGreaterThan(6000);
		assertEquals(1, theatres.size());
		assertEquals("Ritzy", theatres.get(0).getName());

		theatres = cinemaRepository.findByCapacityLessThan(8000);
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));

		theatres = cinemaRepository.findByCapacityLessThan(7000);
		assertEquals(1, theatres.size());
		assertEquals("Picturehouse", theatres.get(0).getName());

	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithNestedProperty() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByVisitedName("Michal");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithBaseAndNestedProperty() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 5000})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationAndVisitedName("London", "Michal");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}
}
