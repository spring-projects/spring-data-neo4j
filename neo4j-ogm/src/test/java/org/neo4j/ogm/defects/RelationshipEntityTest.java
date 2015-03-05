package org.neo4j.ogm.defects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Rating;
import org.neo4j.ogm.domain.cineasts.annotated.User;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Ignore
public class RelationshipEntityTest extends WrappingServerIntegrationTest {

    private Session session;

    @Before
    public void setUpSession() {
        SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.cineasts.annotated");
        session = sessionFactory.openSession(baseNeoUrl());
    }

    @Test
    public void shouldSaveAndRetrieveRelationshipEntitiesDirectly() {
        // we need some guff in the database to observe the failure
        try (Transaction tx = getDatabase().beginTx()) {
            Node arbitraryNode = getDatabase().createNode(DynamicLabel.label("NotAClass"));
            arbitraryNode.setProperty("name", "Colin");
            Node otherNode = getDatabase().createNode(DynamicLabel.label("NotAClass"));
            otherNode.setProperty("age", 39);
            arbitraryNode.createRelationshipTo(otherNode, DynamicRelationshipType.withName("TEST"));

            tx.success();
        }

        User critic = new User();
        critic.setName("Gary");
        Movie film = new Movie();
        film.setTitle("Fast and Furious XVII");
        Rating filmRating = new Rating();
        filmRating.setUser(critic);
        critic.setRatings(Collections.singleton(filmRating));
        filmRating.setMovie(film);
        film.setRatings(Collections.singleton(filmRating));
        filmRating.setStars(2);
        filmRating.setComment("They've made far too many of these films now!");

        session.save(filmRating);

        Rating loadedRating = session.load(Rating.class, filmRating.getId());
        // this passes but the above statement fails
//        Rating loadedRating = session.load(User.class, critic.getId()).getRatings().iterator().next();
        assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
        assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
        assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
    }

    @Test
    public void shouldSaveMultipleRatingsFromDifferentUsersForSameMovie() {
        Movie movie = new Movie();
        movie.setTitle("Pulp Fiction");
        session.save(movie);

        User michal = new User();
        michal.setName("Michal");

        Rating awesome = new Rating();
        awesome.setMovie(movie);
        awesome.setUser(michal);
        awesome.setStars(5);
        michal.setRatings(Collections.singleton(awesome));
        session.save(michal);

        //Check that Pulp Fiction has one rating from Michal
        Collection<Movie> films = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Pulp Fiction"));
        assertEquals(1, films.size());

        Movie film = films.iterator().next();
        assertNotNull(film);
        assertEquals(1, film.getRatings().size());
        assertEquals("Michal",film.getRatings().iterator().next().getUser().getName());

        //Add a rating from luanne for the same movie
        User luanne = new User();
        luanne.setName("luanne");
        luanne.setLogin("luanne");
        luanne.setPassword("luanne");

        Rating rating = new Rating();
        rating.setMovie(film);
        rating.setUser(luanne);
        rating.setStars(3);
        luanne.setRatings(Collections.singleton(rating));
        session.save(luanne);

        //Verify that pulp fiction has two ratings
        films = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Pulp Fiction"));
        film = films.iterator().next();
        assertEquals(2, film.getRatings().size());   //Fail, it has just one rating, luannes

        //Verify that luanne's rating is saved
        Collection<User> users = session.loadByProperty(User.class,new Property<String, Object>("login","luanne"));
        User foundLuanne = users.iterator().next();
        assertEquals(1,foundLuanne.getRatings().size());

        //Verify that Michals rating still exists
        users = session.loadByProperty(User.class,new Property<String, Object>("name","Michal"));
        User foundMichal = users.iterator().next();
        assertEquals(1,foundMichal.getRatings().size()); //Fail, Michals rating is gone
    }

}
