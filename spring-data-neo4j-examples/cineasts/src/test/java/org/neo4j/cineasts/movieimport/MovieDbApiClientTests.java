package org.neo4j.cineasts.movieimport;

import org.junit.Ignore;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * @author mh
 * @since 13.03.11
 */
@Ignore
public class MovieDbApiClientTests {
    private static final String API_KEY = "926d2a79e82920b62f03b1cb57e532e6";

    @Test
    public void testGetMovie() throws Exception {
        Map movie = new MovieDbApiClient(API_KEY).getMovie("2");
        assertEquals(2,movie.get("id"));
    }
    @Test
    public void testGetProfileUrl() throws Exception {
        String format = new MovieDbApiClient(API_KEY).getProfileFormat();
        assertEquals("http://image.tmdb.org/t/p/w45/%s",format);
    }
    @Test
    public void testGetPosterUrl() throws Exception {
        String format = new MovieDbApiClient(API_KEY).getPosterFormat();
        assertEquals("http://image.tmdb.org/t/p/w342/%s",format);
    }

    @Test
    public void testGetPerson() throws Exception {
        Map person = new MovieDbApiClient(API_KEY).getPerson("30112");
        assertEquals(30112,person.get("id"));

    }
}
