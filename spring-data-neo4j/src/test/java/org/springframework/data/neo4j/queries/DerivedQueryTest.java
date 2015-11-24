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

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.driver.Driver;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class DerivedQueryTest {

	@Autowired
	private GraphDatabaseService graphDatabaseService;

	@Autowired
	private Driver driver;

	private Session session;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CinemaRepository cinemaRepository;

	@Autowired
	private RatingRepository ratingRepository;

	@Before
	public void init() throws IOException {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		session = new SessionFactory("org.springframework.data.neo4j.examples.movies.domain").openSession(driver);
	}

	@After
	public void clearDatabase() {
        session.purgeDatabase();
	}

	private void executeUpdate(String cypher) {
		graphDatabaseService.execute(cypher);
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
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsAnded() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 4500})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByLocationAndCapacityGreaterThan("London", 3000);
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));

		theatres = cinemaRepository.findByCapacityLessThanAndLocation(6000, "Bombay");
		assertEquals(1, theatres.size());
		assertEquals("Regal", theatres.get(0).getName());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsOred() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 9000})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByLocationOrCapacityLessThan("London", 100);
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));

		theatres = cinemaRepository.findByCapacityGreaterThanOrLocation(8000, "Paris");
		assertEquals(1, theatres.size());
		assertEquals("Regal", theatres.get(0).getName());
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

	/**
	 * @see DATAGRAPH-662
	 * //TODO FIXME
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void shouldFindNodeEntititiesWithBaseOrNestedProperty() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'The Old Vic', city:'London', capacity: 5000})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationOrVisitedName("P", "Michal");
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
		//assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("The Old Vic")));
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindNodeEntitiesWithNestedRelationshipEntityProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) " +
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->m");

		List<User> users = userRepository.findByRatingsStars(3);
		assertEquals(2, users.size());
		assertTrue(users.contains(new User("Michal")));
		assertTrue(users.contains(new User("Vince")));
	}

	/**
	 * @see DATAGRAPH-629
	 * @see DATAGRAPH-705
	 */
	@Test
	public void shouldFindNodeEntititiesWithTwoNestedPropertiesAndedAcrossDifferentRelatedNodeEntities() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				" CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				" CREATE (u:User {name:'Michal'}) " +
				" CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" +
				" CREATE (m1:Movie {name:'San Andreas'}) " +
				" CREATE (m2:Movie {name:'Pitch Perfect 2'})" +
				" CREATE (p)-[:BLOCKBUSTER]->(m1)" +
				" CREATE (r)-[:BLOCKBUSTER]->(m2)");

		List<Cinema> theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "San Andreas");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));

		theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "Tomorrowland");
		assertEquals(0, theatres.size());
	}

	/**
	 * @see DATAGRAPH-662
	 */
	//FIXME: OR is not supported for nested properties on an entity
	@Test(expected = UnsupportedOperationException.class)
	public void shouldFindNodeEntititiesWithTwoNestedPropertiesOred() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				" CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				" CREATE (u:User {name:'Michal'}) " +
				" CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" +
				" CREATE (m1:Movie {title:'San Andreas'}) " +
				" CREATE (m2:Movie {title:'Pitch Perfect 2'})" +
				" CREATE (p)-[:BLOCKBUSTER]->(m1)" +
				" CREATE (r)-[:BLOCKBUSTER]->(m2)");

		List<Cinema> theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Michal", "San Andreas");
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("Ritzy")));

		theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Vince", "Tomorrowland");
		assertEquals(0, theatres.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithMultipleNestedPropertiesAnded() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				" CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				" CREATE (u:User {name:'Michal', middleName:'M'}) CREATE (u1:User {name:'Vince', middleName:'M'}) " +
				" CREATE (u)-[:VISITED]->(p)  CREATE (u1)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Michal", "M");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Picturehouse")));

		theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Vince", "V");
		assertEquals(0, theatres.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithRelationshipEntityAndNestedProperty() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g) " +
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->m");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3,"Thriller");
		assertEquals(1, users.size());
		assertTrue(users.contains(new User("Michal")));
	}

	/**
	 * Relates to DATAGRAPH-601 and, to an extent, DATAGRAPH-761
	 */
	@Test
	public void shouldFindNodeEntitiesByRegularExpressionMatchingOnPropertiesInDerivedFinderMethods() {
        executeUpdate("CREATE (:Theatre {name:'Odeon', city:'Preston'}), "
                + "(:Theatre {name:'Vue', city:'Dumfries'}), "
                + "(:Theatre {name:'PVR', city:'Mumbai'}) ");

        // ideally, I'd name this to be "findWhereNameMatches" or "findByNameMatching"
        List<Cinema> cinemas = cinemaRepository.findByNameMatches("^[Vv].+$");
        assertEquals("The wrong number of cinemas was returned", 1, cinemas.size());
        assertEquals("An unexpected cinema was retrieved", "Dumfries", cinemas.get(0).getLocation());
	}

	/**
	 * DATAGRAPH-761
	 */
	@Test
	public void shouldMatchNodeEntitiesUsingCaseInsensitiveLikeWithWildcards() {
        executeUpdate("CREATE (:Theatre {name:'IMAX', city:'Chesterfield'}), "
                + "(:Theatre {name:'Odeon', city:'Manchester'}), "
                + "(:Theatre {name:'IMAX', city:'Edinburgh'}) ");

        List<Cinema> cinemas = cinemaRepository.findByLocationLike("*chest*");
        assertEquals("The wrong number of cinemas was returned", 2, cinemas.size());
	}

    /**
     * DATAGRAPH-761
     */
    @Test
    public void shouldMatchNodeEntitiesUsingLikeWithWildcardsAndSpecialCharacters() {
        executeUpdate("CREATE (:Theatre {name:'IMAX', city:'Kolkata (Calcutta)'}), "
                + "(:Theatre {name:'PVR', city:'Bengaluru (Bangalore)'}), "
                + "(:Theatre {name:'Metro Big Cinema', city:'Mumbai (Bombay)'}) ");

        List<Cinema> indianCinemas = cinemaRepository.findByLocationLike("*(B*");
        assertEquals("The wrong number of cinemas was returned", 2, indianCinemas.size());
    }

    /**
     * DATAGRAPH-761
     */
    @Test
    public void shouldMatchNodeEntitiesUsingNotLikeWithAsteriskWildcards() {
        executeUpdate("CREATE (:User {name:'Jeff'}), "
                + "(:User {name:'Jeremy'}), "
                + "(:User {name:'Alan'})");

        List<User> nonMatchingUsers = userRepository.findByNameIsNotLike("Je*");
        assertEquals("The wrong number of users was returned", 1, nonMatchingUsers.size());
        assertEquals("The wrong user was returned", "Alan", nonMatchingUsers.get(0).getName());
    }

}
