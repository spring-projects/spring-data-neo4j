package org.neo4j.cineasts.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.MovieRecommendation;
import org.neo4j.cineasts.domain.Rating;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    @Autowired
    MovieRepository movieRepository;

    @Test
    public void testGetMovie() throws Exception {
        Movie movie = new Movie("1", "Test-Movie").persist();
        Movie found = repository.getMovie("1");
        assertEquals("movie found by id", movie, found);

    }
    @Test
    public void testGetMovieRecommendations() throws Exception {
        Movie movie = new Movie("1", "Test-Movie").persist();
        Movie movie2 = new Movie("2", "Test-Movie2").persist();
        User user=new User("me","me","me").persist();
        user.rate(movie,3,"me");
        User friend=new User("friend","friend","friend").persist();
        friend.rate(movie,5,"friend");
        friend.rate(movie2,3,"friend2");
        assertEquals(2,movieRepository.count());
        final List<MovieRecommendation> recommendations = IteratorUtil.addToCollection(repository.recommendMovies(user), new ArrayList<MovieRecommendation>());
        assertEquals("one recommendation", 1, recommendations.size());
        assertEquals("one recommendation", movie2, recommendations.get(0).getMovie());
        assertEquals("one recommendation", 3, recommendations.get(0).getRating());
    }
    @Test
    public void testRateMovie() throws Exception {
        Movie movie = new Movie("1", "Test-Movie").persist();
        User user=new User("me","me","me").persist();
        repository.rateMovie(movie,user,5,"comment");
        final Rating rating = IteratorUtil.first(movie.getRatings());
        assertEquals("rating stars", 5, rating.getStars());
        assertEquals("rating comment", "comment", rating.getComment());
        assertEquals("rating user", user, rating.getUser());
        assertEquals("rating movie", movie, rating.getMovie());
    }

    @Test
    public void testFindTwoMovies() throws Exception {
        Movie movie1 = new Movie("1", "Test-Movie1").persist();
        Movie movie2 = new Movie("2", "Test-Movie2").persist();
        Movie movie3 = new Movie("3", "Another-Movie3").persist();
        List<Movie> found = repository.findMovies("Test*", 2).getContent();
        assertEquals("2 movies found", 2, found.size());
        assertEquals("2 correct movies found by query", new HashSet<Movie>(asList(movie1, movie2)), new HashSet<Movie>(found));
    }

    @Test
    public void testFindTwoMoviesButRestrictToOne() throws Exception {
        Movie movie1 = new Movie("1", "Test-Movie1").persist();
        Movie movie2 = new Movie("2", "Test-Movie2").persist();
        Movie movie3 = new Movie("3", "Another-Movie3").persist();
        List<Movie> found = repository.findMovies("Test*", 1).getContent();
        assertEquals("1 movie found",1,found.size());
        assertTrue("1 correct movie found by query", found.get(0).getTitle().startsWith("Test"));
    }
}
