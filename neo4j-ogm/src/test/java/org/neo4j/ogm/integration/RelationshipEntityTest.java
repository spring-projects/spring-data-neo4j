/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.integration;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.helpers.collection.IteratorUtil;
import org.neo4j.ogm.domain.cineasts.annotated.Actor;
import org.neo4j.ogm.domain.cineasts.annotated.Knows;
import org.neo4j.ogm.domain.cineasts.annotated.Movie;
import org.neo4j.ogm.domain.cineasts.annotated.Rating;
import org.neo4j.ogm.domain.cineasts.annotated.User;
import org.neo4j.ogm.domain.friendships.Friendship;
import org.neo4j.ogm.domain.friendships.Person;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Vince Bickers
 */
public class RelationshipEntityTest extends InMemoryServerTest {

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        setUp();
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.friendships", "org.neo4j.ogm.domain.cineasts.annotated");
        session = sessionFactory.openSession("http://localhost:" + neoPort);
    }

    @Test
    public void shouldSaveFromStartObjectSetsAllObjectIds() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");

        // could use addFriend(...) but hey
        dave.getFriends().add(new Friendship(dave, mike, 5));

        session.save(dave);

        assertNotNull(dave.getId());
        assertNotNull(mike.getId());
        assertNotNull(dave.getFriends().get(0).getId());

    }

    @Test
    public void shouldSaveAndReloadAllSetsAllObjectIdsAndReferencesCorrectly() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        dave.getFriends().add(new Friendship(dave, mike, 5));

        session.save(dave);

        Collection<Person> personList = session.loadAll(Person.class);

        int expected = 2;
        assertEquals(expected, personList.size());
        for (Person person : personList) {
            if (person.getName().equals("Dave")) {
                expected--;
                assertEquals("Mike", person.getFriends().get(0).getFriend().getName());
            }
            else if (person.getName().equals("Mike")) {
                expected--;
                assertEquals(0, person.getFriends().size());
            }
        }
        assertEquals(0, expected);
    }

    @Test
    public void shouldSaveFromRelationshipEntitySetsAllObjectIds() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");

        Friendship friendship = new Friendship(dave, mike, 5);
        dave.getFriends().add(friendship);

        session.save(friendship);

        assertNotNull(dave.getId());
        assertNotNull(mike.getId());
        assertNotNull(dave.getFriends().get(0).getId());

    }

    @Test
    public void shouldLoadStartObjectHydratesProperly() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship friendship = new Friendship(dave, mike, 5);
        dave.getFriends().add(friendship);

        session.save(dave);

        Person daveCopy = session.load(Person.class, dave.getId());
        Friendship friendshipCopy = daveCopy.getFriends().get(0);
        Person mikeCopy = friendshipCopy.getFriend();

        assertNotNull(daveCopy.getId());
        assertNotNull(mikeCopy.getId());
        assertNotNull(friendshipCopy.getId());

        assertEquals("Dave", daveCopy.getName());
        assertEquals("Mike", mikeCopy.getName());
        assertEquals(5, friendshipCopy.getStrength());

    }

    @Test
    public void shouldLoadRelationshipEntityObjectHydratesProperly() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship friendship = new Friendship(dave, mike, 5);
        dave.getFriends().add(friendship);

        session.save(dave);

        Friendship friendshipCopy = session.load(Friendship.class, friendship.getId());
        Person daveCopy = friendshipCopy.getPerson();
        Person mikeCopy = friendshipCopy.getFriend();

        assertNotNull(daveCopy.getId());
        assertNotNull(mikeCopy.getId());
        assertNotNull(friendshipCopy.getId());

        assertEquals("Dave", daveCopy.getName());
        assertEquals("Mike", mikeCopy.getName());
        assertEquals(5, friendshipCopy.getStrength());

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
        Assert.assertNotNull(film);
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
        Assert.assertNotNull(movie.getRatings()); //Fails. But when entities are created first, test passes, see CineastsRatingsTest.shouldSaveRatingWithMovie
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


        //Movie dieHard = IteratorUtil.firstOrNull(session.loadByProperty(Movie.class, new Property<String, Object>("title", "Die Hard")));

        Movie dieHard = session.load(Movie.class, 0L);

        Assert.assertNotNull(dieHard);
        Assert.assertNotNull(dieHard.getRoles());
        assertEquals(1,dieHard.getRoles().size());
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

    /**
     * @see DATAGRAPH-567
     */
    @Test
    public void shouldSaveRelationshipEntityWithCamelCaseStartEndNodes() {
        Actor bruce = new Actor("Bruce");
        Actor jim = new Actor("Jim");

        Knows knows = new Knows();
        knows.setFirstActor(bruce);
        knows.setSecondActor(jim);
        knows.setSince(new Date());

        bruce.getKnows().add(knows);

        session.save(bruce);

        Actor actor = IteratorUtil.firstOrNull(session.loadByProperty(Actor.class, new Property<String, Object>("name", "Bruce")));
        Assert.assertNotNull(actor);
        assertEquals(1,actor.getKnows().size());
        assertEquals("Jim",actor.getKnows().iterator().next().getSecondActor().getName());
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
        Assert.assertNotNull("The loaded rating shouldn't be null", loadedRating);
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
        Assert.assertNotNull("The loaded rating shouldn't be null", loadedRating);
        assertEquals("The rated film wasn't saved correctly", "Fast and Furious XVII", loadedRating.getMovie().getTitle());
        assertEquals("The critic wasn't saved correctly", "Gary", loadedRating.getUser().getName());
    }

    /**
     * @see DATAGRAPH-569
     */
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
        Assert.assertNotNull(azkaban.getRatings());
        assertEquals(1, azkaban.getRatings().size());

        movies = session.loadByProperty(Movie.class,new Property<String, Object>("title","Harry Potter and the Order of the Phoenix"));
        phoenix = movies.iterator().next();
        Assert.assertNotNull(phoenix.getRatings());
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
        Assert.assertNotNull(azkaban.getRatings());
        assertEquals(1,azkaban.getRatings().size());

    }

    /**
     * @see DATAGRAPH-586
     */
    @Test
    public void shouldBeAbleToDeleteAllRatings() {
        Set<Rating> gobletRatings = new HashSet<>();
        Set<Rating> phoenixRatings = new HashSet<>();


        Movie goblet = new Movie();
        goblet.setTitle("Harry Potter and the Goblet of Fire");
        session.save(goblet);

        Movie phoenix = new Movie();
        phoenix.setTitle("Harry Potter and the Order of the Phoenix");
        session.save(phoenix);

        User adam = new User();
        adam.setName("Adam");

        Rating good = new Rating();
        good.setUser(adam);
        good.setMovie(goblet);
        good.setStars(3);
        gobletRatings.add(good);
        goblet.setRatings(gobletRatings);

        Rating okay = new Rating();
        okay.setMovie(phoenix);
        okay.setUser(adam);
        okay.setStars(2);
        phoenixRatings.add(okay);
        phoenix.setRatings(phoenixRatings);

        Set<Rating> adamsRatings = new HashSet<>();
        adamsRatings.add(good);
        adamsRatings.add(okay);
        adam.setRatings(adamsRatings);

        session.save(adam);

        adam = session.loadByProperty(User.class, new Property<String, Object>("name", "Adam")).iterator().next();
        assertEquals(2, adam.getRatings().size());

        //delete all ratings
        session.deleteAll(Rating.class);
        assertEquals(0, session.loadAll(Rating.class).size());

        phoenix = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Order of the Phoenix")).iterator().next();
        assertNull(phoenix.getRatings());

        goblet = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Goblet of Fire")).iterator().next();
        assertNull(goblet.getRatings());

        adam = session.loadByProperty(User.class, new Property<String, Object>("name", "Adam")).iterator().next();
        assertNull(adam.getRatings());
    }

    /**
     * @see DATAGRAPH-586
     */
    @Test
    public void shouldBeAbleToDeleteOneRating() {
        Set<Rating> gobletRatings = new HashSet<>();
        Set<Rating> phoenixRatings = new HashSet<>();


        Movie goblet = new Movie();
        goblet.setTitle("Harry Potter and the Goblet of Fire");
        session.save(goblet);

        Movie phoenix = new Movie();
        phoenix.setTitle("Harry Potter and the Order of the Phoenix");
        session.save(phoenix);

        User adam = new User();
        adam.setName("Adam");

        Rating good = new Rating();
        good.setUser(adam);
        good.setMovie(goblet);
        good.setStars(3);
        gobletRatings.add(good);
        goblet.setRatings(gobletRatings);

        Rating okay = new Rating();
        okay.setMovie(phoenix);
        okay.setUser(adam);
        okay.setStars(2);
        phoenixRatings.add(okay);
        phoenix.setRatings(phoenixRatings);

        Set<Rating> adamsRatings = new HashSet<>();
        adamsRatings.add(good);
        adamsRatings.add(okay);
        adam.setRatings(adamsRatings);

        session.save(adam);

        adam = session.loadByProperty(User.class, new Property<String, Object>("name", "Adam")).iterator().next();
        assertEquals(2, adam.getRatings().size());

        //delete one rating
        session.delete(okay);
        assertEquals(1, session.loadAll(Rating.class).size());

        phoenix = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Order of the Phoenix")).iterator().next();
        assertNull(phoenix.getRatings());

        goblet = session.loadByProperty(Movie.class, new Property<String, Object>("title", "Harry Potter and the Goblet of Fire")).iterator().next();
        assertEquals(1, goblet.getRatings().size());

        adam = session.loadByProperty(User.class, new Property<String, Object>("name", "Adam")).iterator().next();
        assertEquals(1, adam.getRatings().size());
    }

}
