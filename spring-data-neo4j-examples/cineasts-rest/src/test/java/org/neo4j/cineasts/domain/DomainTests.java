package org.neo4j.cineasts.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.rest.support.RestTestBase;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.hasItem;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class DomainTests extends RestTestBase {

    @Autowired
    protected MovieRepository movieRepository;
    @Autowired
    protected UserRepository userRepository;

    @Test
    public void actorCanPlayARoleInAMovie() {
        Person tomHanks = new Person("1", "Tom Hanks").persist();
        Movie forestGump = new Movie("1", "Forrest Gump").persist();

        Role role = tomHanks.playedIn(forestGump, "Forrest");

        Movie foundForestGump = this.movieRepository.findByPropertyValue(null, "id", "1");

        assertEquals("created and looked up movie equal", forestGump, foundForestGump);
        Role firstRole = foundForestGump.getRoles().iterator().next();
        assertEquals("role forrest", role, firstRole);
        assertEquals("role forrest", "Forrest", firstRole.getName());
    }

    @Test
    public void canFindMovieByTitleQuery() {
        Movie forestGump = new Movie("1", "Forrest Gump").persist();
        Iterator<Movie> queryResults = movieRepository.findAllByQuery("search", "title", "Forre*").iterator();
        assertTrue("found movie by query", queryResults.hasNext());
        Movie foundMovie = queryResults.next();
        assertEquals("created and looked up movie equal", forestGump, foundMovie);
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

    @Test
    public void userCanRateMovie() {
        Movie movie = new Movie("1", "Forrest Gump").persist();
        User user = new User("ich", "Micha", "password").persist();
        Rating awesome = user.rate(movie, 5, "Awesome");

        User foundUser = userRepository.findByPropertyValue("login", "ich");
        Rating rating = user.getRatings().iterator().next();
        assertEquals(awesome, rating);
        assertEquals("Awesome", rating.getComment());
        assertEquals(5, rating.getStars());
        assertEquals(5, movie.getStars(), 0);
    }

    @Test
    public void shouldBeAbleToFindCoActorsThroughCypher() throws Exception {
        Movie citizenKane = new Movie("1", "Citizen Kane").persist();
        Person orson = new Person("1", "Orson Wells").persist();
        Person dorothy = new Person("2", "Dorothy Comingore").persist();
        Person joseph = new Person("3", "Joseph Cotten").persist();
        orson.playedIn(citizenKane, "Charles Foster Kane");
        dorothy.playedIn(citizenKane, "Susan Alexander Kane");
        joseph.playedIn(citizenKane, "Jedediah Leland");

        Iterable<Person> coActors = orson.getCoActors();

        assertThat(coActors, hasItem(dorothy));
        assertThat(coActors, hasItem(joseph));
    }

    @Test
    public void canFindUserByLogin() {
        User user = new User("ich", "Micha", "password").persist();
        User foundUser = userRepository.findByPropertyValue("login", "ich");
        assertEquals(user, foundUser);
    }
}
