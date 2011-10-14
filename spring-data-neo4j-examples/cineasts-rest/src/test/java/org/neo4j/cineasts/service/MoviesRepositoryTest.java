package org.neo4j.cineasts.service;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author mh
 * @since 04.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class MoviesRepositoryTest {
    @Autowired
    CineastsRepository repository;

    @Test
    @Ignore("Fails over REST")
    public void testGetMovie() throws Exception {
        Movie movie = new Movie("1", "Test-Movie").persist();
        Movie found = repository.getMovie("1");
        assertEquals("movie found by id", movie, found);

    }

    @Test
    @Ignore("Fails over REST")
    public void testFindTwoMovies() throws Exception {
        Movie movie1 = new Movie("1", "Test-Movie1").persist();
        Movie movie2 = new Movie("2", "Test-Movie2").persist();
        Movie movie3 = new Movie("3", "Another-Movie3").persist();
        List<Movie> found = repository.findMovies("Test*", 2);
        assertEquals("2 movies found",2,found.size());
        assertEquals("2 correct movies found by query", new HashSet<Movie>(asList(movie1, movie2)), new HashSet<Movie>(found));
    }

    @Test
    @Ignore("Fails over REST")
    public void testFindTwoMoviesButRestrictToOne() throws Exception {
        Movie movie1 = new Movie("1", "Test-Movie1").persist();
        Movie movie2 = new Movie("2", "Test-Movie2").persist();
        Movie movie3 = new Movie("3", "Another-Movie3").persist();
        List<Movie> found = repository.findMovies("Test*", 1);
        assertEquals("1 movie found",1,found.size());
        assertTrue("1 correct movie found by query", found.get(0).getTitle().startsWith("Test"));
    }
}
