/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.examples.movies;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.examples.movies.domain.Actor;
import org.springframework.data.neo4j.examples.movies.domain.Cinema;
import org.springframework.data.neo4j.examples.movies.domain.Genre;
import org.springframework.data.neo4j.examples.movies.domain.Movie;
import org.springframework.data.neo4j.examples.movies.domain.Rating;
import org.springframework.data.neo4j.examples.movies.domain.ReleasedMovie;
import org.springframework.data.neo4j.examples.movies.domain.TempMovie;
import org.springframework.data.neo4j.examples.movies.domain.User;
import org.springframework.data.neo4j.examples.movies.repo.AbstractAnnotatedEntityRepository;
import org.springframework.data.neo4j.examples.movies.repo.AbstractEntityRepository;
import org.springframework.data.neo4j.examples.movies.repo.ActorRepository;
import org.springframework.data.neo4j.examples.movies.repo.CinemaRepository;
import org.springframework.data.neo4j.examples.movies.repo.RatingRepository;
import org.springframework.data.neo4j.examples.movies.repo.TempMovieRepository;
import org.springframework.data.neo4j.examples.movies.repo.UserRepository;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @author Vince Bickers
 * @author Mark Angrish
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Gerrit Meier
 */
@ContextConfiguration(classes = { MoviesIntegrationTests.MoviesContext.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class MoviesIntegrationTests extends MultiDriverTestClass {

	private static final String KNOWN_MAIL_ADDRESS_1 = "a@example.org";
	private static final String KNOWN_MAIL_ADDRESS_2 = "b@example.org";
	private static final String UNKNOWN_MAIL_ADDRESS = "c@example.org";
	private final Logger logger = LoggerFactory.getLogger(MoviesIntegrationTests.class);

	@Autowired PlatformTransactionManager platformTransactionManager;
	@Autowired private Session session;
	@Autowired private UserRepository userRepository;
	@Autowired private UserService userService;
	@Autowired private CinemaRepository cinemaRepository;
	@Autowired private AbstractAnnotatedEntityRepository abstractAnnotatedEntityRepository;
	@Autowired private AbstractEntityRepository abstractEntityRepository;
	@Autowired private TempMovieRepository tempMovieRepository;
	@Autowired private ActorRepository actorRepository;
	@Autowired private RatingRepository ratingRepository;

	private TransactionTemplate transactionTemplate;

	@Before
	public void setUp() {
		transactionTemplate = new TransactionTemplate(platformTransactionManager);
		getGraphDatabaseService().execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldSaveUser() {
		User user = new User("Michal");
		userRepository.save(user);

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})");
	}

	@Test
	public void shouldSaveUserWithoutName() {
		User user = new User();
		userRepository.save(user);

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person)");
	}

	@Test
	public void shouldSaveReleasedMovie() {

		Calendar cinemaReleaseDate = createDate(1994, Calendar.SEPTEMBER, 10, "GMT");
		Calendar cannesReleaseDate = createDate(1994, Calendar.MAY, 12, "GMT");

		ReleasedMovie releasedMovie = new ReleasedMovie("Pulp Fiction", cinemaReleaseDate.getTime(),
				cannesReleaseDate.getTime());

		abstractAnnotatedEntityRepository.save(releasedMovie);

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (m:ReleasedMovie:AbstractAnnotatedEntity {cinemaRelease:'1994-09-10T00:00:00.000Z',"
						+ "cannesRelease:768700800000,title:'Pulp Fiction'})");
	}

	@Test
	public void shouldSaveReleasedMovie2() {

		Calendar cannesReleaseDate = createDate(1994, Calendar.MAY, 12, "GMT");

		ReleasedMovie releasedMovie = new ReleasedMovie("Pulp Fiction", null, cannesReleaseDate.getTime());

		abstractAnnotatedEntityRepository.save(releasedMovie);

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (m:ReleasedMovie:AbstractAnnotatedEntity {cannesRelease:768700800000,title:'Pulp Fiction'})");
	}

	@Test
	public void shouldSaveMovie() {

		Movie movie = new Movie("Pulp Fiction");
		movie.setTags(new String[] { "cool", "classic" });
		movie.setImage(new byte[] { 1, 2, 3 });

		abstractEntityRepository.save(movie);

		// byte arrays have to be transferred with a JSON-supported format. Base64 is the default.
		assertSameGraph(getGraphDatabaseService(),
				"CREATE (m:Movie {name:'Pulp Fiction', tags:['cool','classic'], " + "image:'AQID'})");
	}

	@Test
	public void shouldSaveUsers() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				Set<User> set = new HashSet<>();
				set.add(new User("Michal"));
				set.add(new User("Adam"));
				set.add(new User("Vince"));

				userRepository.saveAll(set);
				assertEquals(3, userRepository.count());
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (:User:Person {name:'Michal'})," + "(:User:Person {name:'Vince'})," + "(:User:Person {name:'Adam'})");
	}

	@Test
	public void shouldSaveUsers2() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				List<User> list = new LinkedList<>();
				list.add(new User("Michal"));
				list.add(new User("Adam"));
				list.add(new User("Vince"));

				userRepository.saveAll(list);
				assertEquals(3, userRepository.count());
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (:User:Person {name:'Michal'})," + "(:User:Person {name:'Vince'})," + "(:User:Person {name:'Adam'})");
	}

	@Test
	public void shouldUpdateUserUsingRepository() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = userRepository.save(new User("Michal"));
				user.setName("Adam");
				userRepository.save(user);
			}
		});

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Adam'})");
	}

	@Test
	@Ignore // FIXME
	// this test expects the session/tx to check for dirty objects, which it currently does not do
	// you must save objects explicitly.
	public void shouldUpdateUserUsingTransactionalService() {
		User user = new User("Michal");
		userRepository.save(user);

		userService.updateUser(user, "Adam"); // notice userRepository.save(..) isn't called,
		// not even in the service impl!

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User {name:'Adam'})");
	}

	@Test
	public void shouldFindUser() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = new User("Michal");
				userRepository.save(user);

				Optional<User> loaded = userRepository.findById(user.getId());

				assertTrue(loaded.isPresent());

				loaded.ifPresent(loadedUser -> {
					assertEquals("Michal", loadedUser.getName());
					assertTrue(loadedUser.equals(user));
					assertTrue(loadedUser == user);
				});
			}
		});
	}

	@Test
	public void shouldFindActorByNumericValueOfStringProperty() {
		Actor actor = new Actor("1", "Tom Hanks");
		actorRepository.save(actor);

		assertNotNull(findByProperty(Actor.class, "id", "1").iterator().next());
	}

	@Test
	@Ignore
	public void shouldFindUserWithoutName() {
		User user = new User();
		userRepository.save(user);

		Optional<User> loaded = userRepository.findById(user.getId());

		assertTrue(loaded.isPresent());

		loaded.ifPresent(loadedUser -> {
			assertNull(loadedUser.getName());
			assertTrue(loadedUser.equals(user));
			assertTrue(loadedUser == user);
		});
	}

	@Test
	public void shouldDeleteUser() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = new User("Michal");
				userRepository.save(user);
				userRepository.delete(user);

				assertFalse(userRepository.findAll().iterator().hasNext());
				assertFalse(userRepository.findAll(1).iterator().hasNext());
				assertFalse(userRepository.existsById(user.getId()));
				assertEquals(0, userRepository.count());
				assertFalse(userRepository.findById(user.getId()).isPresent());
				assertFalse(userRepository.findById(user.getId(), 10).isPresent());
			}
		});

		try (Transaction tx = getGraphDatabaseService().beginTx()) {
			assertFalse(getGraphDatabaseService().getAllNodes().iterator().hasNext());
			tx.success();
		}
	}

	@Test
	public void shouldHandleMultipleConcurrentRequests() throws InterruptedException, Neo4jFailedToStartException {

		ExecutorService executor = Executors.newFixedThreadPool(10);
		final CountDownLatch latch = new CountDownLatch(100);

		for (int i = 0; i < 100; i++) {
			executor.submit(new Runnable() {
				@Override
				public void run() {
					userRepository.save(new User());
					latch.countDown();
				}
			});
		}

		latch.await(); // pause until the count reaches 0

		executor.shutdown();

		assertEquals(100, userRepository.count());
	}

	@Test(expected = DataAccessException.class)
	public void shouldInterceptOGMExceptions() {
		ratingRepository.findAll(0); // ratings are REs and must be found to at least depth 1 in order to get the start and
																	// end nodes
	}

	@Test
	public void shouldSaveUserAndNewGenre() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user);

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})");
	}

	@Test
	public void shouldSaveUserAndNewGenres() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));
		user.interestedIn(new Genre("Historical"));
		user.interestedIn(new Genre("Thriller"));

		userRepository.save(user);

		assertSameGraph(getGraphDatabaseService(),
				"CREATE " + "(u:User:Person {name:'Michal'})," + "(g1:Genre {name:'Drama'}),"
						+ "(g2:Genre {name:'Historical'})," + "(g3:Genre {name:'Thriller'})," + "(u)-[:INTERESTED]->(g1),"
						+ "(u)-[:INTERESTED]->(g2)," + "(u)-[:INTERESTED]->(g3)");
	}

	@Test
	public void shouldSaveUserAndNewGenre2() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user, 1);

		assertSameGraph(getGraphDatabaseService(),
				"CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})");
	}

	@Test
	public void shouldSaveUserAndExistingGenre() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				User vince = new User("Vince");
				vince.interestedIn(drama);

				userRepository.save(vince);
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE " + "(m:User:Person {name:'Michal'})," + "(v:User:Person {name:'Vince'})," + "(g:Genre {name:'Drama'}),"
						+ "(m)-[:INTERESTED]->(g)," + "(v)-[:INTERESTED]->(g)");
	}

	@Test
	public void shouldSaveUserButNotGenre() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user, 0);

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})");
	}

	@Test
	public void shouldUpdateGenreWhenSavedThroughUser() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				drama.setName("New Drama");

				userRepository.save(michal);
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE " + "(m:User:Person {name:'Michal'})," + "(g:Genre {name:'New Drama'})," + "(m)-[:INTERESTED]->(g)");
	}

	@Test
	public void shouldRemoveGenreFromUser() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				michal.notInterestedIn(drama);

				userRepository.save(michal);
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE " + "(m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'})");
	}

	@Test
	public void shouldRemoveGenreFromUserUsingService() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				userService.notInterestedIn(michal.getId(), drama.getId());
			}
		});

		assertSameGraph(getGraphDatabaseService(),
				"CREATE " + "(m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'})");
	}

	@Test
	public void shouldAddNewVisitorToCinema() {
		Cinema cinema = new Cinema("Odeon");
		cinema.addVisitor(new User("Michal"));

		cinemaRepository.save(cinema);

		assertSameGraph(getGraphDatabaseService(), "CREATE " + "(m:User:Person {name:'Michal'}),"
				+ "(c:Theatre {name:'Odeon', capacity:0})," + "(m)-[:VISITED]->(c)");
	}

	@Test
	public void shouldAddExistingVisitorToCinema() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				userRepository.save(michal);

				Cinema cinema = new Cinema("Odeon");
				cinema.addVisitor(michal);

				cinemaRepository.save(cinema);
			}
		});

		assertSameGraph(getGraphDatabaseService(), "CREATE " + "(m:User:Person {name:'Michal'}),"
				+ "(c:Theatre {name:'Odeon', capacity:0})," + "(m)-[:VISITED]->(c)");
	}

	@Test
	public void shouldBefriendPeople() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				michal.befriend(new User("Adam"));
				userRepository.save(michal);
			}
		});

		try {
			assertSameGraph(getGraphDatabaseService(),
					"CREATE (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User:Person {name:'Adam'})");
		} catch (AssertionError error) {
			assertSameGraph(getGraphDatabaseService(),
					"CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'})");
		}
	}

	@Test
	public void shouldLoadOutgoingFriendsWhenUndirected() {

		getGraphDatabaseService().execute("CREATE (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User {name:'Adam'})");

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();
		assertEquals(1, michal.getFriends().size());

		User adam = michal.getFriends().iterator().next();
		assertEquals("Adam", adam.getName());
		assertEquals(1, adam.getFriends().size());

		assertTrue(michal == adam.getFriends().iterator().next());
		assertTrue(michal.equals(adam.getFriends().iterator().next()));
	}

	@Test
	public void shouldLoadIncomingFriendsWhenUndirected() {

		getGraphDatabaseService().execute("CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();
		assertEquals(1, michal.getFriends().size());

		User adam = michal.getFriends().iterator().next();
		assertEquals("Adam", adam.getName());
		assertEquals(1, adam.getFriends().size());

		assertTrue(michal == adam.getFriends().iterator().next());
		assertTrue(michal.equals(adam.getFriends().iterator().next()));
	}

	@Test
	public void shouldSaveNewUserAndNewMovieWithRatings() {
		User user = new User("Michal");
		TempMovie movie = new TempMovie("Pulp Fiction");
		user.rate(movie, 5, "Best movie ever");
		userRepository.save(user);

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();

		assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})-[:RATED {stars:5, "
				+ "comment:'Best movie ever', ratingTimestamp:0}]->(m:Movie {name:'Pulp Fiction'})");
	}

	@Test
	public void shouldSaveNewUserRatingsForAnExistingMovie() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				TempMovie movie = new TempMovie("Pulp Fiction");
				// Save the movie
				movie = tempMovieRepository.save(movie);

				// Create a new user and rate an existing movie
				User user = new User("Michal");
				user.rate(movie, 5, "Best movie ever");
				userRepository.save(user);

				TempMovie tempMovie = ((Iterable<TempMovie>) findByProperty(TempMovie.class, "name", "Pulp Fiction")).iterator()
						.next();
				assertEquals(1, tempMovie.getRatings().size());
			}
		});
	}

	/**
	 * @see DATAGRAPH-707
	 */
	@Test
	public void findByIdShouldConsiderTheEntityType() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				// Save the movie
				TempMovie movie = tempMovieRepository.save(new TempMovie("Pulp Fiction"));

				// Create a new user and rate an existing movie
				User user = new User("Michal");
				user.rate(movie, 5, "Best movie ever");
				userRepository.save(user);

				Optional<TempMovie> tempMovieOptional = tempMovieRepository.findById(movie.getId());

				assertTrue(tempMovieOptional.isPresent());
				tempMovieOptional.ifPresent(actual -> {
					assertEquals(movie.getName(), actual.getName());
				});

				Optional<User> userOptional = userRepository.findById(user.getId());

				assertTrue(userOptional.isPresent());
				userOptional.ifPresent(actual -> {
					assertEquals(user.getName(), actual.getName());
				});

				Optional<Rating> ratingOptional = ratingRepository.findById(user.getRatings().iterator().next().getId());

				assertTrue(ratingOptional.isPresent());
				ratingOptional.ifPresent(actual -> {
					assertEquals(5, actual.getStars());
				});

				assertFalse(tempMovieRepository.findById(user.getId()).isPresent());
				assertFalse(userRepository.findById(movie.getId(), 0).isPresent());
				assertFalse(ratingRepository.findById(user.getId()).isPresent());
			}
		});
	}

	@Test
	/**
	 * @see DATAGRAPH-760
	 */
	public void shouldSaveAndReturnManyEntities() {
		User michal = new User("Michal");
		User adam = new User("Adam");
		User daniela = new User("Daniela");

		List<User> users = Arrays.asList(michal, adam, daniela);
		Iterable<User> savedUsers = userRepository.saveAll(users);
		for (User user : savedUsers) {
			assertNotNull(user.getId());
		}
	}

	/**
	 * @see DATAGRAPH-992
	 */
	@Test
	public void findUserByContainingEmailAddresses() {
		createUserForContainsTest();

		User foundUser = userRepository.findByEmailAddressesContains(Collections.singletonList(KNOWN_MAIL_ADDRESS_1));
		assertNotNull(foundUser);

		foundUser = userRepository.findByEmailAddressesContains(Arrays.asList(KNOWN_MAIL_ADDRESS_2, UNKNOWN_MAIL_ADDRESS));
		assertNotNull(foundUser);
	}

	/**
	 * @see DATAGRAPH-992
	 */
	@Test
	public void findNoUserByContainingEmailAddresses() {
		createUserForContainsTest();

		User foundUser = userRepository.findByEmailAddressesContains(Collections.singletonList(UNKNOWN_MAIL_ADDRESS));
		assertNull(foundUser);
	}

	/**
	 * @see DATAGRAPH-992
	 */
	@Test
	public void findUserByNotContainingEmailAddresses() {
		createUserForContainsTest();

		List<User> foundUser = userRepository.findByEmailAddressesNotContaining(UNKNOWN_MAIL_ADDRESS);
		assertNotNull(foundUser.get(0));
	}

	/**
	 * @see DATAGRAPH-992
	 */
	@Test
	public void findNoUserByNotContainingEmailAddresses() {
		createUserForContainsTest();

		List<User> foundUser = userRepository.findByEmailAddressesNotContaining(KNOWN_MAIL_ADDRESS_1);
		assertTrue(foundUser.isEmpty());
	}

	private void createUserForContainsTest() {
		User user = new User("Somebody");
		Set<String> emailAddresses = new HashSet<>();
		emailAddresses.add(KNOWN_MAIL_ADDRESS_1);
		emailAddresses.add(KNOWN_MAIL_ADDRESS_2);
		user.setEmailAddresses(emailAddresses);
		userRepository.save(user);
	}

	private Calendar createDate(int y, int m, int d, String tz) {

		Calendar calendar = Calendar.getInstance();

		calendar.set(y, m, d);
		calendar.setTimeZone(TimeZone.getTimeZone(tz));

		// need to do this to ensure the test passes, or the calendar will use the current time's values
		// an alternative (better) would be to specify an date format using one of the @Date converters
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);

		return calendar;
	}

	protected Iterable<?> findByProperty(Class clazz, String propertyName, Object propertyValue) {
		return session.loadAll(clazz, new Filter(propertyName, ComparisonOperator.EQUALS, propertyValue));
	}

	//
	private static class Neo4jFailedToStartException extends Exception {

		private Neo4jFailedToStartException(long timeoutValue) {
			super(String.format("Could not start neo4j instance in [%d] ms", timeoutValue));
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
