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

package org.neo4j.ogm.integration;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.friendships.Friendship;
import org.neo4j.ogm.domain.friendships.Person;
import org.neo4j.ogm.exception.InvalidDepthException;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class RelationshipEntityHydrationTest extends InMemoryServerTest {

    private static SessionFactory sessionFactory;

    @Before
    public void init() throws IOException {
        setUp();
        sessionFactory = new SessionFactory("org.neo4j.ogm.domain.friendships");
        session = sessionFactory.openSession("http://localhost:" + neoPort);
    }

    @Test
    public void testThatSaveFromStartObjectSetsAllObjectIds() {

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
    public void testThatSaveAndReloadAllSetsAllObjectIdsAndReferencesCorrectly() {

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
    public void testThatSaveFromRelationshipEntitySetsAllObjectIds() {

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
    public void testThatLoadStartObjectHydratesProperly() {

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
    public void testThatLoadRelationshipEntityObjectHydratesProperly() {

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

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void testThatLoadMultipleRelationshipEntityObjectsHydratesProperly() {
        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship daveMike = new Friendship(dave, mike, 5);
        dave.getFriends().add(daveMike);

        session.save(dave);

        Person greg = new Person("Greg");
        Friendship daveGreg = new Friendship(dave,greg, 3);
        dave.getFriends().add(daveGreg);
        session.save(dave);

        Collection<Friendship> friendships = session.loadAll(Friendship.class, Arrays.asList(daveMike.getId(),daveGreg.getId()));
        assertNotNull(friendships);
        assertEquals(2, friendships.size());
        for(Friendship friendship : friendships) {
            if(friendship.getId().equals(daveMike.getId())) {
                assertEquals(dave, friendship.getPerson());
                assertEquals(mike, friendship.getFriend());
                assertEquals(5, friendship.getStrength());
            }
            else {
                assertEquals(dave, friendship.getPerson());
                assertEquals(greg,friendship.getFriend());
                assertEquals(3,friendship.getStrength());
            }
        }
    }

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void testThatLoadingAllRelationshipEntityObjectsHydratesProperly() {

        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship daveMike = new Friendship(dave, mike, 5);
        dave.getFriends().add(daveMike);

        session.save(dave);

        Person greg = new Person("Greg");
        Friendship daveGreg = new Friendship(dave,greg, 3);
        dave.getFriends().add(daveGreg);
        session.save(dave);

        Collection<Friendship> friendships = session.loadAll(Friendship.class);
        assertNotNull(friendships);
        assertEquals(2, friendships.size());
        for(Friendship friendship : friendships) {
            if(friendship.getId().equals(daveMike.getId())) {
                assertEquals(dave, friendship.getPerson());
                assertEquals(mike, friendship.getFriend());
                assertEquals(5, friendship.getStrength());
            }
            else {
                assertEquals(dave, friendship.getPerson());
                assertEquals(greg,friendship.getFriend());
                assertEquals(3,friendship.getStrength());
            }
        }
    }

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void testThatLoadMultipleRelationshipEntityObjectsByIdHydratesProperly() {
        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship daveMike = new Friendship(dave, mike, 5);
        dave.getFriends().add(daveMike);

        session.save(dave);

        Person greg = new Person("Greg");
        Friendship daveGreg = new Friendship(dave,greg, 3);
        dave.getFriends().add(daveGreg);
        session.save(dave);

        Collection<Friendship> friendships = session.loadAll(Arrays.asList(daveGreg,daveMike));
        assertNotNull(friendships);
        assertEquals(2, friendships.size());
        for(Friendship friendship : friendships) {
            if(friendship.getId().equals(daveMike.getId())) {
                assertEquals(dave, friendship.getPerson());
                assertEquals(mike, friendship.getFriend());
                assertEquals(5, friendship.getStrength());
            }
            else {
                assertEquals(dave, friendship.getPerson());
                assertEquals(greg,friendship.getFriend());
                assertEquals(3,friendship.getStrength());
            }
        }
    }

    /**
     * @see DATAGRAPH-552
     */
    @Test
    public void testThatLoadMultipleRelationshipEntityObjectsByPropertyHydratesProperly() {
        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship daveMike = new Friendship(dave, mike, 5);
        dave.getFriends().add(daveMike);

        session.save(dave);

        Person greg = new Person("Greg");
        Friendship daveGreg = new Friendship(dave,greg, 3);
        dave.getFriends().add(daveGreg);
        session.save(dave);

        Collection<Friendship> friendships = session.loadByProperty(Friendship.class,new Property<String, Object>("strength", 5));
        assertNotNull(friendships);
        assertEquals(1, friendships.size());
        Friendship friendship = friendships.iterator().next();
        assertEquals(dave, friendship.getPerson());
        assertEquals(mike, friendship.getFriend());
        assertEquals(5, friendship.getStrength());
    }



    /**
     * @see DATAGRAPH-552
     */
    @Test(expected = InvalidDepthException.class)
    public void testThatLoadingARelationshipEntityWithDepthZeroIsNotAllowed() {
        Person mike = new Person("Mike");
        Person dave = new Person("Dave");
        Friendship friendship = new Friendship(dave, mike, 5);
        dave.getFriends().add(friendship);

        session.save(dave);

        session.load(Friendship.class, friendship.getId(),0);
    }


}
