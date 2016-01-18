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

package org.springframework.data.neo4j.examples.movies;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.*;
import org.springframework.data.neo4j.examples.movies.repo.*;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.data.neo4j.server.InProcessServer;
import org.springframework.data.neo4j.server.Neo4jServer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.assertSameGraph;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 * @uathor Vince Bickers
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class MoviesIntegrationTest extends MultiDriverTestClass {

    private final Logger logger = LoggerFactory.getLogger(MoviesIntegrationTest.class);


    @Autowired
    private Session session;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private CinemaRepository cinemaRepository;
    @Autowired
    private AbstractAnnotatedEntityRepository abstractAnnotatedEntityRepository;
    @Autowired
    private AbstractEntityRepository abstractEntityRepository;
    @Autowired
    private TempMovieRepository tempMovieRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private RatingRepository ratingRepository;

    @Before
    public void clear() {
        session.clear();
        session.purgeDatabase();
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
                "CREATE (m:ReleasedMovie:AbstractAnnotatedEntity {cinemaRelease:'1994-09-10T00:00:00.000Z'," +
                        "cannesRelease:768700800000,title:'Pulp Fiction'})");
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
        movie.setTags(new String[]{"cool", "classic"});
        movie.setImage(new byte[]{1, 2, 3});

        abstractEntityRepository.save(movie);

        // byte arrays have to be transferred with a JSON-supported format. Base64 is the default.
        assertSameGraph(getGraphDatabaseService(), "CREATE (m:Movie {name:'Pulp Fiction', tags:['cool','classic'], " +
                "image:'AQID'})");
    }

    @Test
    public void shouldSaveUsers() {
        Set<User> set = new HashSet<>();
        set.add(new User("Michal"));
        set.add(new User("Adam"));
        set.add(new User("Vince"));

        userRepository.save(set);

        assertSameGraph(getGraphDatabaseService(), "CREATE (:User:Person {name:'Michal'})," +
                "(:User:Person {name:'Vince'})," +
                "(:User:Person {name:'Adam'})");

        assertEquals(3, userRepository.count());
    }

    @Test
    public void shouldSaveUsers2() {
        List<User> list = new LinkedList<>();
        list.add(new User("Michal"));
        list.add(new User("Adam"));
        list.add(new User("Vince"));

        userRepository.save(list);

        assertSameGraph(getGraphDatabaseService(), "CREATE (:User:Person {name:'Michal'})," +
                "(:User:Person {name:'Vince'})," +
                "(:User:Person {name:'Adam'})");

        assertEquals(3, userRepository.count());
    }

    @Test
    public void shouldUpdateUserUsingRepository() {
        User user = userRepository.save(new User("Michal"));
        user.setName("Adam");
        userRepository.save(user);

        assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Adam'})");
    }

    @Test
    @Ignore  // FIXME
    // this test expects the session/tx to check for dirty objects, which it currently does not do
    // you must save objects explicitly.
    public void shouldUpdateUserUsingTransactionalService() {
        User user = new User("Michal");
        userRepository.save(user);

        userService.updateUser(user, "Adam"); //notice userRepository.save(..) isn't called,
        // not even in the service impl!

        assertSameGraph(getGraphDatabaseService(), "CREATE (u:User {name:'Adam'})");
    }

    @Test
    public void shouldFindUser() {
        User user = new User("Michal");
        userRepository.save(user);

        User loaded = userRepository.findOne(user.getId());

        assertEquals("Michal", loaded.getName());

        assertTrue(loaded.equals(user));
        assertTrue(loaded == user);
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

        User loaded = userRepository.findOne(user.getId());

        assertNull(loaded.getName());

        assertTrue(loaded.equals(user));
        assertTrue(loaded == user);
    }

    @Test
    public void shouldDeleteUser() {
        User user = new User("Michal");
        userRepository.save(user);
        userRepository.delete(user);

        assertFalse(userRepository.findAll().iterator().hasNext());
        assertFalse(userRepository.findAll(1).iterator().hasNext());
        assertFalse(userRepository.exists(user.getId()));
        assertEquals(0, userRepository.count());
        assertNull(userRepository.findOne(user.getId()));
        assertNull(userRepository.findOne(user.getId(), 10));

        try (Transaction tx = getGraphDatabaseService().beginTx()) {
            assertFalse(GlobalGraphOperations.at(getGraphDatabaseService()).getAllNodes().iterator().hasNext());
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

        System.out.println("all threads joined");
        executor.shutdown();

        assertEquals( 100, userRepository.count() );

    }

    @Test(expected = DataAccessException.class)
    public void shouldInterceptOGMExceptions() {
        ratingRepository.findAll(0);  // ratings are REs and must be found to at least depth 1 in order to get the start and end nodes
    }

    @Test
    public void shouldSaveUserAndNewGenre() {
        User user = new User("Michal");
        user.interestedIn(new Genre("Drama"));

        userRepository.save(user);

        assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})");
    }

    @Test
    public void shouldSaveUserAndNewGenres() {
        User user = new User("Michal");
        user.interestedIn(new Genre("Drama"));
        user.interestedIn(new Genre("Historical"));
        user.interestedIn(new Genre("Thriller"));

        userRepository.save(user);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(u:User:Person {name:'Michal'})," +
                "(g1:Genre {name:'Drama'})," +
                "(g2:Genre {name:'Historical'})," +
                "(g3:Genre {name:'Thriller'})," +
                "(u)-[:INTERESTED]->(g1)," +
                "(u)-[:INTERESTED]->(g2)," +
                "(u)-[:INTERESTED]->(g3)");
    }

    @Test
    public void shouldSaveUserAndNewGenre2() {
        User user = new User("Michal");
        user.interestedIn(new Genre("Drama"));

        userRepository.save(user, 1);

        assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})");
    }

    @Test
    public void shouldSaveUserAndExistingGenre() {
        User michal = new User("Michal");
        Genre drama = new Genre("Drama");
        michal.interestedIn(drama);

        userRepository.save(michal);

        User vince = new User("Vince");
        vince.interestedIn(drama);

        userRepository.save(vince);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(v:User:Person {name:'Vince'})," +
                "(g:Genre {name:'Drama'})," +
                "(m)-[:INTERESTED]->(g)," +
                "(v)-[:INTERESTED]->(g)");
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
        User michal = new User("Michal");
        Genre drama = new Genre("Drama");
        michal.interestedIn(drama);

        userRepository.save(michal);

        drama.setName("New Drama");

        userRepository.save(michal);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'New Drama'})," +
                "(m)-[:INTERESTED]->(g)");
    }

    @Test
    public void shouldRemoveGenreFromUser() {
        User michal = new User("Michal");
        Genre drama = new Genre("Drama");
        michal.interestedIn(drama);

        userRepository.save(michal);

        michal.notInterestedIn(drama);

        userRepository.save(michal);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'Drama'})");
    }

    @Test
    public void shouldRemoveGenreFromUserUsingService() {
        User michal = new User("Michal");
        Genre drama = new Genre("Drama");
        michal.interestedIn(drama);

        userRepository.save(michal);

        userService.notInterestedIn(michal.getId(), drama.getId());

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'Drama'})");
    }

    @Test
    public void shouldAddNewVisitorToCinema() {
        Cinema cinema = new Cinema("Odeon");
        cinema.addVisitor(new User("Michal"));

        cinemaRepository.save(cinema);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(c:Theatre {name:'Odeon', capacity:0})," +
                "(m)-[:VISITED]->(c)");
    }

    @Test
    public void shouldAddExistingVisitorToCinema() {
        User michal = new User("Michal");
        userRepository.save(michal);

        Cinema cinema = new Cinema("Odeon");
        cinema.addVisitor(michal);

        cinemaRepository.save(cinema);

        assertSameGraph(getGraphDatabaseService(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(c:Theatre {name:'Odeon', capacity:0})," +
                "(m)-[:VISITED]->(c)");
    }

    @Test
    public void shouldBefriendPeople() {
        User michal = new User("Michal");
        michal.befriend(new User("Adam"));
        userRepository.save(michal);

        try {
            assertSameGraph(getGraphDatabaseService(), "CREATE (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User:Person {name:'Adam'})");
        } catch (AssertionError error) {
            assertSameGraph(getGraphDatabaseService(), "CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'})");
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

        assertSameGraph(getGraphDatabaseService(), "CREATE (u:User:Person {name:'Michal'})-[:RATED {stars:5, " +
                "comment:'Best movie ever', ratingTimestamp:0}]->(m:Movie {name:'Pulp Fiction'})");
    }

    @Test
    public void shouldSaveNewUserRatingsForAnExistingMovie() {
        TempMovie movie = new TempMovie("Pulp Fiction");
        //Save the movie
        movie = tempMovieRepository.save(movie);

        //Create a new user and rate an existing movie
        User user = new User("Michal");
        user.rate(movie, 5, "Best movie ever");
        userRepository.save(user);

        TempMovie tempMovie = ((Iterable<TempMovie>) findByProperty(TempMovie.class, "name", "Pulp Fiction")).iterator().next();
        assertEquals(1, tempMovie.getRatings().size());
    }

    /**
     * @see DATAGRAPH-707
     */
    @Test
    public void findOneShouldConsiderTheEntityType() {
        TempMovie movie = new TempMovie("Pulp Fiction");
        //Save the movie
        movie = tempMovieRepository.save(movie);

        //Create a new user and rate an existing movie
        User user = new User("Michal");
        user.rate(movie, 5, "Best movie ever");
        userRepository.save(user);

        assertEquals(movie.getName(), tempMovieRepository.findOne(movie.getId()).getName());
        assertEquals(user.getName(), userRepository.findOne(user.getId()).getName());
        assertEquals(5, ratingRepository.findOne(user.getRatings().iterator().next().getId()).getStars());

        assertNull(tempMovieRepository.findOne(user.getId()));
        assertNull(userRepository.findOne(movie.getId(), 0));
        assertNull(ratingRepository.findOne(user.getId()));
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
        Iterable<User> savedUsers = userRepository.save(users);
        for (User user : savedUsers) {
            assertNotNull(user.getId());
        }
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

    private void waitForNeo4jToStart(long maxTimeToWait) throws Neo4jFailedToStartException {
        long startTime = System.currentTimeMillis();

        Transaction transaction;
        do {
            transaction = getGraphDatabaseService().beginTx();
        } while (transaction == null && System.currentTimeMillis() - startTime <= maxTimeToWait);

        if (transaction == null) {
            throw new Neo4jFailedToStartException(maxTimeToWait);
        }
        transaction.close();
    }

    protected Iterable<?> findByProperty(Class clazz, String propertyName, Object propertyValue) {
        return session.loadAll(clazz, new Filter(propertyName, propertyValue));
    }

    protected Iterable<?> findByProperty(Class clazz, String propertyName, Object propertyValue, int depth) {
        return session.loadAll(clazz, new Filter(propertyName, propertyValue), depth);
    }

    //
    private static class Neo4jFailedToStartException extends Exception {
        private Neo4jFailedToStartException(long timeoutValue) {
            super(String.format("Could not start neo4j instance in [%d] ms", timeoutValue));
        }
    }

}
