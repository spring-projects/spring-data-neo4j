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

import static org.junit.Assert.*;
import static org.neo4j.ogm.testutil.GraphTestUtils.*;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cypher.javacompat.ExecutionEngine;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.Neo4jIntegrationTestRule;
import org.neo4j.tooling.GlobalGraphOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.neo4j.examples.movies.context.MoviesContext;
import org.springframework.data.neo4j.examples.movies.domain.*;
import org.springframework.data.neo4j.examples.movies.repo.*;
import org.springframework.data.neo4j.examples.movies.service.UserService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Michal Bachman
 * @author Luanne Misquitta
 */
@ContextConfiguration(classes = {MoviesContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MoviesIntegrationTest {

    private final Logger logger = LoggerFactory.getLogger( MoviesIntegrationTest.class );

    @Rule
    public final Neo4jIntegrationTestRule neo4jRule = new Neo4jIntegrationTestRule(7879);

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
    private Session session;

    private GraphDatabaseService getDatabase() {
        return neo4jRule.getGraphDatabaseService();
    }

    @Test
    public void shouldSaveUser()
    {
        User user = new User( "Michal" );
        userRepository.save( user );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person {name:'Michal'})" );
        assertEquals( 0L, (long) user.getId() );
    }

    @Test
    public void shouldSaveUserWithoutName()
    {
        User user = new User();
        userRepository.save( user );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person)" );
        assertEquals( 0L, (long) user.getId() );
    }

    @Test
    public void shouldSaveReleasedMovie()
    {

        Calendar cinemaReleaseDate = createDate( 1994, Calendar.SEPTEMBER, 10, "GMT" );
        Calendar cannesReleaseDate = createDate( 1994, Calendar.MAY, 12, "GMT" );

        ReleasedMovie releasedMovie = new ReleasedMovie( "Pulp Fiction", cinemaReleaseDate.getTime(),
                cannesReleaseDate.getTime() );

        abstractAnnotatedEntityRepository.save( releasedMovie );

        assertSameGraph( getDatabase(),
                "CREATE (m:ReleasedMovie:AbstractAnnotatedEntity {cinemaRelease:'1994-09-10T00:00:00.000Z'," +
                        "cannesRelease:768700800000,title:'Pulp Fiction'})" );
    }

    @Test
    public void shouldSaveReleasedMovie2()
    {

        Calendar cannesReleaseDate = createDate( 1994, Calendar.MAY, 12, "GMT" );

        ReleasedMovie releasedMovie = new ReleasedMovie( "Pulp Fiction", null, cannesReleaseDate.getTime() );

        abstractAnnotatedEntityRepository.save( releasedMovie );

        assertSameGraph( getDatabase(),
                "CREATE (m:ReleasedMovie:AbstractAnnotatedEntity {cannesRelease:768700800000,title:'Pulp Fiction'})" );

    }

    @Test
    public void shouldSaveMovie()
    {
        Movie movie = new Movie( "Pulp Fiction" );
        movie.setTags( new String[]{"cool", "classic"} );
        movie.setImage( new byte[]{1, 2, 3} );

        abstractEntityRepository.save( movie );

        // byte arrays have to be transferred with a JSON-supported format. Base64 is the default.
        assertSameGraph( getDatabase(), "CREATE (m:Movie {title:'Pulp Fiction', tags:['cool','classic'], " +
                "image:'AQID'})" );
    }

    @Test
    public void shouldSaveUsers()
    {
        Set<User> set = new HashSet<>();
        set.add( new User( "Michal" ) );
        set.add( new User( "Adam" ) );
        set.add( new User( "Vince" ) );

        userRepository.save( set );

        assertSameGraph( getDatabase(), "CREATE (:User:Person {name:'Michal'})," +
                "(:User:Person {name:'Vince'})," +
                "(:User:Person {name:'Adam'})" );

        assertEquals( 3, userRepository.count() );
    }

    @Test
    public void shouldSaveUsers2()
    {
        List<User> list = new LinkedList<>();
        list.add( new User( "Michal" ) );
        list.add( new User( "Adam" ) );
        list.add( new User( "Vince" ) );

        userRepository.save( list );

        assertSameGraph( getDatabase(), "CREATE (:User:Person {name:'Michal'})," +
                "(:User:Person {name:'Vince'})," +
                "(:User:Person {name:'Adam'})" );

        assertEquals( 3, userRepository.count() );
    }

    @Test
    public void shouldUpdateUserUsingRepository()
    {
        User user = userRepository.save( new User( "Michal" ) );
        user.setName( "Adam" );
        userRepository.save( user );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person {name:'Adam'})" );
        assertEquals( 0L, (long) user.getId() );
    }

    @Test
    @Ignore  // FIXME
    // this test expects the session/tx to check for dirty objects, which it currently does not do
    // you must save objects explicitly.
    public void shouldUpdateUserUsingTransactionalService()
    {
        User user = new User( "Michal" );
        userRepository.save( user );

        userService.updateUser( user, "Adam" ); //notice userRepository.save(..) isn't called,
        // not even in the service impl!

        assertSameGraph( getDatabase(), "CREATE (u:User {name:'Adam'})" );
        assertEquals( 0L, (long) user.getId() );
    }

    @Test
    public void shouldFindUser()
    {
        User user = new User( "Michal" );
        userRepository.save( user );

        User loaded = userRepository.findOne( 0L );

        assertEquals( 0L, (long) loaded.getId() );
        assertEquals( "Michal", loaded.getName() );

        assertTrue( loaded.equals( user ) );
        assertTrue( loaded == user );
    }

    @Test
    public void shouldFindActorByNumericValueOfStringProperty() {
        Actor actor = new Actor("1", "Tom Hanks");
        actorRepository.save(actor);

        assertNotNull(findByProperty(Actor.class, "id" , "1" ).iterator().next());
    }

    @Test
    public void shouldFindUserWithoutName()
    {
        User user = new User();
        userRepository.save( user );

        User loaded = userRepository.findOne( 0L );

        assertEquals( 0L, (long) loaded.getId() );
        assertNull( loaded.getName() );

        assertTrue( loaded.equals( user ) );
        assertTrue( loaded == user );
    }

    @Test
    public void shouldDeleteUser()
    {
        User user = new User( "Michal" );
        userRepository.save( user );
        userRepository.delete( user );

        assertFalse( userRepository.findAll().iterator().hasNext() );
        assertFalse( userRepository.findAll( 1 ).iterator().hasNext() );
        assertFalse( userRepository.exists( 0L ) );
        assertEquals( 0, userRepository.count() );
        assertNull( userRepository.findOne( 0L ) );
        assertNull( userRepository.findOne( 0L, 10 ) );

        try ( Transaction tx = getDatabase().beginTx() )
        {
            assertFalse( GlobalGraphOperations.at( getDatabase() ).getAllNodes().iterator().hasNext() );
            tx.success();
        }
    }

    @Test
    public void shouldCreateUsersInMultipleThreads() throws InterruptedException, Neo4jFailedToStartException
    {
        waitForNeo4jToStart( 5000l );

        ExecutorService executor = Executors.newFixedThreadPool( 10 );
        CountDownLatch latch = new CountDownLatch( 100 );

        for ( int i = 0; i < 100; i++ )
        {
            executor.submit( new UserSaver( latch, i ) );
        }

        latch.await(); // pause until the count reaches 0
        executor.shutdown();

        assertEquals( 100, userRepository.count() );
    }

    @Test(expected= DataAccessException.class)
    @Ignore("this isn't working as the docs say it should. We must be doing something wrong")
    public void shouldInterceptOGMExceptions() {
        User user = null;
        userRepository.save(user);
    }

    private class UserSaver implements Runnable
    {

        private final int userNumber;
        private final CountDownLatch latch;

        public UserSaver( CountDownLatch latch, int userNumber )
        {
            this.latch = latch;
            this.userNumber = userNumber;
        }

        @Override
        public void run()
        {
            try
            {
                logger.info( "Calling userRepository.save() for user #" + this.userNumber );
                userRepository.save( new User( "User" + this.userNumber ) );
                logger.info( "Saved user #" + this.userNumber );
            }
            finally
            {
                latch.countDown();
            }
        }

    }

    @Test
    public void shouldSaveUserAndNewGenre()
    {
        User user = new User( "Michal" );
        user.interestedIn( new Genre( "Drama" ) );

        userRepository.save( user );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})" );
    }

    @Test
    public void shouldSaveUserAndNewGenres()
    {
        User user = new User( "Michal" );
        user.interestedIn( new Genre( "Drama" ) );
        user.interestedIn( new Genre( "Historical" ) );
        user.interestedIn( new Genre( "Thriller" ) );

        userRepository.save( user );

        assertSameGraph( getDatabase(), "CREATE " +
                "(u:User:Person {name:'Michal'})," +
                "(g1:Genre {name:'Drama'})," +
                "(g2:Genre {name:'Historical'})," +
                "(g3:Genre {name:'Thriller'})," +
                "(u)-[:INTERESTED]->(g1)," +
                "(u)-[:INTERESTED]->(g2)," +
                "(u)-[:INTERESTED]->(g3)" );
    }

    @Test
    public void shouldSaveUserAndNewGenre2()
    {
        User user = new User( "Michal" );
        user.interestedIn( new Genre( "Drama" ) );

        userRepository.save( user, 1 );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person {name:'Michal'})-[:INTERESTED]->(g:Genre {name:'Drama'})" );
    }

    @Test
    public void shouldSaveUserAndExistingGenre()
    {
        User michal = new User( "Michal" );
        Genre drama = new Genre( "Drama" );
        michal.interestedIn( drama );

        userRepository.save( michal );

        User vince = new User( "Vince" );
        vince.interestedIn( drama );

        userRepository.save( vince );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(v:User:Person {name:'Vince'})," +
                "(g:Genre {name:'Drama'})," +
                "(m)-[:INTERESTED]->(g)," +
                "(v)-[:INTERESTED]->(g)" );
    }

    @Test
    public void shouldSaveUserButNotGenre()
    {
        User user = new User( "Michal" );
        user.interestedIn( new Genre( "Drama" ) );

        userRepository.save( user, 0 );

        assertSameGraph( getDatabase(), "CREATE (u:User:Person {name:'Michal'})" );
    }

    @Test
    public void shouldUpdateGenreWhenSavedThroughUser()
    {
        User michal = new User( "Michal" );
        Genre drama = new Genre( "Drama" );
        michal.interestedIn( drama );

        userRepository.save( michal );

        drama.setName( "New Drama" );

        userRepository.save( michal );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'New Drama'})," +
                "(m)-[:INTERESTED]->(g)" );
    }

    @Test
    public void shouldRemoveGenreFromUser()
    {
        User michal = new User( "Michal" );
        Genre drama = new Genre( "Drama" );
        michal.interestedIn( drama );

        userRepository.save( michal );

        michal.notInterestedIn( drama );

        userRepository.save( michal );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'Drama'})" );
    }

    @Test
    public void shouldRemoveGenreFromUserUsingService()
    {
        User michal = new User( "Michal" );
        Genre drama = new Genre( "Drama" );
        michal.interestedIn( drama );

        userRepository.save( michal );

        userService.notInterestedIn( michal.getId(), drama.getId() );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(g:Genre {name:'Drama'})" );
    }

    @Test
    public void shouldAddNewVisitorToCinema()
    {
        Cinema cinema = new Cinema( "Odeon" );
        cinema.addVisitor( new User( "Michal" ) );

        cinemaRepository.save( cinema );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(c:Theatre {name:'Odeon', capacity:0})," +
                "(m)-[:VISITED]->(c)" );
    }

    @Test
    public void shouldAddExistingVisitorToCinema()
    {
        User michal = new User( "Michal" );
        userRepository.save( michal );

        Cinema cinema = new Cinema( "Odeon" );
        cinema.addVisitor( michal );

        cinemaRepository.save( cinema );

        assertSameGraph( getDatabase(), "CREATE " +
                "(m:User:Person {name:'Michal'})," +
                "(c:Theatre {name:'Odeon', capacity:0})," +
                "(m)-[:VISITED]->(c)" );
    }

    @Test
    public void shouldBefriendPeople()
    {
        User michal = new User( "Michal" );
        michal.befriend( new User( "Adam" ) );
        userRepository.save( michal );

        try
        {
            assertSameGraph( getDatabase(), "CREATE (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User:Person {name:'Adam'})" );
        }
        catch ( AssertionError error )
        {
            assertSameGraph( getDatabase(), "CREATE (m:User:Person {name:'Michal'})<-[:FRIEND_OF]-(a:User:Person {name:'Adam'})" );
        }
    }

    @Test
    public void shouldLoadFriends()
    {
        new ExecutionEngine( getDatabase() ).execute( "CREATE (m:User {name:'Michal'})-[:FRIEND_OF]->(a:User " +
                "{name:'Adam'})" );

        User michal = ((Iterable<User>)findByProperty(User.class, "name", "Michal" )).iterator().next();
        assertEquals( 1, michal.getFriends().size() );

        User adam = michal.getFriends().iterator().next();
        assertEquals( "Adam", adam.getName() );
        assertEquals( 1, adam.getFriends().size() );

        assertTrue( michal == adam.getFriends().iterator().next() );
        assertTrue( michal.equals( adam.getFriends().iterator().next() ) );
    }

    @Test
    public void shouldLoadFriends2()
    {
        new ExecutionEngine( getDatabase() ).execute( "CREATE (m:User {name:'Michal'})<-[:FRIEND_OF]-(a:User " +
                "{name:'Adam'})" );

        User michal = ((Iterable<User>)findByProperty(User.class, "name", "Michal" )).iterator().next();
        assertEquals( 1, michal.getFriends().size() );

        User adam = michal.getFriends().iterator().next();
        assertEquals( "Adam", adam.getName() );
        assertEquals( 1, adam.getFriends().size() );

        assertTrue( michal == adam.getFriends().iterator().next() );
        assertTrue( michal.equals( adam.getFriends().iterator().next() ) );
    }


    @Test
    public void shouldSaveNewUserAndNewMovieWithRatings()
    {
        User user = new User( "Michal" );
        TempMovie movie = new TempMovie( "Pulp Fiction" );
        user.rate( movie, 5, "Best movie ever" );
        userRepository.save( user );

        User michal = ((Iterable<User>)findByProperty(User.class, "name", "Michal" )).iterator().next();

        assertSameGraph(getDatabase(), "CREATE (u:User:Person {name:'Michal'})-[:RATED {stars:5, " +
                "comment:'Best movie ever'}]->(m:Movie {title:'Pulp Fiction'})");
    }

    @Test
    public void shouldSaveNewUserRatingsForAnExistingMovie()
    {
        TempMovie movie = new TempMovie( "Pulp Fiction" );
        //Save the movie
        movie = tempMovieRepository.save(movie);

        //Create a new user and rate an existing movie
        User user = new User( "Michal" );
        user.rate( movie, 5, "Best movie ever" );
        userRepository.save( user );

        TempMovie tempMovie = ((Iterable<TempMovie>) findByProperty(TempMovie.class, "title", "Pulp Fiction")).iterator().next();
        assertEquals(1,tempMovie.getRatings().size());
    }

    private Calendar createDate( int y, int m, int d, String tz )
    {

        Calendar calendar = Calendar.getInstance();

        calendar.set( y, m, d );
        calendar.setTimeZone( TimeZone.getTimeZone( tz ) );

        // need to do this to ensure the test passes, or the calendar will use the current time's values
        // an alternative (better) would be to specify an date format using one of the @Date converters
        calendar.set( Calendar.HOUR_OF_DAY, 0 );
        calendar.set( Calendar.MINUTE, 0 );
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );

        return calendar;
    }


    private void waitForNeo4jToStart( long maxTimeToWait ) throws Neo4jFailedToStartException
    {
        long startTime = System.currentTimeMillis();
        org.neo4j.ogm.session.transaction.Transaction transaction;

        do
        {
            transaction = new SessionFactory().openSession( neo4jRule.url() ).beginTransaction();
        } while ( transaction == null && System.currentTimeMillis() - startTime <= maxTimeToWait );

        if ( transaction == null )
        {
            throw new Neo4jFailedToStartException( maxTimeToWait );
        }
    }

    private static class Neo4jFailedToStartException extends Exception
    {
        private Neo4jFailedToStartException( long timeoutValue )
        {
            super( String.format( "Could not start neo4j instance in [%d] ms", timeoutValue ) );
        }
    }

    protected Iterable<?> findByProperty(Class clazz, String propertyName, Object propertyValue) {
        return session.loadAll(clazz, new Filter(propertyName, propertyValue));
    }

    protected Iterable<?> findByProperty(Class clazz, String propertyName, Object propertyValue, int depth) {
        return session.loadAll(clazz, new Filter(propertyName, propertyValue), depth);
    }

}
