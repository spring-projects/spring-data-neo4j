package org.neo4j.cineasts.domain;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.cineasts.repository.MovieRepository;
import org.neo4j.cineasts.repository.ActorRepository;
import org.neo4j.cineasts.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * @author mh
 * @since 04.03.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"/movies-test-context.xml"})
@Transactional
public class DomainTest {

    @Autowired
    protected MovieRepository movieRepository;
    @Autowired
    protected UserRepository userRepository;

    @Autowired Neo4jOperations template;
    @Autowired
    ActorRepository actorRepository;

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void actorCanPlayARoleInAMovie() {
        Actor tomHanks = template.save(new Actor("1", "Tom Hanks"));
        Movie forestGump = template.save(new Movie("1", "Forrest Gump"));

        Role role = tomHanks.playedIn(forestGump, "Forrest");
        template.save(role);

        Movie foundForestGump = this.movieRepository.findById("1");

        assertEquals("created and looked up movie equal", forestGump, foundForestGump);
        Role firstRole = foundForestGump.getRoles().iterator().next();
        assertEquals("role forrest",role, firstRole);
        assertEquals("role forrest","Forrest", firstRole.getName());
    }

    @Test
    public void canFindMovieByTitleQuery() {
        Movie forestGump = template.save(new Movie("1", "Forrest Gump"));
        Iterator<Movie> queryResults = movieRepository.findAllByQuery("search", "title", "Forre*").iterator();
        assertTrue("found movie by query",queryResults.hasNext());
        Movie foundMovie = queryResults.next();
        assertEquals("created and looked up movie equal", forestGump, foundMovie);
        assertFalse("found only one movie by query", queryResults.hasNext());
    }

    @Test
    public void userCanRateMovie() {
        Movie movie = template.save(new Movie("1", "Forrest Gump"));
        User user = template.save(new User("ich", "Micha", "password"));
        Rating awesome = user.rate(template,movie, 5, "Awesome");

        user = userRepository.findByPropertyValue("login", "ich");
        movie = movieRepository.findById("1");
        Rating rating = user.getRatings().iterator().next();
        assertEquals(awesome,rating);
        assertEquals("Awesome",rating.getComment());
        assertEquals(5,rating.getStars());
        assertEquals(5,movie.getStars(),0);
    }
    @Test
    public void canFindUserByLogin() {
        User user = template.save(new User("ich", "Micha", "password"));
        User foundUser = userRepository.findByPropertyValue("login", "ich");
        assertEquals(user, foundUser);
    }
    @Test
    public void testCreateUser() throws Exception {
        User user = userRepository.save(new User("me", "me", "me", User.Roles.ROLE_USER));
        assertEquals("user login", "me",user.getLogin());
        assertNotNull("user roles", user.getRole());
        assertEquals("user role", User.Roles.ROLE_USER,user.getRole()[0]);
        User user2=userRepository.findOne(user.getId());
        assertEquals("loaded user id", user.getId(),user2.getId());
        assertEquals("loaded user login", "me",user2.getLogin());
        assertNotNull("loaded user roles", user2.getRole());
        assertEquals("loaded user login", User.Roles.ROLE_USER,user2.getRole()[0]);
    }
}
