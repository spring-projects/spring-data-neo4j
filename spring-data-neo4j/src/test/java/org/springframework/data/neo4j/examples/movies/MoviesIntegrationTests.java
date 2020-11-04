/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.examples.movies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.data.neo4j.test.GraphDatabaseServiceAssert.assertThat;

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
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.data.neo4j.queries.MoviesContextConfiguration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.TransactionStatus;
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
 * @author Michael J. Simons
 */
@ContextConfiguration(classes = MoviesContextConfiguration.class)
@RunWith(SpringRunner.class)
public class MoviesIntegrationTests {

	private static final String KNOWN_MAIL_ADDRESS_1 = "a@example.org";
	private static final String KNOWN_MAIL_ADDRESS_2 = "b@example.org";
	private static final String UNKNOWN_MAIL_ADDRESS = "c@example.org";

	@Autowired private GraphDatabaseService graphDatabaseService;
	@Autowired private Session session;
	@Autowired private UserRepository userRepository;
	@Autowired private UserService userService;
	@Autowired private CinemaRepository cinemaRepository;
	@Autowired private AbstractAnnotatedEntityRepository abstractAnnotatedEntityRepository;
	@Autowired private AbstractEntityRepository abstractEntityRepository;
	@Autowired private TempMovieRepository tempMovieRepository;
	@Autowired private ActorRepository actorRepository;
	@Autowired private RatingRepository ratingRepository;
	@Autowired private TransactionTemplate transactionTemplate;

	@Before
	public void setUp() {
		graphDatabaseService.execute("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n");
	}

	@Test
	public void shouldSaveUser() {
		User user = new User("Michal");
		userRepository.save(user);

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Michal'}) RETURN n")
				.withId(user.getId());
	}

	@Test
	public void shouldSaveUserWithoutName() {
		User user = new User();
		userRepository.save(user);

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person) RETURN n").withId(user.getId());
	}

	@Test
	public void shouldSaveReleasedMovie() {

		Calendar cinemaReleaseDate = createDate(1994, Calendar.SEPTEMBER, 10, "GMT");
		Calendar cannesReleaseDate = createDate(1994, Calendar.MAY, 12, "GMT");

		ReleasedMovie releasedMovie = new ReleasedMovie("Pulp Fiction", cinemaReleaseDate.getTime(),
				cannesReleaseDate.getTime());

		abstractAnnotatedEntityRepository.save(releasedMovie);

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:ReleasedMovie:AbstractAnnotatedEntity {cinemaRelease:'1994-09-10T00:00:00.000Z',"
						+ "cannesRelease:768700800000,title:'Pulp Fiction'}) RETURN m AS n");
	}

	@Test
	public void shouldSaveReleasedMovie2() {

		Calendar cannesReleaseDate = createDate(1994, Calendar.MAY, 12, "GMT");

		ReleasedMovie releasedMovie = new ReleasedMovie("Pulp Fiction", null, cannesReleaseDate.getTime());

		abstractAnnotatedEntityRepository.save(releasedMovie);

		assertThat(graphDatabaseService).containsNode(
				"MATCH (m:ReleasedMovie:AbstractAnnotatedEntity {cannesRelease:768700800000,title:'Pulp Fiction'}) RETURN m AS n");
	}

	@Test
	public void shouldSaveMovie() {

		Movie movie = new Movie("Pulp Fiction");
		movie.setTags(new String[] { "cool", "classic" });
		movie.setImage(new byte[] { 1, 2, 3 });

		abstractEntityRepository.save(movie);

		// byte arrays have to be transferred with a JSON-supported format. Base64 is the default.
		assertThat(graphDatabaseService).containsNode(
				"MATCH (m:Movie {name:'Pulp Fiction', tags:['cool','classic'], " + "image:'AQID'}) RETURN m AS n");
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
				assertThat(userRepository.count()).isEqualTo(3);
			}
		});

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Michal'}) RETURN n");
		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Vince'}) RETURN n");
		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Adam'}) RETURN n");

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
				assertThat(userRepository.count()).isEqualTo(3);
			}
		});

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Michal'}) RETURN n");
		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Vince'}) RETURN n");
		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Adam'}) RETURN n");
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

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Adam'}) RETURN n");
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

		assertThat(graphDatabaseService).containsNode("MATCH (n:User:Person {name:'Adam'}) RETURN n");
	}

	@Test
	public void shouldFindUser() {

		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User user = new User("Michal");
				userRepository.save(user);

				Optional<User> loaded = userRepository.findById(user.getId());

				assertThat(loaded.isPresent()).isTrue();

				loaded.ifPresent(loadedUser -> {
					assertThat(loadedUser.getName()).isEqualTo("Michal");
					assertThat(loadedUser.equals(user)).isTrue();
					assertThat(loadedUser == user).isTrue();
				});
			}
		});
	}

	@Test
	public void shouldFindActorByNumericValueOfStringProperty() {
		Actor actor = new Actor("1", "Tom Hanks");
		actorRepository.save(actor);

		assertThat(findByProperty(Actor.class, "id", "1").iterator().next()).isNotNull();
	}

	@Test
	@Ignore
	public void shouldFindUserWithoutName() {
		User user = new User();
		userRepository.save(user);

		Optional<User> loaded = userRepository.findById(user.getId());

		assertThat(loaded.isPresent()).isTrue();

		loaded.ifPresent(loadedUser -> {
			assertThat(loadedUser.getName()).isNull();
			assertThat(loadedUser.equals(user)).isTrue();
			assertThat(loadedUser == user).isTrue();
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

				assertThat(userRepository.findAll().iterator().hasNext()).isFalse();
				assertThat(userRepository.findAll(1).iterator().hasNext()).isFalse();
				assertThat(userRepository.existsById(user.getId())).isFalse();
				assertThat(userRepository.count()).isEqualTo(0);
				assertThat(userRepository.findById(user.getId()).isPresent()).isFalse();
				assertThat(userRepository.findById(user.getId(), 5).isPresent())
						.isFalse();
			}
		});

		try (Result result = graphDatabaseService.execute("MATCH (n) RETURN n")) {
			assertThat(result.hasNext()).isFalse();
		}
	}

	@Test
	public void shouldHandleMultipleConcurrentRequests() throws InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(10);
		final CountDownLatch latch = new CountDownLatch(100);

		for (int i = 0; i < 100; i++) {
			executor.submit(() -> {
				userRepository.save(new User());
				latch.countDown();
			});
		}

		latch.await(); // pause until the count reaches 0

		executor.shutdown();

		assertThat(userRepository.count()).isEqualTo(100);
	}

	@Test(expected = DataAccessException.class)
	public void shouldInterceptOGMExceptions() {
		// ratings are REs and must be found to at least depth 1 in order to get the start and end nodes
		ratingRepository.findAll(0);
	}

	@Test
	public void shouldSaveUserAndNewGenre() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user);

		assertThat(graphDatabaseService)
				.containsNode("MATCH (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'}) RETURN u AS n")
				.withId(user.getId());
	}

	@Test
	public void shouldSaveUserAndNewGenres() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));
		user.interestedIn(new Genre("Historical"));
		user.interestedIn(new Genre("Thriller"));

		userRepository.save(user);

		assertThat(graphDatabaseService).containsNode("MATCH (u:User:Person {name:'Michal'}),"
				+ "(g1:Genre {name:'Drama'})," + "(g2:Genre {name:'Historical'})," + "(g3:Genre {name:'Thriller'}),"
				+ "(u)-[:INTERESTED]->(g1)," + "(u)-[:INTERESTED]->(g2)," + "(u)-[:INTERESTED]->(g3) RETURN u AS n ")
				.withId(user.getId());
	}

	@Test
	public void shouldSaveUserAndNewGenre2() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user, 1);

		assertThat(graphDatabaseService)
				.containsNode("MATCH (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'}) RETURN u AS n")
				.withId(user.getId());
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

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:User:Person {name:'Michal'})," + "(v:User:Person {name:'Vince'}),"
						+ "(g:Genre {name:'Drama'})," + "(m)-[:INTERESTED]->(g)," + "(v)-[:INTERESTED]->(g) RETURN m AS n");
	}

	@Test
	public void shouldSaveUserButNotGenre() {
		User user = new User("Michal");
		user.interestedIn(new Genre("Drama"));

		userRepository.save(user, 0);

		assertThat(graphDatabaseService).containsNode("MATCH (u:User:Person {name:'Michal'}) RETURN u AS n")
				.withId(user.getId());
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

		assertThat(graphDatabaseService).containsNode("MATCH (m:User:Person {name:'Michal'}),"
				+ "(g:Genre {name:'New Drama'})," + "(m)-[:INTERESTED]->(g) RETURN m AS n");
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

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'}) RETURN m AS n");
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

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'}) RETURN m AS n");
	}

	@Test
	public void shouldAddNewVisitorToCinema() {
		Cinema cinema = new Cinema("Odeon");
		cinema.addVisitor(new User("Michal"));

		cinemaRepository.save(cinema);

		assertThat(graphDatabaseService).containsNode("MATCH (m:User:Person {name:'Michal'}),"
				+ "(c:Theatre {name:'Odeon', capacity:0})," + "(m)-[:VISITED]->(c) RETURN m AS n");
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

		assertThat(graphDatabaseService).containsNode("MATCH (m:User:Person {name:'Michal'}),"
				+ "(c:Theatre {name:'Odeon', capacity:0})," + "(m)-[:VISITED]->(c) RETURN m AS n");
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
			assertThat(graphDatabaseService)
					.containsNode("MATCH (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User:Person {name:'Adam'}) RETURN m AS n");
		} catch (AssertionError error) {
			assertThat(graphDatabaseService).containsNode(
					"MATCH (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'}) RETURN m AS n");
		}
	}

	@Test
	public void shouldLoadOutgoingFriendsWhenUndirected() {

		graphDatabaseService.execute("CREATE (m:User:Person {name:'Michal'})-[:FRIEND_OF]->(a:User {name:'Adam'})");

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();
		assertThat(michal.getFriends().size()).isEqualTo(1);

		User adam = michal.getFriends().iterator().next();
		assertThat(adam.getName()).isEqualTo("Adam");
		assertThat(adam.getFriends().size()).isEqualTo(1);

		assertThat(michal == adam.getFriends().iterator().next()).isTrue();
		assertThat(michal.equals(adam.getFriends().iterator().next())).isTrue();
	}

	@Test
	public void shouldLoadIncomingFriendsWhenUndirected() {

		graphDatabaseService.execute("CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User {name:'Adam'})");

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();
		assertThat(michal.getFriends().size()).isEqualTo(1);

		User adam = michal.getFriends().iterator().next();
		assertThat(adam.getName()).isEqualTo("Adam");
		assertThat(adam.getFriends().size()).isEqualTo(1);

		assertThat(michal == adam.getFriends().iterator().next()).isTrue();
		assertThat(michal.equals(adam.getFriends().iterator().next())).isTrue();
	}

	@Test
	public void shouldSaveNewUserAndNewMovieWithRatings() {
		User user = new User("Michal");
		TempMovie movie = new TempMovie("Pulp Fiction");
		user.rate(movie, 5, "Best movie ever");
		userRepository.save(user);

		User michal = ((Iterable<User>) findByProperty(User.class, "name", "Michal")).iterator().next();

		assertThat(graphDatabaseService)
				.containsNode("MATCH (u:User:Person {name:'Michal'})-[:RATED {stars:5, "
						+ "comment:'Best movie ever', ratingTimestamp:0}]->(m:Movie {name:'Pulp Fiction'}) RETURN u AS n")
				.withId(michal.getId());
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
				assertThat(tempMovie.getRatings().size()).isEqualTo(1);
			}
		});
	}

	@Test // DATAGRAPH-707
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

				assertThat(tempMovieOptional.isPresent()).isTrue();
				tempMovieOptional.ifPresent(actual -> {
					assertThat(actual.getName()).isEqualTo(movie.getName());
				});

				Optional<User> userOptional = userRepository.findById(user.getId());

				assertThat(userOptional.isPresent()).isTrue();
				userOptional.ifPresent(actual -> {
					assertThat(actual.getName()).isEqualTo(user.getName());
				});

				Optional<Rating> ratingOptional = ratingRepository.findById(user.getRatings().iterator().next().getId());

				assertThat(ratingOptional.isPresent()).isTrue();
				ratingOptional.ifPresent(actual -> {
					assertThat(actual.getStars()).isEqualTo(5);
				});

				assertThat(tempMovieRepository.findById(user.getId()).isPresent())
						.isFalse();
				assertThat(userRepository.findById(movie.getId(), 0).isPresent())
						.isFalse();
				assertThat(ratingRepository.findById(user.getId()).isPresent()).isFalse();
			}
		});
	}

	@Test // DATAGRAPH-760
	public void shouldSaveAndReturnManyEntities() {
		User michal = new User("Michal");
		User adam = new User("Adam");
		User daniela = new User("Daniela");

		List<User> users = Arrays.asList(michal, adam, daniela);
		Iterable<User> savedUsers = userRepository.saveAll(users);
		for (User user : savedUsers) {
			assertThat(user.getId()).isNotNull();
		}
	}

	@Test // DATAGRAPH-992
	public void findUserByContainingEmailAddresses() {
		createUserForContainsTest();

		User foundUser = userRepository.findByEmailAddressesContains(Collections.singletonList(KNOWN_MAIL_ADDRESS_1));
		assertThat(foundUser).isNotNull();

		foundUser = userRepository.findByEmailAddressesContains(Arrays.asList(KNOWN_MAIL_ADDRESS_2, UNKNOWN_MAIL_ADDRESS));
		assertThat(foundUser).isNotNull();
	}

	@Test // DATAGRAPH-992
	public void findNoUserByContainingEmailAddresses() {
		createUserForContainsTest();

		User foundUser = userRepository.findByEmailAddressesContains(Collections.singletonList(UNKNOWN_MAIL_ADDRESS));
		assertThat(foundUser).isNull();
	}

	@Test // DATAGRAPH-992
	public void findUserByNotContainingEmailAddresses() {
		createUserForContainsTest();

		List<User> foundUser = userRepository.findByEmailAddressesNotContaining(UNKNOWN_MAIL_ADDRESS);
		assertThat(foundUser.get(0)).isNotNull();
	}

	@Test // DATAGRAPH-992
	public void findNoUserByNotContainingEmailAddresses() {
		createUserForContainsTest();

		List<User> foundUser = userRepository.findByEmailAddressesNotContaining(KNOWN_MAIL_ADDRESS_1);
		assertThat(foundUser.isEmpty()).isTrue();
	}

	@Test // DATAGRAPH-1407
	public void shouldRemoveGenreFromUserDespiteReadonlyCustomQuery() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				michal.notInterestedIn(drama);

				userRepository.getAllUsers();
				userRepository.save(michal);
			}
		});

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'})" +
						" WHERE NOT (m)-[:INTERESTED]->(g) RETURN m AS n");
	}

	@Test // DATAGRAPH-1407
	public void shouldRemoveGenreFromUserDespiteReadonlyCustomQueryWithQueryResult() {
		transactionTemplate.execute(new TransactionCallbackWithoutResult() {
			@Override
			public void doInTransactionWithoutResult(TransactionStatus status) {
				User michal = new User("Michal");
				Genre drama = new Genre("Drama");
				michal.interestedIn(drama);

				userRepository.save(michal);

				michal.notInterestedIn(drama);

				userRepository.retrieveAllUsersAndTheirAges();
				userRepository.save(michal);
			}
		});

		assertThat(graphDatabaseService)
				.containsNode("MATCH (m:User:Person {name:'Michal'})," + "(g:Genre {name:'Drama'})" +
						" WHERE NOT (m)-[:INTERESTED]->(g) RETURN m AS n");
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

	private static class Neo4jFailedToStartException extends Exception {

		private Neo4jFailedToStartException(long timeoutValue) {
			super(String.format("Could not start neo4j instance in [%d] ms", timeoutValue));
		}
	}
}
