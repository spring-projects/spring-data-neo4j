package org.neo4j.ogm.defects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Rating;
import org.neo4j.ogm.domain.cineasts.annotated.User;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.WrappingServerIntegrationTest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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

    @Test
    public void shouldCreateREWithExistingStartAndEndNodes() {
        session.execute(load("org/neo4j/ogm/cql/cineasts.cql"));

        Collection<Movie> films = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Top Gear"));
        Movie movie = films.iterator().next();
        assertEquals(2,movie.getRatings().size());

        User michal = session.loadByProperty(User.class, new Property<String, Object>("name", "Michal")).iterator().next();

        Set<Rating> ratings = new HashSet<>();
        Rating awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(michal);
        awesome.setStars(5);
        ratings.add(awesome);

        michal.setRatings(ratings); //Overwrite Michal's earlier rating
        movie.setRatings(ratings);
        session.save(movie);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Top Gear"));
        movie = movies.iterator().next();
        assertNotNull(movie.getRatings()); //Fails. But when entities are created first, test passes, see CineastsRatingsTest.shouldSaveRatingWithMovie
        assertEquals(1,movie.getRatings().size());
        assertEquals("Michal",movie.getRatings().iterator().next().getUser().getName());
    }

    @Test
    public void shouldNotLoseRelationshipEntitiesWhenALoadedEntityIsPersisted() {
        session.execute(load("org/neo4j/ogm/cql/cineasts.cql"));

        Movie topGear = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Top Gear")).iterator().next();
        assertEquals(2,topGear.getRatings().size());  //2 ratings
        session.save(topGear);

        topGear = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Top Gear")).iterator().next();
        assertEquals(2,topGear.getRatings().size());  //Then there was one

        User michal = session.loadByProperty(User.class, new Property<String, Object>("name", "Michal")).iterator().next();
        assertEquals(2,michal.getRatings().size());  //The Top Gear Rating is gone
    }

    @Test
    public void shouldLoadActorsForAPersistedMovie() {
        session.execute(
                "CREATE " +
                        "(dh:Movie {title:'Die Hard'}), " +
                        "(bw:Actor {name: 'Bruce Willis'}), " +
                        "(bw)-[:ACTS_IN {role : 'John'}]->(dh)");

        //This works
        /*Actor bruce = IteratorUtil.firstOrNull(session.loadByProperty(Actor.class, new Property<String, Object>("name","Bruce Willis")));
        assertNotNull(bruce);
        assertEquals(1,bruce.getRoles().size());
        */

        /* This loads the movie but not roles.
           It works only when
           1. Either Movie.setRoles is not defined, or Movie.setRoles is defined and annotated
           2. There is no method which accepts a Role parameter such as the following in the Movie class:
            public void addRole(Role role) {
                roles.add(role);
            }

         */
        Movie dieHard = IteratorUtil.firstOrNull(session.loadByProperty(Movie.class, new Property<String, Object>("title", "Die Hard")));
        assertNotNull(dieHard);
        assertNotNull(dieHard.getRoles());
        assertEquals(1,dieHard.getRoles().size());
    }

    /**
     From IntegrationTest, move tests that depend on it to appropriate places once fixed
     */
    private static String load(String cqlFile) {    //
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(cqlFile)));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                sb.append(" ");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

}
