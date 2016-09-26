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

package org.springframework.data.neo4j.queries;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metric;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.Director;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.DirectorRepository;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.restaurants.domain.Restaurant;
import org.springframework.data.neo4j.examples.restaurants.repo.RestaurantRepository;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class DerivedQueryIT extends MultiDriverTestClass {

	private static GraphDatabaseService graphDatabaseService;

	@Autowired
	private Neo4jOperations neo4jOperations;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private CinemaRepository cinemaRepository;

	@Autowired
	private RatingRepository ratingRepository;

	@Autowired
	private DirectorRepository directorRepository;

	@BeforeClass
	public static void beforeClass(){
		graphDatabaseService = getGraphDatabaseService();
	}

	@Before
	public void clearDatabase() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
		neo4jOperations.clear();
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
	 * /* * @see DATAGRAPH-628
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
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

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
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3, "Thriller");
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

	/**
	 * @see DATAGRAPH-787
	 */
	@Test
	public void shouldFindDirectorsByName() {
		executeUpdate("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'}) CREATE (d:Director {name:'Vince'})");

		Collection<Director> directors = directorRepository.findByName("Vince");
		Iterator<Director> iterator = directors.iterator();
		assertTrue(iterator.hasNext());
		Director director = iterator.next();
		assertEquals("Vince", director.getName());
		assertFalse(iterator.hasNext());

		directors = directorRepository.findByName("Michal");
		iterator = directors.iterator();
		assertFalse(iterator.hasNext());
	}


	/**
	 * @see DATAGRAPH-744
	 */
	@Test
	public void shouldFindUserWithCustomDepth() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) " +
				" CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " +
				" CREATE (u:User {name:'Michal'}) " +
				" CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" +
				" CREATE (m1:Movie {name:'San Andreas'}) " +
				" CREATE (m2:Movie {name:'Pitch Perfect 2'})" +
				" CREATE (p)-[:BLOCKBUSTER]->(m1)" +
				" CREATE (r)-[:BLOCKBUSTER]->(m2)" +
				" CREATE (u)-[:RATED {stars :3}]->(m1)");

		Cinema cinema = cinemaRepository.findByName("Picturehouse", 0);
		assertNotNull(cinema);
		assertEquals("Picturehouse", cinema.getName());
		assertEquals(0, cinema.getVisited().size());
		assertEquals(null, cinema.getBlockbusterOfTheWeek());

		cinema = cinemaRepository.findByName("Picturehouse", 1);
		assertNotNull(cinema);
		assertEquals("Picturehouse", cinema.getName());
		assertEquals(1, cinema.getVisited().size());
		assertEquals(0, cinema.getVisited().iterator().next().getRatings().size());
		assertEquals("San Andreas", cinema.getBlockbusterOfTheWeek().getName());

		neo4jOperations.clear();

		cinema = cinemaRepository.findByName("Picturehouse", 2);
		assertNotNull(cinema);
		assertEquals("Picturehouse", cinema.getName());
		assertEquals(1, cinema.getVisited().size());
		assertEquals(1, cinema.getVisited().iterator().next().getRatings().size());
		assertEquals("San Andreas", cinema.getBlockbusterOfTheWeek().getName());
	}


	/**
	 * @see DATAGRAPH-744
	 */
	@Test
	public void shouldFindUsersByNameWithStaticDepth() {
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Bachman'})<-[:FRIEND_OF]-(a:User {name:'Adam', surname:'George'})");

		User user = userRepository.findBySurname("Bachman");
		assertNotNull(user);
		assertEquals("Michal", user.getName());
		assertEquals("Bachman", user.getSurname());
		assertEquals(0, user.getFriends().size());
	}

	/**
	 * @see DATAGRAPH-864
	 */
	@Test
	public void shouldSupportLiteralMapsInQueryResults() {
		executeUpdate("CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})" +
				" CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) " +
				" CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<EntityWrappingQueryResult> result = userRepository.findRatingsWithLiteralMap();
		assertNotNull(result);
		assertEquals(2, result.size());
		EntityWrappingQueryResult row = result.get(0);
		if (row.getUser().getName().equals("Vince")) {
			assertEquals(1, row.getLiteralMap().size());
			assertEquals("Chocolat", row.getLiteralMap().iterator().next().get("movietitle"));
			assertEquals(3, row.getLiteralMap().iterator().next().get("stars"));
		} else {
			assertEquals(2, row.getLiteralMap().size());
		}
	}

	/**
	 * @throws InterruptedException
	 * @see DATAGRAPH-876
	 */
	@Test
	public void shouldAllowMultiThreadedDerivedFinderExecution() throws InterruptedException {
		int numThreads = 3;
		executeUpdate("CREATE (m:User {name:'Michal', surname:'Bachman'}), (a:User {name:'Adam', surname:'George'}), (l:User {name:'Luanne', surname:'Misquitta'})");

		neo4jOperations.clear();

		String[] firstNames = new String[]{"Michal", "Adam", "Luanne"};
		String[] lastNames = new String[]{"Bachman", "George", "Misquitta"};

		ExecutorService executor = Executors.newFixedThreadPool(numThreads);
		CountDownLatch latch = new CountDownLatch(numThreads);

		for (int i = 0; i < numThreads; i++) {
			executor.submit(new DerivedQueryRunner(latch, firstNames[i], lastNames[i]));
		}
		latch.await(); // pause until the count reaches 0

		// force termination of all threads
		executor.shutdownNow();
	}

	/**
	 * @see DATAGRAPH-680
	 */
	@Test
	public void shouldPageDerivedFinderQueries() {
		for (int i = 0; i < 10; i++) {
			userRepository.save(new User("A", "B"));
		}
		userRepository.save(new User("A", "C"));

		Pageable pageable = new PageRequest(0, 4);
		Page<User> page = userRepository.findByNameAndSurname("A", "B", pageable);
		assertEquals(4, page.getNumberOfElements());
		assertEquals(8, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertEquals(12, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertFalse(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", new PageRequest(0, 10));
		assertEquals(10, page.getNumberOfElements());
		assertEquals(20, page.getTotalElements()); //this should not be relied on as incorrect as the total elements is an estimate
		assertTrue(page.hasNext()); //this cannot be relied upon because the total number of elements is an estimate

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertEquals(0, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}

	/**
	 * @see DATAGRAPH-680
	 */
	@Test
	public void shouldSliceDerivedFinderQueries() {
		for (int i = 0; i < 10; i++) {
			User user = new User("A");
			user.rate(new TempMovie("Temp"), 5, "just okay");
			userRepository.save(user);
		}
		userRepository.save(new User("A", "C"));

		Pageable pageable = new PageRequest(0, 4);
		Slice<User> page = userRepository.findByNameAndRatingsStars("A", 5, pageable);
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndRatingsStars("A", 5, page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndRatingsStars("A", 5, page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertFalse(page.hasNext());

		page = userRepository.findByNameAndRatingsStars("A", 5, new PageRequest(0, 10));
		assertEquals(10, page.getNumberOfElements());
		assertFalse(page.hasNext());
	}


	class DerivedQueryRunner implements Runnable {

		private final CountDownLatch latch;
		private final String firstName;
		private final String lastName;

		public DerivedQueryRunner(CountDownLatch latch, String firstName, String lastName) {
			this.latch = latch;
			this.firstName = firstName;
			this.lastName = lastName;
		}

		@Override
		public void run() {
			try {
				User user = userRepository.findBySurname(lastName);
				assertNotNull(user);
				assertEquals(firstName, user.getName());
				assertEquals(lastName, user.getSurname());
			} finally {
				latch.countDown();
			}
		}
	}
}
