package org.neo4j.cineasts.movieimport;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.Person;
import org.neo4j.cineasts.service.CineastsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 13.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class MovieDbImportServiceTest extends RestTestBase {

    @Autowired
    MovieDbImportService importService;

    @Autowired
    CineastsRepository cineastsRepository;

    @Test
    public void testImportMovie() throws Exception {
        Movie movie = importService.importMovie("2");
        assertEquals("movie-id","2", movie.getId());
        assertEquals("movie-title","Ariel", movie.getTitle());
    }

    @Test
    public void testImportMovieTwice() throws Exception {
        Movie movie = importService.importMovie("2");
        Movie movie2 = importService.importMovie("2");
        final Movie foundMovie = cineastsRepository.getMovie("2");
        assertEquals("movie-id", movie, foundMovie);
    }

    @Test
    public void testImportPerson() throws Exception {
        Person actor = importService.importPerson("105955");
        assertEquals("movie-id","105955", actor.getId());
        assertEquals("movie-title","George M. Williamson", actor.getName());
    }
}
