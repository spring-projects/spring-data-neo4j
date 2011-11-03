package org.neo4j.cineasts.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

/**
 * @author mh
 * @since 04.03.11
 */
@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( { "/movies-test-context.xml" } )
@Transactional
public class MoviesRepositoryTest extends RestTestBase {
    @Autowired
    CineastsRepository repository;

    @Test
    public void testGetMovie() throws Exception {
        Movie movie = new Movie( "1", "Test-Movie" ).persist();
        Movie found = repository.getMovie( "1" );
        assertEquals( "movie found by id", movie, found );

    }

    @Test
    public void testFindTwoMovies() throws Exception {
        Movie movie1 = new Movie( "1", "Test-Movie1" ).persist();
        Movie movie2 = new Movie( "2", "Test-Movie2" ).persist();
        Movie movie3 = new Movie( "3", "Another-Movie3" ).persist();
        Page<Movie> found = repository.findMovies( "Test*", 2 );

        Collection<Movie> movies = asCollection( found );

        assertEquals( "2 movies found", 2, movies.size() );
        assertThat( found, hasItem( movie1 ) );
        assertThat( found, hasItem(movie2) );
    }

    @Test
    public void testFindTwoMoviesButRestrictToOne() throws Exception {
        Movie movie1 = new Movie( "1", "Test-Movie1" ).persist();
        Movie movie2 = new Movie( "2", "Test-Movie2" ).persist();
        Movie movie3 = new Movie( "3", "Another-Movie3" ).persist();
        Page<Movie> found = repository.findMovies( "Test*", 1 );

        Collection<Movie> movies = asCollection( found );

        assertEquals( "1 movie found", 1, movies.size() );
        assertTrue( "1 correct movie found by query", movies.iterator().next().getTitle().startsWith( "Test" ) );
    }
}
