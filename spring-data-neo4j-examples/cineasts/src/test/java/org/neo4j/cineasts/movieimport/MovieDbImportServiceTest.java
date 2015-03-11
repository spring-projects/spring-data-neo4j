package org.neo4j.cineasts.movieimport;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.PersistenceContext;
import org.neo4j.cineasts.domain.Actor;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.Person;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 13.03.11
 */
@ContextConfiguration(classes = {PersistenceContext.class})
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class MovieDbImportServiceTest extends WrappingServerIntegrationTest {

    @Autowired
    MovieDbImportService importService;
    @Autowired
    MovieRepository movieRepository;

    @Override
    protected int neoServerPort() {
        return PersistenceContext.NEO4J_PORT;
    }

    @Test
    public void testImportMovie() throws Exception {
        Movie movie = importService.importMovie("2");
        assertEquals("movie-id", "2", movie.getId());
        assertEquals("movie-title", "Ariel", movie.getTitle());
    }

    @Test
    @Ignore
    public void testImportMovieWithSamePersonAsActorAndDirector() throws Exception {
        Movie movie = importService.importMovie("200");
        assertEquals("movie-id", "200", movie.getId());
        assertEquals("movie-title", "Star Trek: Insurrection", movie.getTitle());
    }

    @Test
    public void testImportMovieTwice() throws Exception {
        Movie movie = importService.importMovie("603");
        Movie movie2 = importService.importMovie("603");
        final Movie foundMovie = movieRepository.findById("603");
        assertEquals("movie-id", movie, foundMovie);
    }

    @Test
    public void testImportPerson() throws Exception {
        Person actor = importService.importPerson("105955", new Actor("105955", null));
        assertEquals("person-id", "105955", actor.getId());
        assertEquals("person-title", "George M. Williamson", actor.getName());
    }

    @Test
    public void shouldImportMovieWithTwoDirectors() throws Exception {
        Movie movie = importService.importMovie("603");
        movie = movieRepository.findByProperty("id", "603").iterator().next();
        assertEquals(2, movie.getDirectors().size());
    }
}
