package org.neo4j.cineasts.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.MovieRecommendation;
import org.neo4j.cineasts.domain.Rating;
import org.neo4j.cineasts.domain.User;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.neo4j.template.Neo4jOperations;
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
    @Autowired MovieRepository movieRepository;
    @Autowired UserRepository userRepository;
    @Autowired Neo4jOperations template;

    @Test
    public void testGetMovie() throws Exception {
        Movie movie = movieRepository.save(new Movie("1", "Test-Movie"));
        Movie found = movieRepository.findById("1");
        assertEquals("movie found by id", movie, found);
    }

    @Test
    public void testGetMovieRecommendations() throws Exception {
        Movie movie = movieRepository.save(new Movie("1", "Test-Movie"));
        Movie movie2 = movieRepository.save(new Movie("2", "Test-Movie2"));
        User user = userRepository.save(new User("me", "me", "me"));
        user.rate(template,movie,3,"me");
        User friend = userRepository.save(new User("friend", "friend", "friend"));
        friend.rate(template, movie, 5, "friend");
        friend.rate(template, movie2, 3, "friend2");
        assertEquals(2,movieRepository.count());
        final List<MovieRecommendation> recommendations = movieRepository.getRecommendations(user);
        assertEquals("one recommendation", 1, recommendations.size());
        assertEquals("one recommendation", movie2, recommendations.get(0).getMovie());
        assertEquals("one recommendation", 3, recommendations.get(0).getRating());
    }
    @Test
    public void testRateMovie() throws Exception {
        Movie movie = movieRepository.save(new Movie("1", "Test-Movie"));
        User user = userRepository.save(new User("me", "me", "me"));
        user.rate(template, movie, 5, "comment");
        movie = movieRepository.findById("1");
        final Rating rating = IteratorUtil.first(movie.getRatings());
        assertEquals("rating stars", 5, rating.getStars());
        assertEquals("rating comment", "comment", rating.getComment());
        assertEquals("rating user", user, rating.getUser());
        assertEquals("rating movie", movie, rating.getMovie());
    }

    @Test
    public void testFindTwoMovies() throws Exception {
        Movie movie1 = movieRepository.save(new Movie("1", "Test-Movie1"));
        Movie movie2 = movieRepository.save(new Movie("2", "Test-Movie2"));
        Movie movie3 = movieRepository.save(new Movie("3", "Another-Movie3"));
        List<Movie> found = movieRepository.findByTitleLike("Test*", new PageRequest(0, 2)).getContent();
        assertEquals("2 movies found", 2, found.size());
        assertEquals("2 correct movies found by query", new HashSet<Movie>(asList(movie1, movie2)), new HashSet<Movie>(found));
    }

    @Test
    public void testFindTwoMoviesButRestrictToOne() throws Exception {
        Movie movie1 = movieRepository.save(new Movie("1", "Test-Movie1"));
        Movie movie2 = movieRepository.save(new Movie("2", "Test-Movie2"));
        Movie movie3 = movieRepository.save(new Movie("3", "Another-Movie3"));
        List<Movie> found = movieRepository.findByTitleLike("Test*", new PageRequest(0, 1)).getContent();
        assertEquals("1 movie found",1,found.size());
        assertTrue("1 correct movie found by query", found.get(0).getTitle().startsWith("Test"));
    }
}
