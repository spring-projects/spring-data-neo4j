/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.neo4j.ogm.integration.cineasts.annotated;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Rating;
import org.neo4j.ogm.domain.cineasts.annotated.SecurityRole;
import org.neo4j.ogm.domain.cineasts.annotated.Title;
import org.neo4j.ogm.domain.cineasts.annotated.User;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Simple integration test based on cineasts that exercises relationship entities.
 */
public class CineastsIntegrationTest extends InMemoryServerTest {

    @BeforeClass
    public static void init() throws IOException {
        setUp();
        session = new SessionFactory("org.neo4j.ogm.domain.cineasts.annotated").openSession("http://localhost:" + neoPort);
        importCineasts();
    }

    private static void importCineasts() {
        session.execute(load("org/neo4j/ogm/cql/cineasts.cql"));
    }

    @Test
    public void loadRatingsAndCommentsAboutMovies() {
        Collection<Movie> movies = session.loadAll(Movie.class);

        assertEquals(3, movies.size());

        for (Movie movie : movies) {

            System.out.println("Movie: " + movie.getTitle());
            if (movie.getRatings() != null) {
                for (Rating rating : movie.getRatings()) {
                    assertNotNull("The film on the rating shouldn't be null", rating.getMovie());
                    assertSame("The film on the rating was not mapped correctly", movie, rating.getMovie());
                    assertNotNull("The film critic wasn't set", rating.getUser());
                    System.out.println("\trating: " + rating.getMovie());
                    System.out.println("\t\tcomment: " + rating.getComment());
                    System.out.println("\t\tcritic:  " + rating.getUser().getName());
                }
            }
        }
    }

    @Test
    public void loadParticularUserRatingsAndComments() {
        Collection<User> filmCritics = session.loadByProperty(User.class, new Property<String, Object>("name", "Michal"));
        assertEquals(1, filmCritics.size());

        User critic = filmCritics.iterator().next();
        assertEquals(2, critic.getRatings().size());

        for (Rating rating : critic.getRatings()) {
            assertNotNull("The comment should've been mapped", rating.getComment());
            assertTrue("The star rating should've been mapped", rating.getStars() > 0);
            assertNotNull("The user start node should've been mapped", rating.getUser());
            assertNotNull("The movie end node should've been mapped", rating.getMovie());
        }
    }

    @Test
    public void loadRatingsForSpecificFilm() {
        Collection<Movie> films = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Top Gear"));
        assertEquals(1, films.size());

        Movie film = films.iterator().next();
        assertEquals(2, film.getRatings().size());

        for (Rating rating : film.getRatings()) {
            assertTrue("The star rating should've been mapped", rating.getStars() > 0);
            assertNotNull("The user start node should've been mapped", rating.getUser());
            assertSame("The wrong film was mapped to the rating", film, rating.getMovie());
        }
    }

    @Test
    public void saveAndRetrieveUserWithSecurityRoles() {
        User user = new User();
        user.setLogin("daniela");
        user.setName("Daniela");
        user.setPassword("daniela");
        user.setSecurityRoles(new SecurityRole[]{SecurityRole.USER});
        session.save(user);

        Collection<User> users = session.loadByProperty(User.class,new Property<String, Object>("login","daniela"));
        assertEquals(1,users.size());
        User daniela = users.iterator().next();
        assertEquals("Daniela", daniela.getName());
        assertEquals(1,daniela.getSecurityRoles().length);
        assertEquals(SecurityRole.USER,daniela.getSecurityRoles()[0]);
    }

    @Test
    public void saveAndRetrieveUserWithTitles() {
        User user = new User();
        user.setLogin("vince");
        user.setName("Vince");
        user.setPassword("vince");
        user.setTitles(Arrays.asList(Title.MR));
        session.save(user);

        Collection<User> users = session.loadByProperty(User.class,new Property<String, Object>("login","vince"));
        assertEquals(1,users.size());
        User vince = users.iterator().next();
        assertEquals("Vince", vince.getName());
        assertEquals(1, vince.getTitles().size());
        assertEquals(Title.MR,vince.getTitles().get(0));

    }

    @Test
    public void saveAndRetrieveUserWithDifferentCharset() {
        User user = new User();
        user.setLogin("aki");
        user.setName("Aki Kaurismäki");
        user.setPassword("aki");
        session.save(user);

        Collection<User> users = session.loadByProperty(User.class,new Property<String, Object>("login","aki"));
        assertEquals(1,users.size());
        User aki = users.iterator().next();
        assertEquals("Aki Kaurismäki", aki.getName());

    }

    @Test
    @Ignore
    public void shouldSaveRatingWithMovie() {
        Movie movie = new Movie();
        movie.setTitle("Pulp Fiction");

        User michal = new User();
        michal.setName("Michal");

        Set<Rating> ratings = new HashSet<>();
        Rating awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(michal);
        awesome.setStars(5);
        ratings.add(awesome);

        michal.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        assertEquals("Michal",movie.getRatings().iterator().next().getUser().getName());
    }

    @Test
    @Ignore
    public void shouldBeAbleToModifyRating() {
        Movie movie = new Movie();
        movie.setTitle("Pulp Fiction");

        User michal = new User();
        michal.setName("Michal");

        Set<Rating> ratings = new HashSet<>();
        Rating awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(michal);
        awesome.setStars(5);
        ratings.add(awesome);

        michal.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        Rating rating =  movie.getRatings().iterator().next();
        assertEquals("Michal", rating.getUser().getName());
        assertEquals(5,rating.getStars());

        //Modify the rating stars
        ratings.iterator().next().setStars(2);
        michal.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);
        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        rating =  movie.getRatings().iterator().next();
        assertEquals("Michal", rating.getUser().getName());
        assertEquals(2,rating.getStars());

    }

    @Test
    @Ignore
    public void shouldSaveMultipleUserRatingsForAMovie() {  //Even though this is an unrealistic use case
        Movie pulp = new Movie();
        pulp.setTitle("Pulp Fiction");
        session.save(pulp);

        User michal = new User();
        michal.setName("Michal");

        Set<Rating> ratings = new HashSet<>();
        Rating good = new Rating();
        good.setUser(michal);
        good.setMovie(pulp);
        good.setStars(3);
        ratings.add(good);
        michal.setRatings(ratings);
        pulp.setRatings(ratings);

        session.save(michal);
        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        pulp = movies.iterator().next();
        assertNotNull(pulp.getRatings());
        assertEquals(1,pulp.getRatings().size());
        assertEquals("Michal",pulp.getRatings().iterator().next().getUser().getName());

        Rating betterNextTime = new Rating();
        betterNextTime.setMovie(pulp);
        betterNextTime.setUser(michal);
        betterNextTime.setStars(4);
        ratings.add(betterNextTime);
        michal.setRatings(ratings);
        pulp.setRatings(ratings);

        session.save(michal);
        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        pulp = movies.iterator().next();
        assertNotNull(pulp.getRatings());
        assertEquals(2,pulp.getRatings().size());
    }

    @Test
    @Ignore
    public void shouldSaveMultipleUserRatings() {
        Set<Rating> pulpRatings = new HashSet<>();
        Set<Rating> topGearRatings = new HashSet<>();

        Movie pulp = new Movie();
        pulp.setTitle("Pulp Fiction");
        session.save(pulp);

        Movie topGear = new Movie();
        topGear.setTitle("Top Gear");
        session.save(topGear);

        User michal = new User();
        michal.setName("Michal");

        Rating good = new Rating();
        good.setUser(michal);
        good.setMovie(pulp);
        good.setStars(3);
        pulpRatings.add(good);
        pulp.setRatings(pulpRatings);

        Rating okay = new Rating();
        okay.setMovie(topGear);
        okay.setUser(michal);
        okay.setStars(2);
        topGearRatings.add(okay);
        topGear.setRatings(topGearRatings);

        Set<Rating> michalsRatings = new HashSet<>();
        michalsRatings.add(good);
        michalsRatings.add(okay);
        michal.setRatings(michalsRatings);

        session.save(michal);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        pulp = movies.iterator().next();
        assertNotNull(pulp.getRatings());
        assertEquals(1,pulp.getRatings().size());

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Top Gear"));
        topGear = movies.iterator().next();
        assertNotNull(topGear.getRatings());
        assertEquals(1,topGear.getRatings().size());

        michal = session.loadByProperty(User.class,new Property<String, Object>("name","Michal")).iterator().next();
        assertEquals(2,michal.getRatings().size());

        michal.setRatings(pulpRatings); //Get rid of the Top Gear rating
        session.save(michal);

        michal = session.loadByProperty(User.class,new Property<String, Object>("name","Michal")).iterator().next();
        assertEquals(1,michal.getRatings().size());

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Top Gear"));
        topGear = movies.iterator().next();
        assertNotNull(topGear.getRatings());
        assertEquals(0,topGear.getRatings().size());

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Pulp Fiction"));
        pulp = movies.iterator().next();
        assertNotNull(pulp.getRatings());
        assertEquals(1,pulp.getRatings().size());

    }
}
