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

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Rating;
import org.neo4j.ogm.domain.cineasts.annotated.User;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Simple integration test based on cineasts that exercises the Rating relationship entity.
 */
public class CineastsRatingsTest extends InMemoryServerTest {

    @Before
    public void init() throws IOException {
        setUp();
        session = new SessionFactory("org.neo4j.ogm.domain.cineasts.annotated").openSession("http://localhost:" + neoPort);
    }

    @Test
    public void shouldSaveRatingWithMovie() {
        Movie movie = new Movie();
        movie.setTitle("Harry Potter and the Chamber of Secrets");

        User luanne = new User();
        luanne.setName("Luanne");

        Set<Rating> ratings = new HashSet<>();
        Rating awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(luanne);
        awesome.setStars(5);
        ratings.add(awesome);

        luanne.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Chamber of Secrets"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        assertEquals("Luanne",movie.getRatings().iterator().next().getUser().getName());

        //Overwrite the rating
        ratings = new HashSet<>();
        awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(luanne);
        awesome.setStars(4);
        ratings.add(awesome);

        luanne.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Chamber of Secrets"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        assertEquals(4,movie.getRatings().iterator().next().getStars());
    }

    @Test
    public void shouldBeAbleToModifyRating() {
        Movie movie = new Movie();
        movie.setTitle("Harry Potter and the Philosophers Stone");

        User vince = new User();
        vince.setName("Vince");

        Set<Rating> ratings = new HashSet<>();
        Rating awesome = new Rating();
        awesome.setComment("Awesome");
        awesome.setMovie(movie);
        awesome.setUser(vince);
        awesome.setStars(5);
        ratings.add(awesome);

        vince.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Philosophers Stone"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        Rating rating =  movie.getRatings().iterator().next();
        assertEquals("Vince", rating.getUser().getName());
        assertEquals(5,rating.getStars());

        //Modify the rating stars
        ratings.iterator().next().setStars(2);
        vince.setRatings(ratings);
        movie.setRatings(ratings);
        session.save(movie);
        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Philosophers Stone"));
        movie = movies.iterator().next();
        assertEquals(1,movie.getRatings().size());
        rating =  movie.getRatings().iterator().next();
        assertEquals("Vince", rating.getUser().getName());
        assertEquals(2,rating.getStars());

    }

    @Test
    public void shouldSaveMultipleUserRatingsForAMovie() {  //Even though this is an unrealistic use case
        Movie azkaban = new Movie();
        azkaban.setTitle("Harry Potter and the Prisoner of Azkaban");
        session.save(azkaban);

        User daniela = new User();
        daniela.setName("Daniela");

        Set<Rating> ratings = new HashSet<>();
        Rating good = new Rating();
        good.setUser(daniela);
        good.setMovie(azkaban);
        good.setStars(3);
        ratings.add(good);
        daniela.setRatings(ratings);
        azkaban.setRatings(ratings);

        session.save(daniela);
        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Prisoner of Azkaban"));
        azkaban = movies.iterator().next();
        assertNotNull(azkaban.getRatings());
        assertEquals(1, azkaban.getRatings().size());
        assertEquals("Daniela",azkaban.getRatings().iterator().next().getUser().getName());

        Rating betterNextTime = new Rating();
        betterNextTime.setMovie(azkaban);
        betterNextTime.setUser(daniela);
        betterNextTime.setStars(4);
        ratings.add(betterNextTime);
        daniela.setRatings(ratings);
        azkaban.setRatings(ratings);

        session.save(daniela);
        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Prisoner of Azkaban"));
        azkaban = movies.iterator().next();
        assertNotNull(azkaban.getRatings());
        assertEquals(2, azkaban.getRatings().size());
    }

    @Test
    public void shouldBeAbleToSaveAndUpdateMultipleUserRatings() {
        Set<Rating> azkabanRatings = new HashSet<>();
        Set<Rating> phoenixRatings = new HashSet<>();

        Movie azkaban = new Movie();
        azkaban.setTitle("Harry Potter and the Goblet of Fire");
        session.save(azkaban);

        Movie phoenix = new Movie();
        phoenix.setTitle("Harry Potter and the Order of the Phoenix");
        session.save(phoenix);

        User adam = new User();
        adam.setName("Adam");

        Rating good = new Rating();
        good.setUser(adam);
        good.setMovie(azkaban);
        good.setStars(3);
        azkabanRatings.add(good);
        azkaban.setRatings(azkabanRatings);

        Rating okay = new Rating();
        okay.setMovie(phoenix);
        okay.setUser(adam);
        okay.setStars(2);
        phoenixRatings.add(okay);
        phoenix.setRatings(phoenixRatings);

        Set<Rating> michalsRatings = new HashSet<>();
        michalsRatings.add(good);
        michalsRatings.add(okay);
        adam.setRatings(michalsRatings);

        session.save(adam);

        Collection<Movie> movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Goblet of Fire"));
        azkaban = movies.iterator().next();
        assertNotNull(azkaban.getRatings());
        assertEquals(1, azkaban.getRatings().size());

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Order of the Phoenix"));
        phoenix = movies.iterator().next();
        assertNotNull(phoenix.getRatings());
        assertEquals(1, phoenix.getRatings().size());

        adam = session.loadByProperty(User.class,new Property<String, Object>("name","Adam")).iterator().next();
        assertEquals(2, adam.getRatings().size());

        adam.setRatings(azkabanRatings); //Get rid of the Phoenix rating
        session.save(adam);

        adam = session.loadByProperty(User.class,new Property<String, Object>("name","Adam")).iterator().next();
        assertEquals(1, adam.getRatings().size());

        movies = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Order of the Phoenix"));
        phoenix = movies.iterator().next();
        assertNull(phoenix.getRatings());

        movies = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Goblet of Fire"));
        azkaban = movies.iterator().next();
        assertNotNull(azkaban.getRatings());
        assertEquals(1,azkaban.getRatings().size());

    }

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void shouldSaveAndRetrieveRelationshipEntitiesDirectly() {
        // we need some guff in the database
        session.execute(
                "CREATE " +
                        "(nc:NotAClass {name:'Colin'}), " +
                        "(g:NotAClass {age: 39}), " +
                        "(g)-[:TEST {comment : 'test'}]->(nc)");

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

        //load the rating by id
        Rating loadedRating = session.load(Rating.class, filmRating.getId());
        assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The relationship properties weren't saved correctly", filmRating.getStars(), loadedRating.getStars());
        assertEquals("The rated film wasn't saved correctly", film.getTitle(), loadedRating.getMovie().getTitle());
        assertEquals("The critic wasn't saved correctly", critic.getId(), loadedRating.getUser().getId());
    }

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void shouldSaveAndRetrieveRelationshipEntitiesPreExistingDirectly() {

        session.execute(
                "CREATE " +
                        "(ff:Movie {title:'Fast and Furious XVII'}), " +
                        "(g:User {name: 'Gary'}), " +
                        "(g)-[:RATED {comment : 'Too many of these films!'}]->(ff)");

        Rating loadedRating = session.load(Rating.class, 0l);
        assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The rated film wasn't saved correctly", "Fast and Furious XVII", loadedRating.getMovie().getTitle());
        assertEquals("The critic wasn't saved correctly", "Gary", loadedRating.getUser().getName());
    }


}
