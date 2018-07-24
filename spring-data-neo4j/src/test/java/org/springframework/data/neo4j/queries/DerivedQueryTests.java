/*
 * Copyright (c)  [2011-2018] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.Director;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.domain.queryresult.EntityWrappingQueryResult;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.DirectorRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Luanne Misquitta
 * @author Mark Angrish
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = { DerivedQueryTests.MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class DerivedQueryTests extends MultiDriverTestClass {

	@Autowired PlatformTransactionManager platformTransactionManager;

	@Autowired private Session session;

	@Autowired private UserRepository userRepository;

	@Autowired private CinemaRepository cinemaRepository;

	@Autowired private DirectorRepository directorRepository;

	private TransactionTemplate transactionTemplate;

	@Before
	public void clearDatabase() {
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	private void executeUpdate(String cypher) {
		getGraphDatabaseService().execute(cypher);
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
	 * @see DATAGRAPH-628
	 */
	@Test
	public void shouldFindNodeEntitiesWithLabels() {
		executeUpdate(
				"CREATE (u:User {name:'Michal'}) CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'}) CREATE (u)-[:VISITED]->(p)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
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
		});
	}

	@Test
	public void shouldUseDepthOnCollectionFinder() {
		executeUpdate(
				"CREATE (u:User {name:'Michal'}) CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'}) CREATE (u)-[:VISITED]->(p)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {

				List<Cinema> theatres = cinemaRepository.findByLocation("London", 0);
				assertEquals(2, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));
				assertTrue(theatres.contains(new Cinema("Ritzy")));
				assertNull(theatres.get(0).getBlockbusterOfTheWeek());
				assertTrue(theatres.get(0).getVisited().isEmpty());
			}
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntitiesMultipleAndedProperties() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameAndLocation("Ritzy", "London");
		assertEquals(1, theatres.size());
		assertEquals("Michal", theatres.get(0).getVisited().iterator().next().getName());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesMultipleOredProperties() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByNameOrLocation("Ritzy", "London");
		assertEquals(2, theatres.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldReturnNoResultsCorrectly() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London'}) CREATE (r:Theatre {name:'Ritzy', city:'London'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		Collection<Cinema> theatres = cinemaRepository.findByName("Does not exist");
		assertEquals(0, theatres.size());
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithComparisonOperators() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
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
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsAnded() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 4500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByLocationAndCapacityGreaterThan("London", 3000);
				assertEquals(2, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));
				assertTrue(theatres.contains(new Cinema("Ritzy")));

				theatres = cinemaRepository.findByCapacityLessThanAndLocation(6000, "Bombay");
				assertEquals(1, theatres.size());
				assertEquals("Regal", theatres.get(0).getName());
			}
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithMultipleComparisonOperatorsOred() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 9000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByLocationOrCapacityLessThan("London", 100);
				assertEquals(2, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));
				assertTrue(theatres.contains(new Cinema("Ritzy")));

				theatres = cinemaRepository.findByCapacityGreaterThanOrLocation(8000, "Paris");
				assertEquals(1, theatres.size());
				assertEquals("Regal", theatres.get(0).getName());
			}
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)");

		List<Cinema> theatres = cinemaRepository.findByVisitedName("Michal");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}

	/**
	 * @see DATAGRAPH-???
	 */
	@Test
	public void shouldFindNodeEntititiesWithDeepNestedProperty() {
		executeUpdate("CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500})"
				+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r) CREATE (m1:Movie {name:'Speed'})"
				+ " CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g)");

		List<Cinema> theatres = cinemaRepository.findByVisitedInterestedName("Thriller");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithBaseAndNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'Regal', city:'Bombay', capacity: 5000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationAndVisitedName("London", "Michal");
		assertEquals(1, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
	}

	/**
	 * @see DATAGRAPH-662 //TODO FIXME
	 */
	@Test(expected = UnsupportedOperationException.class)
	public void shouldFindNodeEntititiesWithBaseOrNestedProperty() {
		executeUpdate(
				"CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) CREATE (m:Theatre {name:'The Old Vic', city:'London', capacity: 5000})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(m)");

		List<Cinema> theatres = cinemaRepository.findByLocationOrVisitedName("P", "Michal");
		assertEquals(2, theatres.size());
		assertTrue(theatres.contains(new Cinema("Ritzy")));
		// assertTrue(theatres.contains(new Cinema("Picturehouse")));
		assertTrue(theatres.contains(new Cinema("The Old Vic")));
	}

	/**
	 * @see DATAGRAPH-632
	 */
	@Test
	public void shouldFindNodeEntitiesWithNestedRelationshipEntityProperty() {
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

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
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {name:'San Andreas'}) "
				+ " CREATE (m2:Movie {name:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "San Andreas");
				assertEquals(1, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));

				theatres = cinemaRepository.findByVisitedNameAndBlockbusterOfTheWeekName("Michal", "Tomorrowland");
				assertEquals(0, theatres.size());
			}
		});
	}

	/**
	 * @see DATAGRAPH-662
	 */
	// FIXME: OR is not supported for nested properties on an entity
	@Test(expected = UnsupportedOperationException.class)
	public void shouldFindNodeEntititiesWithTwoNestedPropertiesOred() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {title:'San Andreas'}) "
				+ " CREATE (m2:Movie {title:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Michal", "San Andreas");
				assertEquals(2, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));
				assertTrue(theatres.contains(new Cinema("Ritzy")));

				theatres = cinemaRepository.findByVisitedNameOrBlockbusterOfTheWeekName("Vince", "Tomorrowland");
				assertEquals(0, theatres.size());
			}
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithMultipleNestedPropertiesAnded() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) "
				+ " CREATE (u:User {name:'Michal', middleName:'M'}) CREATE (u1:User {name:'Vince', middleName:'M'}) "
				+ " CREATE (u)-[:VISITED]->(p)  CREATE (u1)-[:VISITED]->(r)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<Cinema> theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Michal", "M");
				assertEquals(1, theatres.size());
				assertTrue(theatres.contains(new Cinema("Picturehouse")));

				theatres = cinemaRepository.findByVisitedNameAndVisitedMiddleName("Vince", "V");
				assertEquals(0, theatres.size());
			}
		});
	}

	/**
	 * @see DATAGRAPH-629
	 */
	@Test
	public void shouldFindNodeEntititiesWithRelationshipEntityAndNestedProperty() {
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) CREATE (g:Genre {name:'Thriller'}) CREATE (u)-[:INTERESTED]->(g) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3, "Thriller");
		assertEquals(1, users.size());
		assertTrue(users.contains(new User("Michal")));
	}

	/**
	 * Relates to DATAGRAPH-601 and, to an extent, DATAGRAPH-761
	 */
	@Test
	public void shouldFindNodeEntitiesByRegularExpressionMatchingOnPropertiesInDerivedFinderMethods() {
		executeUpdate("CREATE (:Theatre {name:'Odeon', city:'Preston'}), " + "(:Theatre {name:'Vue', city:'Dumfries'}), "
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
				+ "(:Theatre {name:'Odeon', city:'Manchester'}), " + "(:Theatre {name:'IMAX', city:'Edinburgh'}) ");

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
		executeUpdate("CREATE (:User {name:'Jeff'}), " + "(:User {name:'Jeremy'}), " + "(:User {name:'Alan'})");

		List<User> nonMatchingUsers = userRepository.findByNameIsNotLike("Je*");
		assertEquals("The wrong number of users was returned", 1, nonMatchingUsers.size());
		assertEquals("The wrong user was returned", "Alan", nonMatchingUsers.get(0).getName());
	}

	/**
	 * @see DATAGRAPH-787
	 */
	@Test
	public void shouldFindDirectorsByName() {
		executeUpdate(
				"CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'}) CREATE (d:Director {name:'Vince'})");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
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
		});
	}

	/**
	 * @see DATAGRAPH-744
	 */
	@Test
	public void shouldFindUserWithCustomDepth() {
		executeUpdate("CREATE (p:Theatre {name:'Picturehouse', city:'London', capacity:5000}) "
				+ " CREATE (r:Theatre {name:'Ritzy', city:'London', capacity: 7500}) " + " CREATE (u:User {name:'Michal'}) "
				+ " CREATE (u)-[:VISITED]->(r)  CREATE (u)-[:VISITED]->(p)" + " CREATE (m1:Movie {name:'San Andreas'}) "
				+ " CREATE (m2:Movie {name:'Pitch Perfect 2'})" + " CREATE (p)-[:BLOCKBUSTER]->(m1)"
				+ " CREATE (r)-[:BLOCKBUSTER]->(m2)" + " CREATE (u)-[:RATED {stars :3}]->(m1)");

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
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

				cinema = cinemaRepository.findByName("Picturehouse", 2);
				assertNotNull(cinema);
				assertEquals("Picturehouse", cinema.getName());
				assertEquals(1, cinema.getVisited().size());
				assertEquals(1, cinema.getVisited().iterator().next().getRatings().size());
				assertEquals("San Andreas", cinema.getBlockbusterOfTheWeek().getName());
			}
		});
	}

	/**
	 * @see DATAGRAPH-744
	 */
	@Test
	public void shouldFindUsersByNameWithStaticDepth() {
		executeUpdate(
				"CREATE (m:User {name:'Michal', surname:'Bachman'})<-[:FRIEND_OF]-(a:User {name:'Adam', surname:'George'})");

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
		executeUpdate(
				"CREATE (m1:Movie {title:'Speed'}) CREATE (m2:Movie {title:'The Matrix'}) CREATE (m:Movie {title:'Chocolat'})"
						+ " CREATE (u:User {name:'Michal'}) CREATE (u1:User {name:'Vince'}) "
						+ " CREATE (u)-[:RATED {stars:3}]->(m1)  CREATE (u)-[:RATED {stars:4}]->(m2) CREATE (u1)-[:RATED {stars:3}]->(m)");

		List<EntityWrappingQueryResult> result = userRepository.findRatingsWithLiteralMap();
		assertNotNull(result);
		assertEquals(2, result.size());
		EntityWrappingQueryResult row = result.get(0);
		if (row.getUser().getName().equals("Vince")) {
			assertEquals(1, row.getLiteralMap().size());
			assertEquals("Chocolat", row.getLiteralMap().iterator().next().get("movietitle"));
			assertEquals(3L, row.getLiteralMap().iterator().next().get("stars"));
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
		executeUpdate(
				"CREATE (m:User {name:'Michal', surname:'Bachman'}), (a:User {name:'Adam', surname:'George'}), (l:User {name:'Luanne', surname:'Misquitta'})");

		String[] firstNames = new String[] { "Michal", "Adam", "Luanne" };
		String[] lastNames = new String[] { "Bachman", "George", "Misquitta" };

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
	@Transactional
	public void shouldPageDerivedFinderQueries() {
		for (int i = 0; i < 10; i++) {
			userRepository.save(new User("A", "B"));
		}
		userRepository.save(new User("A", "C"));

		Pageable pageable = new PageRequest(0, 4);
		Page<User> page = userRepository.findByNameAndSurname("A", "B", pageable);
		assertEquals(4, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements());
		assertEquals(0, page.getNumber());
		assertTrue(page.isFirst());
		assertFalse(page.isLast());
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertEquals(4, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements());
		assertEquals(1, page.getNumber());
		assertFalse(page.isFirst());
		assertFalse(page.isLast());
		assertTrue(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", page.nextPageable());
		assertEquals(2, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements());
		assertEquals(2, page.getNumber());
		assertFalse(page.isFirst());
		assertTrue(page.isLast());
		assertFalse(page.hasNext());

		page = userRepository.findByNameAndSurname("A", "B", new PageRequest(0, 10));
		assertEquals(10, page.getNumberOfElements());
		assertEquals(10, page.getTotalElements());
		assertEquals(0, page.getNumber());
		assertTrue(page.isFirst());
		assertTrue(page.isLast());
		assertFalse(page.hasNext());
	}

	/**
	 * @see DATAGRAPH-680
	 */
	@Test
	@Transactional
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

	@Test // DATAGRAPH-1093
	public void shouldFindNodeEntitiesByAttributeIgnoringCase() {
		executeUpdate("CREATE (:Director {name:'Patty Jenkins'})\n" + //
				"      ,(:Director {name:'Marry Harron'})\n" + //
				"      ,(m1:Movie {title:'Speed'})\n" + //
				"      ,(m2:Movie {title:'The Matrix'})\n" + //
				"      ,(m3:Movie {title:'Chocolat'})\n" + //
				"      ,(g:Genre {name:'Thriller'})\n" + //
				"      ,(u1:User {name:'Michal'})\n" + //
				"            ,(u1)-[:INTERESTED]->(g)\n" + "            ,(u1)-[:RATED {stars:3}]->(m1)\n" + //
				"            ,(u1)-[:RATED {stars:4}]->(m2)\n" + //
				"      ,(u2:User {name:'Vince'})\n" + //
				"            ,(u1)-[:RATED {stars:3}]->(m3)");

		Collection<Director> directors = directorRepository.findByName("paTTY jenKins");
		assertThat(directors).isEmpty();

		// Ignore case for attribute Director#name set to ALWAYS
		directors = directorRepository.findByNameIgnoreCase("paTTY jenKins");
		assertThat(directors).hasSize(1).extracting(Director::getName).containsExactly("Patty Jenkins");

		List<User> users = userRepository.findByRatingsStarsAndInterestedName(3, "THRILLER");
		assertThat(users).isEmpty();

		// Ignore case for attribute Director#name set to ALWAYS
		users = userRepository.findByRatingsStarsAndInterestedNameIgnoreCase(3, "THRILLER");
		assertThat(users).hasSize(1).extracting(User::getName).containsExactly("Michal");

		// Ignore case for both Rating#stars and Genre#name to WHEN_POSSIBLE
		users = userRepository.findByRatingsStarsAndInterestedNameAllIgnoreCase(3, "THRILLER");
		assertThat(users).hasSize(1).extracting(User::getName).containsExactly("Michal");

		// Ignore case for Rating#stars set to ALWAYS
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> userRepository.findByRatingsStarsIgnoreCase(3))
				.withMessageStartingWith("Unable to ignore case of int types, the property 'ratings' must reference a String");
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

	@Configuration
	@ComponentScan({ "org.springframework.data.neo4j.examples.movies.service" })
	@EnableNeo4jRepositories("org.springframework.data.neo4j.examples.movies.repo")
	@EnableTransactionManagement
	static class MoviesContext {

		@Bean
		public PlatformTransactionManager transactionManager() {
			return new Neo4jTransactionManager(sessionFactory());
		}

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.examples.movies.domain");
		}
	}

}
